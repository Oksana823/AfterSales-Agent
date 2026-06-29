package com.aftersales.platform.agent.plan;

public record PlanStep(String id, PlanAction action, String condition, boolean requiresApproval) {}
