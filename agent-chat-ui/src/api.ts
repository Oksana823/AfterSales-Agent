export interface AgentResponse {
    runId: number;
    taskType: string;
    answer: string;
}

export interface AgentRun {
    id: number;
    userInput: string;
    taskType: string;
    status: string;
    finalAnswer: string | null;
    replayFromRunId: number | null;
    createdAt: string;
    updatedAt: string;
}

export interface AgentStep {
    id: number;
    runId: number;
    agentName: string;
    stepName: string;
    result: string;
    createdAt: string;
}

export interface ToolCallLog {
    id: number;
    runId: number;
    toolName: string;
    argumentsJson: string;
    resultJson: string;
    elapsedMs: number;
    status: string;
    errorMessage: string | null;
    createdAt: string;
}

export interface ModelCallLog {
    id: number;
    runId: number;
    scene: string;
    modelName: string;
    elapsedMs: number;
    status: string;
    errorMessage: string | null;
    createdAt: string;
}

export interface RunDetail {
    run: AgentRun;
    steps: AgentStep[];
    toolCalls: ToolCallLog[];
    modelCalls: ModelCallLog[];
}

export interface ApprovalRequest {
    id: number;
    runId: number;
    actionName: string;
    orderId: number;
    reason: string;
    status: string;
    createdAt: string;
    handledAt: string | null;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(path, {
        ...init,
        headers: {'Content-Type': 'application/json', ...init?.headers},
    });
    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || '请求失败（' + response.status + '）');
    }
    return response.json() as Promise<T>;
}

export function submitTask(message: string): Promise<AgentResponse> {
    return request('/api/agent/chat', {
        method: 'POST',
        body: JSON.stringify({message}),
    });
}

export function getRun(runId: number): Promise<RunDetail> {
    return request('/api/agent/runs/' + runId);
}

export function replayRun(runId: number): Promise<AgentResponse> {
    return request('/api/agent/runs/' + runId + '/replay', {method: 'POST'});
}

export function getPendingApprovals(): Promise<ApprovalRequest[]> {
    return request('/api/agent/approvals/pending');
}

export function approveRequest(id: number): Promise<{message: string}> {
    return request('/api/agent/approvals/' + id + '/approve', {method: 'POST'});
}

export function rejectRequest(id: number): Promise<{message: string}> {
    return request('/api/agent/approvals/' + id + '/reject', {method: 'POST'});
}

export function rebuildProductIndex(): Promise<{message: string}> {
    return request('/api/agent/products/index/rebuild', {method: 'POST'});
}
