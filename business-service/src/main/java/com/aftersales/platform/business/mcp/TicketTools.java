package com.aftersales.platform.business.mcp;

import com.aftersales.platform.common.domain.Ticket;
import com.aftersales.platform.business.service.TicketService;
import com.aftersales.platform.business.service.BusinessToolLogService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 工单 MCP 工具适配层，对外暴露创建、查询和客服回复回写能力。
 */
@Component
public class TicketTools {
    private final TicketService ticketService;
    private final BusinessToolLogService log;

    public TicketTools(TicketService ticketService, BusinessToolLogService log) {
        this.ticketService = ticketService;
        this.log = log;
    }

    @Tool(description = "创建售后工单")
    public Ticket createTicket(@ToolParam(description = "Agent run id") Long runId,
                               @ToolParam(description = "订单ID") Long orderId,
                               @ToolParam(description = "用户ID") Long userId,
                               @ToolParam(description = "商品ID") Long productId,
                               @ToolParam(description = "原因") String reason,
                               @ToolParam(description = "客服回复") String reply) {
        return log.call(runId, "ticket.createTicket",
                Map.of("orderId", orderId, "userId", userId, "productId", productId, "reason", reason),
                () -> ticketService.create(orderId, userId, productId, reason, reply));
    }

    @Tool(description = "更新工单客服回复")
    public Ticket updateTicketCustomerReply(@ToolParam(description = "Agent run id") Long runId,
                                            @ToolParam(description = "工单ID") Long ticketId,
                                            @ToolParam(description = "客服回复") String reply) {
        return log.call(runId, "ticket.updateCustomerReply", Map.of("ticketId", ticketId),
                () -> ticketService.updateCustomerReply(ticketId, reply));
    }

    @Tool(description = "查询工单详情")
    public Ticket getTicket(@ToolParam(description = "Agent run id") Long runId,
                            @ToolParam(description = "工单ID") Long ticketId) {
        return log.call(runId, "ticket.getTicket", Map.of("ticketId", ticketId), () -> ticketService.get(ticketId));
    }

    @Tool(description = "根据订单查询工单")
    public List<Ticket> getTicketsByOrder(@ToolParam(description = "Agent run id") Long runId,
                                          @ToolParam(description = "订单ID") Long orderId) {
        return log.call(runId, "ticket.getTicketsByOrder", Map.of("orderId", orderId),
                () -> ticketService.byOrder(orderId));
    }
}
