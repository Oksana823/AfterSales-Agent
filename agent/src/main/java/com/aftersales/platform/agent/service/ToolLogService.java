package com.aftersales.platform.agent.service;

import org.springframework.stereotype.Service;
import java.util.function.Supplier;

@Service
public class ToolLogService {
    private final TraceService traceService;
    private final JsonService json;
    public ToolLogService(TraceService traceService, JsonService json) { this.traceService = traceService; this.json = json; }
    public <T> T call(Long runId, String toolName, Object args, Supplier<T> supplier) {
        long start = System.currentTimeMillis();
        try {
            T result = supplier.get();
            traceService.tool(runId, toolName, json.toJson(args), json.toJson(result), System.currentTimeMillis() - start, "SUCCESS", null);
            return result;
        } catch (RuntimeException e) {
            traceService.tool(runId, toolName, json.toJson(args), null, System.currentTimeMillis() - start, "FAILED", e.getMessage());
            throw e;
        }
    }
}
