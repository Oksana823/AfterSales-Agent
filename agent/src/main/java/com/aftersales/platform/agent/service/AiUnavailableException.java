package com.aftersales.platform.agent.service;

/**
 * 表示任务无法在模型故障时安全降级，统一映射为 HTTP 503。
 */
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String message) {
        super(message);
    }
}
