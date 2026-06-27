import React, {useEffect, useState} from 'react';
import './App.css';
import {
    AgentResponse,
    ApprovalRequest,
    RunDetail,
    approveRequest,
    getPendingApprovals,
    getRun,
    rebuildProductIndex,
    rejectRequest,
    replayRun,
    submitTask,
} from './api';

type View = 'task' | 'trace' | 'approvals';

const EXAMPLES = [
    {
        label: '售后处理',
        text: '帮我处理用户10086最近一笔订单，如果已付款但超过48小时未发货，就创建售后工单，并生成客服回复。',
    },
    {
        label: '取消审批',
        text: '帮我取消订单10001，理由是用户不想要了。',
    },
    {
        label: '商品咨询',
        text: '帮我找一个适合学生用的轻薄本，预算5000左右。',
    },
];

function App() {
    const [view, setView] = useState<View>('task');
    const [input, setInput] = useState(EXAMPLES[0].text);
    const [response, setResponse] = useState<AgentResponse | null>(null);
    const [detail, setDetail] = useState<RunDetail | null>(null);
    const [runIdInput, setRunIdInput] = useState('');
    const [approvals, setApprovals] = useState<ApprovalRequest[]>([]);
    const [loading, setLoading] = useState(false);
    const [notice, setNotice] = useState('');
    const [error, setError] = useState('');

    useEffect(() => {
        if (view === 'approvals') {
            void loadApprovals();
        }
    }, [view]);

    async function loadRunDetail(runId: number) {
        const data = await getRun(runId);
        setDetail(data);
        setRunIdInput(String(runId));
    }

    async function handleSubmit() {
        if (!input.trim() || loading) return;
        setLoading(true);
        setError('');
        setNotice('');
        try {
            const result = await submitTask(input.trim());
            setResponse(result);
            await loadRunDetail(result.runId);
            setNotice('任务已执行，Run #' + result.runId + ' 的轨迹已载入。');
        } catch (e) {
            setError(errorMessage(e));
        } finally {
            setLoading(false);
        }
    }

    async function handleLookup() {
        const id = Number(runIdInput);
        if (!Number.isInteger(id) || id <= 0) {
            setError('请输入有效的 Run ID。');
            return;
        }
        setLoading(true);
        setError('');
        try {
            await loadRunDetail(id);
        } catch (e) {
            setError(errorMessage(e));
        } finally {
            setLoading(false);
        }
    }

    async function handleReplay() {
        if (!detail || loading) return;
        setLoading(true);
        setError('');
        try {
            const result = await replayRun(detail.run.id);
            setResponse(result);
            await loadRunDetail(result.runId);
            setNotice('Replay 完成，已生成新 Run #' + result.runId + '。');
        } catch (e) {
            setError(errorMessage(e));
        } finally {
            setLoading(false);
        }
    }

    async function loadApprovals() {
        setLoading(true);
        setError('');
        try {
            setApprovals(await getPendingApprovals());
        } catch (e) {
            setError(errorMessage(e));
        } finally {
            setLoading(false);
        }
    }

    async function handleApproval(id: number, approve: boolean) {
        setLoading(true);
        setError('');
        try {
            const result = approve ? await approveRequest(id) : await rejectRequest(id);
            setNotice(result.message);
            await loadApprovals();
        } catch (e) {
            setError(errorMessage(e));
            setLoading(false);
        }
    }

    async function handleRebuildIndex() {
        setLoading(true);
        setError('');
        try {
            await rebuildProductIndex();
            setNotice('Elasticsearch 商品索引重建完成。');
        } catch (e) {
            setError(errorMessage(e));
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="shell">
            <aside className="sidebar">
                <div className="brand">
                    <span className="brand__mark">AS</span>
                    <div>
                        <strong>售后智能台</strong>
                        <small>Multi-Agent Harness</small>
                    </div>
                </div>
                <nav className="nav" aria-label="主导航">
                    <NavButton active={view === 'task'} label="任务执行" onClick={() => setView('task')}/>
                    <NavButton active={view === 'trace'} label="运行轨迹" onClick={() => setView('trace')}/>
                    <NavButton active={view === 'approvals'} label="审批队列" count={approvals.length} onClick={() => setView('approvals')}/>
                </nav>
                <div className="sidebar__status">
                    <span className="status-dot"/>
                    Gateway :8080
                </div>
            </aside>

            <main className="workspace">
                <header className="topbar">
                    <div>
                        <h1>{viewTitle(view)}</h1>
                        <p>{viewDescription(view)}</p>
                    </div>
                    <button className="button button--secondary" onClick={handleRebuildIndex} disabled={loading}>
                        重建商品索引
                    </button>
                </header>

                {(notice || error) && (
                    <div className={'notice ' + (error ? 'notice--error' : '')}>
                        {error || notice}
                    </div>
                )}

                {view === 'task' && (
                    <TaskView
                        input={input}
                        setInput={setInput}
                        response={response}
                        loading={loading}
                        onSubmit={handleSubmit}
                        onShowTrace={() => setView('trace')}
                    />
                )}

                {view === 'trace' && (
                    <TraceView
                        detail={detail}
                        runIdInput={runIdInput}
                        setRunIdInput={setRunIdInput}
                        loading={loading}
                        onLookup={handleLookup}
                        onReplay={handleReplay}
                    />
                )}

                {view === 'approvals' && (
                    <ApprovalView
                        approvals={approvals}
                        loading={loading}
                        onRefresh={loadApprovals}
                        onDecision={handleApproval}
                    />
                )}
            </main>
        </div>
    );
}

function NavButton({active, label, count, onClick}: {
    active: boolean;
    label: string;
    count?: number;
    onClick: () => void;
}) {
    return (
        <button className={'nav__item ' + (active ? 'nav__item--active' : '')} onClick={onClick}>
            <span>{label}</span>
            {count !== undefined && count > 0 && <b>{count}</b>}
        </button>
    );
}

function TaskView({input, setInput, response, loading, onSubmit, onShowTrace}: {
    input: string;
    setInput: (value: string) => void;
    response: AgentResponse | null;
    loading: boolean;
    onSubmit: () => void;
    onShowTrace: () => void;
}) {
    return (
        <div className="task-layout">
            <section className="task-panel">
                <label htmlFor="task-input">自然语言任务</label>
                <textarea
                    id="task-input"
                    value={input}
                    onChange={(event) => setInput(event.target.value)}
                    rows={6}
                    placeholder="输入售后、取消订单或商品咨询任务"
                />
                <div className="examples">
                    {EXAMPLES.map((example) => (
                        <button key={example.label} onClick={() => setInput(example.text)}>
                            <strong>{example.label}</strong>
                            <span>{example.text}</span>
                        </button>
                    ))}
                </div>
                <div className="actions">
                    <button className="button button--primary" onClick={onSubmit} disabled={loading || !input.trim()}>
                        {loading ? '执行中...' : '提交任务'}
                    </button>
                </div>
            </section>

            <section className="result-panel">
                <div className="section-heading">
                    <h2>执行结果</h2>
                    {response && <Status value={response.taskType}/>}
                </div>
                {response ? (
                    <>
                        <dl className="run-summary">
                            <div><dt>Run ID</dt><dd>#{response.runId}</dd></div>
                            <div><dt>任务类型</dt><dd>{taskTypeName(response.taskType)}</dd></div>
                        </dl>
                        <div className="answer">{response.answer}</div>
                        <button className="text-button" onClick={onShowTrace}>查看完整执行轨迹 →</button>
                    </>
                ) : (
                    <Empty title="等待任务" text="提交任务后，这里会显示 Agent 回复和 Run ID。"/>
                )}
            </section>
        </div>
    );
}

function TraceView({detail, runIdInput, setRunIdInput, loading, onLookup, onReplay}: {
    detail: RunDetail | null;
    runIdInput: string;
    setRunIdInput: (value: string) => void;
    loading: boolean;
    onLookup: () => void;
    onReplay: () => void;
}) {
    return (
        <div className="trace-view">
            <div className="lookup">
                <input
                    value={runIdInput}
                    onChange={(event) => setRunIdInput(event.target.value)}
                    onKeyDown={(event) => event.key === 'Enter' && onLookup()}
                    placeholder="输入 Run ID"
                    inputMode="numeric"
                />
                <button className="button button--primary" onClick={onLookup} disabled={loading}>查询</button>
                <button className="button button--secondary" onClick={onReplay} disabled={loading || !detail}>Replay</button>
            </div>

            {!detail ? <Empty title="尚未载入轨迹" text="执行任务或输入 Run ID 查询。"/> : (
                <>
                    <section className="run-band">
                        <div>
                            <span>RUN #{detail.run.id}</span>
                            <h2>{detail.run.userInput}</h2>
                        </div>
                        <Status value={detail.run.status}/>
                    </section>

                    <section className="trace-section">
                        <h2>Agent Steps <span>{detail.steps.length}</span></h2>
                        <div className="timeline">
                            {detail.steps.map((step, index) => (
                                <article className="step" key={step.id}>
                                    <div className="step__index">{index + 1}</div>
                                    <div>
                                        <div className="step__meta">
                                            <strong>{step.agentName}</strong>
                                            <span>{formatTime(step.createdAt)}</span>
                                        </div>
                                        <h3>{step.stepName}</h3>
                                        <p>{step.result}</p>
                                    </div>
                                </article>
                            ))}
                        </div>
                    </section>

                    <section className="trace-section">
                        <h2>Model Calls <span>{detail.modelCalls?.length || 0}</span></h2>
                        <div className="table-wrap">
                            <table>
                                <thead><tr><th>场景</th><th>模型</th><th>状态</th><th>耗时</th><th>异常</th></tr></thead>
                                <tbody>
                                {(detail.modelCalls || []).map((call) => (
                                    <tr key={call.id}>
                                        <td><code>{call.scene}</code></td>
                                        <td>{call.modelName}</td>
                                        <td><Status value={call.status}/></td>
                                        <td>{call.elapsedMs} ms</td>
                                        <td>{call.errorMessage || "-"}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </section>

                    <section className="trace-section">
                        <h2>Tool Calls <span>{detail.toolCalls.length}</span></h2>
                        <div className="table-wrap">
                            <table>
                                <thead>
                                <tr><th>工具</th><th>状态</th><th>耗时</th><th>参数</th><th>结果</th></tr>
                                </thead>
                                <tbody>
                                {detail.toolCalls.map((call) => (
                                    <tr key={call.id}>
                                        <td><code>{call.toolName}</code></td>
                                        <td><Status value={call.status}/></td>
                                        <td>{call.elapsedMs} ms</td>
                                        <td><JsonValue value={call.argumentsJson}/></td>
                                        <td><JsonValue value={call.errorMessage || call.resultJson}/></td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </section>
                </>
            )}
        </div>
    );
}

function ApprovalView({approvals, loading, onRefresh, onDecision}: {
    approvals: ApprovalRequest[];
    loading: boolean;
    onRefresh: () => void;
    onDecision: (id: number, approve: boolean) => void;
}) {
    return (
        <div>
            <div className="approval-toolbar">
                <span>{approvals.length} 项待处理</span>
                <button className="button button--secondary" onClick={onRefresh} disabled={loading}>刷新</button>
            </div>
            {approvals.length === 0 ? (
                <Empty title="暂无待审批任务" text="取消订单任务会在这里等待人工决策。"/>
            ) : (
                <div className="approval-list">
                    {approvals.map((approval) => (
                        <article className="approval-card" key={approval.id}>
                            <div className="approval-card__head">
                                <div>
                                    <span>审批 #{approval.id} · Run #{approval.runId}</span>
                                    <h2>取消订单 #{approval.orderId}</h2>
                                </div>
                                <Status value={approval.status}/>
                            </div>
                            <p><strong>申请理由：</strong>{approval.reason}</p>
                            <small>{formatTime(approval.createdAt)}</small>
                            <div className="actions">
                                <button className="button button--danger" onClick={() => onDecision(approval.id, false)} disabled={loading}>拒绝</button>
                                <button className="button button--primary" onClick={() => onDecision(approval.id, true)} disabled={loading}>批准并取消订单</button>
                            </div>
                        </article>
                    ))}
                </div>
            )}
        </div>
    );
}

function Empty({title, text}: {title: string; text: string}) {
    return <div className="empty"><strong>{title}</strong><p>{text}</p></div>;
}

function Status({value}: {value: string}) {
    return <span className={'status status--' + value.toLowerCase()}>{statusName(value)}</span>;
}

function JsonValue({value}: {value: string}) {
    let display = value;
    try {
        display = JSON.stringify(JSON.parse(value), null, 2);
    } catch {
        // 后端异常文本直接展示。
    }
    return <pre title={display}>{display}</pre>;
}

function errorMessage(error: unknown): string {
    return error instanceof Error ? error.message : '请求失败，请检查 Gateway 和 Agent 是否启动。';
}

function viewTitle(view: View): string {
    return {task: '任务执行', trace: '运行轨迹', approvals: '审批队列'}[view];
}

function viewDescription(view: View): string {
    return {
        task: '提交自然语言任务，由 Supervisor 分派给业务 Agent。',
        trace: '查看每个 Agent 步骤、MCP 工具调用与执行结果。',
        approvals: '人工确认敏感操作，批准后才会真正取消订单。',
    }[view];
}

function taskTypeName(value: string): string {
    return {
        AFTER_SALES: '售后处理',
        CANCEL_ORDER: '取消订单',
        PRODUCT_CONSULTATION: '商品咨询',
        UNKNOWN: '未知任务',
    }[value] || value;
}

function statusName(value: string): string {
    return {
        RUNNING: '执行中',
        COMPLETED: '已完成',
        FAILED: '失败',
        WAITING_APPROVAL: '等待审批',
        PENDING: '待审批',
        SUCCESS: '成功',
        ERROR: '异常',
        AFTER_SALES: '售后处理',
        CANCEL_ORDER: '取消订单',
        PRODUCT_CONSULTATION: '商品咨询',
    }[value] || value;
}

function formatTime(value: string): string {
    return value ? new Date(value).toLocaleString('zh-CN', {hour12: false}) : '-';
}

export default App;
