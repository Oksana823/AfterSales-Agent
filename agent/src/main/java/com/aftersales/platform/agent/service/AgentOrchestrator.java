package com.aftersales.platform.agent.service;

import com.aftersales.platform.agent.agent.NlpExtractor;
import com.aftersales.platform.agent.agent.AgentRoleGateway;
import com.aftersales.platform.agent.domain.ApprovalRequest;
import com.aftersales.platform.agent.domain.OrderInfo;
import com.aftersales.platform.agent.domain.Product;
import com.aftersales.platform.agent.domain.Ticket;
import com.aftersales.platform.agent.domain.Enums.OrderStatus;
import com.aftersales.platform.agent.domain.Enums.RunStatus;
import com.aftersales.platform.agent.domain.Enums.TaskType;
import com.aftersales.platform.agent.mcp.client.AgentToolGateway;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentOrchestrator {
    private final AgentRoleGateway roles;
    private final NlpExtractor nlp;
    private final TraceService trace;
    private final ApprovalService approvals;
    private final AgentToolGateway tools;

    public AgentOrchestrator(AgentRoleGateway roles, NlpExtractor nlp, TraceService trace,
                             ApprovalService approvals, AgentToolGateway tools) {
        this.roles = roles;
        this.nlp = nlp;
        this.trace = trace;
        this.approvals = approvals;
        this.tools = tools;
    }

    /** Agent 主入口：创建 run、记录计划、按任务类型分派到具体 Agent 流程。 */
    public AgentResponse execute(String input) {
        return execute(input, null);
    }

    public AgentResponse execute(String input, Long replayFromRunId) {
        Long runId = trace.createRun(input, TaskType.UNKNOWN, replayFromRunId);

        try {
            TaskType type = roles.classify(runId, input);
            trace.updateTaskType(runId, type);
            trace.step(runId, "Supervisor", "任务分类", type.name());
            trace.step(runId, "Planner", "执行计划",
                    String.join(" -> ", roles.plan(runId, type, input)));

            String answer = switch (type) {
                case AFTER_SALES -> afterSales(runId, input);
                case CANCEL_ORDER -> cancel(runId, input);
                case PRODUCT_CONSULTATION -> consult(runId, input);
                case UNKNOWN -> unknown(runId);
            };

            return new AgentResponse(runId, type.name(), answer);
        } catch (RuntimeException e) {
            trace.finish(runId, RunStatus.FAILED, e.getMessage());
            throw e;
        }
    }

    private String afterSales(Long runId, String input) {
        Long userId = nlp.firstNumber(input);
        OrderInfo order = tools.getLatestOrder(runId, userId);
        trace.step(runId, "Order Agent", "查询最近订单", "orderId=" + order.id());

        Boolean delayed = tools.isDelayedShipment(runId, order.id());
        trace.step(runId, "Order Agent", "延迟发货判断", delayed.toString());

        if (!delayed) {
            String answer = "订单" + order.id() + "不满足已付款且超过阈值未发货条件。";
            trace.finish(runId, RunStatus.COMPLETED, answer);
            return answer;
        }

        Product product = tools.getProduct(runId, order.productId());
        String policy = tools.getAfterSalesPolicy(runId, product.id());
        String reply = roles.afterSalesReply(runId, order.id(), product.name(), policy);
        Ticket ticket = tools.createTicket(runId, order.id(), order.userId(), order.productId(), "已付款超过48小时未发货", reply);

        trace.step(runId, "Ticket Agent", "创建售后工单", "ticketId=" + ticket.id());
        trace.step(runId, "Reporter Agent", "生成回复", reply);
        trace.finish(runId, RunStatus.COMPLETED, reply);
        return reply;
    }

    private String cancel(Long runId, String input) {
        Long orderId = nlp.firstNumber(input);
        String reason = nlp.cancelReason(input);
        OrderInfo order = tools.getOrder(runId, orderId);
        trace.step(runId, "Order Agent", "查询订单", order.status().name());

        if (order.status() == OrderStatus.CANCELLED) {
            String answer = "订单" + order.id() + "已经取消，无需重复创建审批。";
            trace.step(runId, "Order Agent", "取消条件判断", answer);
            trace.finish(runId, RunStatus.COMPLETED, answer);
            return answer;
        }
        if (order.status() != OrderStatus.CREATED && order.status() != OrderStatus.PAID) {
            String answer = "订单" + order.id() + "当前状态为" + order.status() + "，不允许取消。";
            trace.step(runId, "Order Agent", "取消条件判断", answer);
            trace.finish(runId, RunStatus.COMPLETED, answer);
            return answer;
        }

        if (roles.sensitive(runId, "cancelOrder")) {
            ApprovalRequest approval = approvals.create(runId, "cancelOrder", orderId, reason);
            String answer = roles.cancelWaiting(approval.id(), orderId);
            trace.step(runId, "Risk Agent", "创建审批", answer);
            trace.finish(runId, RunStatus.WAITING_APPROVAL, answer);
            return answer;
        }

        OrderInfo cancelled = tools.cancelOrder(runId, orderId, reason);
        String answer = roles.cancelApproved(cancelled.id());
        trace.finish(runId, RunStatus.COMPLETED, answer);
        return answer;
    }

    private String consult(Long runId, String input) {
        List<Product> result = tools.searchProducts(runId, nlp.productKeyword(input));
        trace.step(runId, "Product Agent", "ES商品搜索", "count=" + result.size());
        String answer = roles.productAdvice(runId, result);
        trace.step(runId, "Reporter Agent", "生成推荐", answer);
        trace.finish(runId, RunStatus.COMPLETED, answer);
        return answer;
    }

    private String unknown(Long runId) {
        String answer = "暂不支持该任务。";
        trace.finish(runId, RunStatus.COMPLETED, answer);
        return answer;
    }

    public record AgentResponse(Long runId, String taskType, String answer) {
    }
}
