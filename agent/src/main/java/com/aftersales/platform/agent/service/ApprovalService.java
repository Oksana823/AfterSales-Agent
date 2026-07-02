package com.aftersales.platform.agent.service;

import com.aftersales.platform.agent.agent.ReporterAgent;
import com.aftersales.platform.agent.domain.ApprovalRequest;
import com.aftersales.platform.common.domain.Enums.ApprovalStatus;
import com.aftersales.platform.common.domain.Enums.OrderStatus;
import com.aftersales.platform.common.domain.Enums.RunStatus;
import com.aftersales.platform.common.domain.OrderInfo;
import com.aftersales.platform.agent.mcp.client.AgentToolGateway;
import com.aftersales.platform.agent.plan.PlanAction;
import com.aftersales.platform.agent.repository.ApprovalRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 敏感操作审批服务，负责创建审批、处理批准或拒绝，并在批准后恢复取消计划。
 */
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

    /**
     * 审批通过后签发一次性授权，再通过 MCP 调用取消工具。
     */
    public String approve(Long id) {
        // ===== 1) 只允许处理仍为 PENDING 的审批单，防止重复点击 =====
        ApprovalRequest approval = pendingApproval(id);
        // ===== 2) 批准前重新查询订单，避免审批等待期间订单状态已经变化 =====
        OrderInfo current = tools.getOrder(approval.runId(), approval.orderId());

        if (current.status() == OrderStatus.CANCELLED) {
            return rejectInvalidOrder(approval, "订单已取消，无需重复执行");
        }
        if (current.status() != OrderStatus.CREATED && current.status() != OrderStatus.PAID) {
            return rejectInvalidOrder(approval, "订单状态" + current.status() + "不允许取消");
        }

        // ===== 3) 在 Redis 签发两分钟一次性凭证，Business 没有凭证就拒绝取消 =====
        cancellationAuthorization.authorize(approval.runId(), approval.orderId(), approval.id());
        OrderInfo order;
        try {
            // ===== 4) 恢复计划中的 CANCEL_ORDER，通过 MCP 调用真正修改订单 =====
            order = tools.cancelOrder(approval.runId(), approval.orderId(), approval.reason());
        } catch (RuntimeException exception) {
            cancellationAuthorization.revoke(approval.runId(), approval.orderId());
            throw exception;
        }

        // ===== 5) 使用条件更新落库审批结果，防止并发审批覆盖 =====
        if (approvals.updateStatus(id, ApprovalStatus.APPROVED) != 1) {
            throw new IllegalStateException("审批状态已变化，请刷新后重试");
        }

        String answer = reporter.cancelApproved(order.id());
        trace.step(approval.runId(), "Plan Executor", PlanAction.CANCEL_ORDER.name(),
                "SUCCESS, orderId=" + order.id());
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
