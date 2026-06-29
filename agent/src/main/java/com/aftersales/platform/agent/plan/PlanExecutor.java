package com.aftersales.platform.agent.plan;

import com.aftersales.platform.agent.agent.AgentRoleGateway;
import com.aftersales.platform.agent.agent.NlpExtractor;
import com.aftersales.platform.agent.domain.ApprovalRequest;
import com.aftersales.platform.agent.mcp.client.AgentToolGateway;
import com.aftersales.platform.agent.service.ApprovalService;
import com.aftersales.platform.agent.service.TraceService;
import com.aftersales.platform.common.domain.*;
import com.aftersales.platform.common.domain.Enums.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PlanExecutor {
    private static final String PENDING_REPLY = "客服回复待生成";
    private final NlpExtractor nlp; private final TraceService trace; private final ApprovalService approvals; private final AgentToolGateway tools; private final AgentRoleGateway roles;
    public PlanExecutor(NlpExtractor nlp, TraceService trace, ApprovalService approvals, AgentToolGateway tools, AgentRoleGateway roles) { this.nlp = nlp; this.trace = trace; this.approvals = approvals; this.tools = tools; this.roles = roles; }

    /** 严格按已校验计划执行；所有业务动作仍通过MCP，敏感动作仍由Java审批策略控制。 */
    public PlanExecutionResult execute(Long runId, String input, ExecutionPlan plan) {
        State state = new State(input);
        for (PlanStep step : plan.steps()) {
            if (state.finished) { record(runId, step, "SKIPPED, reason=流程已结束"); continue; }
            if ("delayed == true".equals(step.condition()) && !Boolean.TRUE.equals(state.delayed)) { record(runId, step, "SKIPPED, reason=未发生延迟发货"); continue; }
            switch (step.action()) {
                case GET_LATEST_ORDER -> { state.order = tools.getLatestOrder(runId, nlp.firstNumber(input)); record(runId, step, "SUCCESS, orderId=" + state.order.id()); }
                case CHECK_DELAYED_SHIPMENT -> { state.delayed = tools.isDelayedShipment(runId, requireOrder(state).id()); record(runId, step, "SUCCESS, delayed=" + state.delayed); }
                case GET_PRODUCT -> { state.product = tools.getProduct(runId, requireOrder(state).productId()); record(runId, step, "SUCCESS, productId=" + state.product.id()); }
                case GET_AFTER_SALES_POLICY -> { state.policy = tools.getAfterSalesPolicy(runId, requireProduct(state).id()); record(runId, step, "SUCCESS"); }
                case CREATE_TICKET -> { OrderInfo order = requireOrder(state); state.ticket = tools.createTicket(runId, order.id(), order.userId(), order.productId(), "已付款超过48小时未发货", PENDING_REPLY); record(runId, step, "SUCCESS, ticketId=" + state.ticket.id()); }
                case GENERATE_AFTER_SALES_REPLY -> { state.answer = roles.afterSalesReply(runId, requireTicket(state).id(), requireOrder(state).id(), requireProduct(state).name(), state.policy); record(runId, step, "SUCCESS"); }
                case UPDATE_TICKET_REPLY -> { tools.updateTicketCustomerReply(runId, requireTicket(state).id(), requireAnswer(state)); record(runId, step, "SUCCESS, ticketId=" + state.ticket.id()); }
                case GET_ORDER -> { state.order = tools.getOrder(runId, nlp.firstNumber(input)); state.cancelReason = nlp.cancelReason(input); record(runId, step, "SUCCESS, orderId=" + state.order.id()); }
                case VALIDATE_CANCELLABLE -> validateCancellation(runId, step, state);
                case CREATE_CANCEL_APPROVAL -> {
                    if (!roles.sensitive(runId, "cancelOrder")) throw new IllegalStateException("取消订单安全策略异常：未识别为敏感操作");
                    ApprovalRequest approval = approvals.create(runId, "cancelOrder", requireOrder(state).id(), state.cancelReason);
                    state.answer = roles.cancelWaiting(approval.id(), state.order.id());
                    record(runId, step, "SUCCESS, approvalId=" + approval.id());
                    trace.step(runId, "Plan Executor", PlanAction.CANCEL_ORDER.name(), "WAITING_APPROVAL");
                    return new PlanExecutionResult(state.answer, RunStatus.WAITING_APPROVAL);
                }
                case CANCEL_ORDER -> throw new IllegalStateException("取消订单必须等待审批回调，不能在初始计划中直接执行");
                case SEARCH_PRODUCTS -> { state.products = tools.searchProducts(runId, nlp.productKeyword(input)); record(runId, step, "SUCCESS, count=" + state.products.size()); }
                case GENERATE_PRODUCT_ADVICE -> { state.answer = roles.productAdvice(runId, state.products); record(runId, step, "SUCCESS"); }
                case RETURN_UNSUPPORTED -> { state.answer = "暂不支持该任务。"; state.finished = true; record(runId, step, "SUCCESS"); }
            }
        }
        if (plan.taskType() == TaskType.AFTER_SALES && !Boolean.TRUE.equals(state.delayed)) state.answer = "订单" + requireOrder(state).id() + "不满足已付款且超过阈值未发货条件。";
        return new PlanExecutionResult(requireAnswer(state), RunStatus.COMPLETED);
    }

    private void validateCancellation(Long runId, PlanStep step, State state) {
        OrderInfo order = requireOrder(state);
        if (order.status() == OrderStatus.CANCELLED) { state.answer = "订单" + order.id() + "已经取消，无需重复创建审批。"; state.finished = true; record(runId, step, "STOPPED, reason=订单已取消"); return; }
        if (order.status() != OrderStatus.CREATED && order.status() != OrderStatus.PAID) { state.answer = "订单" + order.id() + "当前状态为" + order.status() + "，不允许取消。"; state.finished = true; record(runId, step, "STOPPED, reason=状态不可取消"); return; }
        record(runId, step, "SUCCESS");
    }
    private void record(Long runId, PlanStep step, String result) { trace.step(runId, "Plan Executor", step.action().name(), result); }
    private OrderInfo requireOrder(State s) { if (s.order == null) throw new IllegalStateException("计划缺少订单上下文"); return s.order; }
    private Product requireProduct(State s) { if (s.product == null) throw new IllegalStateException("计划缺少商品上下文"); return s.product; }
    private Ticket requireTicket(State s) { if (s.ticket == null) throw new IllegalStateException("计划缺少工单上下文"); return s.ticket; }
    private String requireAnswer(State s) { if (s.answer == null || s.answer.isBlank()) throw new IllegalStateException("计划未生成最终结果"); return s.answer; }
    public record PlanExecutionResult(String answer, RunStatus status) {}
    private static class State { final String input; OrderInfo order; Product product; Ticket ticket; Boolean delayed; String policy; String answer; String cancelReason; List<Product> products = List.of(); boolean finished; State(String input) { this.input = input; } }
}
