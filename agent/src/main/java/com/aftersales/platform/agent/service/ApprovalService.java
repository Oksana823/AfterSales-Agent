package com.aftersales.platform.agent.service;

import com.aftersales.platform.agent.agent.ReporterAgent;
import com.aftersales.platform.agent.domain.ApprovalRequest;
import com.aftersales.platform.agent.domain.Enums.ApprovalStatus;
import com.aftersales.platform.agent.domain.Enums.OrderStatus;
import com.aftersales.platform.agent.domain.Enums.RunStatus;
import com.aftersales.platform.agent.domain.OrderInfo;
import com.aftersales.platform.agent.mcp.client.AgentToolGateway;
import com.aftersales.platform.agent.repository.ApprovalRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApprovalService {
    private final ApprovalRepository approvals;
    private final AgentToolGateway tools;
    private final CancellationAuthorizationService cancellationAuthorization;
    private final TraceService trace;
    private final ReporterAgent reporter;

    public ApprovalService(ApprovalRepository approvals, AgentToolGateway tools,
                           CancellationAuthorizationService cancellationAuthorization,
                           TraceService trace, ReporterAgent reporter) {
        this.approvals = approvals;
        this.tools = tools;
        this.cancellationAuthorization = cancellationAuthorization;
        this.trace = trace;
        this.reporter = reporter;
    }

    public ApprovalRequest create(Long runId, String action, Long orderId, String reason) {
        return approvals.create(runId, action, orderId, reason);
    }

    public List<ApprovalRequest> pending() {
        return approvals.pending();
    }

    /** 审批通过后签发一次性授权，再通过 MCP 调用取消工具。 */
    public String approve(Long id) {
        ApprovalRequest approval = pendingApproval(id);
        OrderInfo current = tools.getOrder(approval.runId(), approval.orderId());

        if (current.status() == OrderStatus.CANCELLED) {
            return rejectInvalidOrder(approval, "订单已取消，无需重复执行");
        }
        if (current.status() != OrderStatus.CREATED && current.status() != OrderStatus.PAID) {
            return rejectInvalidOrder(approval, "订单状态" + current.status() + "不允许取消");
        }

        cancellationAuthorization.authorize(approval.runId(), approval.orderId(), approval.id());
        OrderInfo order;
        try {
            order = tools.cancelOrder(approval.runId(), approval.orderId(), approval.reason());
        } catch (RuntimeException exception) {
            cancellationAuthorization.revoke(approval.runId(), approval.orderId());
            throw exception;
        }

        if (approvals.updateStatus(id, ApprovalStatus.APPROVED) != 1) {
            throw new IllegalStateException("审批状态已变化，请刷新后重试");
        }

        String answer = reporter.cancelApproved(order.id());
        trace.step(approval.runId(), "Order Agent", "审批后取消订单", answer);
        trace.finish(approval.runId(), RunStatus.COMPLETED, answer);
        return answer;
    }

    public String reject(Long id) {
        ApprovalRequest approval = pendingApproval(id);
        if (approvals.updateStatus(id, ApprovalStatus.REJECTED) != 1) {
            throw new IllegalStateException("审批状态已变化，请刷新后重试");
        }

        String answer = "审批已拒绝，订单" + approval.orderId() + "保持原状态。";
        trace.step(approval.runId(), "Risk Agent", "审批拒绝", answer);
        trace.finish(approval.runId(), RunStatus.COMPLETED, answer);
        return answer;
    }

    private ApprovalRequest pendingApproval(Long id) {
        ApprovalRequest approval = approvals.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("审批单不存在"));
        if (approval.status() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("审批单已处理，当前状态：" + approval.status());
        }
        return approval;
    }

    private String rejectInvalidOrder(ApprovalRequest approval, String reason) {
        if (approvals.updateStatus(approval.id(), ApprovalStatus.REJECTED) != 1) {
            throw new IllegalStateException("审批状态已变化，请刷新后重试");
        }

        String answer = reason + "，审批单已自动拒绝。";
        trace.step(approval.runId(), "Risk Agent", "审批自动拒绝", answer);
        trace.finish(approval.runId(), RunStatus.COMPLETED, answer);
        return answer;
    }
}
