package com.aftersales.platform.agent.service;

/**
 * 携带已持久化的 runId，使客户端在业务失败时仍能查询完整 Trace。
 */
public class RunExecutionException extends RuntimeException {
    private final Long runId;

    public RunExecutionException(Long runId, RuntimeException cause) {
        super(cause.getMessage(), cause);
        this.runId = runId;
    }

    public Long getRunId() {
        return runId;
    }
}
