package com.aftersales.platform.common.domain;

/**
 * 集中定义跨服务共享的任务、运行、审批、订单和工单状态枚举。
 */
public final class Enums {
    private Enums() {
    }

    public enum OrderStatus {CREATED, PAID, SHIPPED, COMPLETED, CANCELLED}

    public enum TicketStatus {OPEN, CLOSED}

    public enum RunStatus {RUNNING, WAITING_APPROVAL, COMPLETED, COMPLETED_WITH_WARNINGS, FAILED}

    public enum TaskType {AFTER_SALES, CANCEL_ORDER, PRODUCT_CONSULTATION, UNKNOWN}

    public enum ApprovalStatus {PENDING, APPROVED, REJECTED}
}
