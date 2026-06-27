# 智能售后订单Agent

面向售后订单场景的分布式 Multi-Agent 平台。系统用 Agent Worker 完成任务分类、计划、风险识别和报告生成，用 MCP Streamable HTTP 执行业务工具，并以 MySQL Trace、Approval、Replay 和 Redis 安全控制形成完整 Harness。

## 项目结构

- `agent`：API、任务编排、Trace、Approval、Replay、MCP Server/Client、业务与数据访问。
- `agent-worker`：可独立部署和横向扩容的 Supervisor、Planner、Risk、Reporter Agent。
- `gateway`：统一入口，基于 Nacos 路由到 Agent。
- `agent-chat-ui`：任务、Agent Step、工具调用、模型调用和审批控制台。

## 技术栈

JDK 21、Spring Boot 3.5、Spring AI 1.1、MCP Streamable HTTP、MySQL、Redis、Elasticsearch、Nacos、Spring Cloud Gateway、React、Docker Compose。

## 架构

```text
Browser -> Gateway :18080 -> Agent :8050
                              |-> Agent Worker :8060 (Nacos/HTTP)
                              |-> MCP Client -> /mcp -> MCP Tools
                              |-> MySQL / Redis / Elasticsearch
```

Agent Worker 不可用时回退本地角色；MCP 仅在连接故障时回退本地工具。业务错误不会重试。取消订单必须经过 Approval，并使用 Redis 一次性授权阻止外部 MCP Client 绕过审批。

## 本地启动（IDEA）

1. 创建数据库：`CREATE DATABASE aftersales_agent DEFAULT CHARACTER SET utf8mb4;`
2. 执行 `docker compose up -d`，只启动 Redis、Elasticsearch、Nacos。
3. 在 IDEA 依次运行 `AgentWorkerApplication`（8060）、`AgentApplication`（8050）、`GatewayApplication`（18080）。
4. 给 Worker 和 Agent 两个 Run Configuration 都配置 `AI_API_KEY=你的密钥`；MySQL 密码不是默认值时再配置 `MYSQL_PASSWORD`。
5. 在 `agent-chat-ui` 执行 `npm install`、`npm start`，访问 http://localhost:3000。
6. 初始化商品索引：`POST http://localhost:18080/api/agent/products/index/rebuild`。

Agent 启动时自动执行 `schema.sql` 和 `data.sql`。测试数据包括用户 10086、5 个商品、订单 10001/10002/10003。

## Docker Compose

Compose 不再构建或启动 Java 与前端应用，只管理本地依赖：

```powershell
docker compose up -d
docker compose ps
```

Docker Desktop 中 Redis、Elasticsearch、Nacos 归入 `aftersales` 分组。ES 主机端口为 19200；MySQL 使用宿主机安装。

## 模型配置

Agent 与 Worker 支持 OpenAI-compatible API：`AI_ENABLED`、`AI_MODEL`、`AI_BASE_URL`、`AI_API_KEY`、`AI_CHAT_MODEL`。密钥只放环境变量。模型失败时回退规则。

## Nacos 配置

- `aftersales-agent.yaml`：MCP 地址、maxToolCalls、缓存、审批、ES、Worker 地址。
- `aftersales-agent-worker.yaml`：模型开关及模型参数。

配置在启动时加载；服务注册名为 `aftersales-agent`、`aftersales-agent-worker`、`aftersales-gateway`。

## Harness

- Trace：`agent_run`、`agent_step`、`tool_call_log`、`model_call_log`。
- Approval：取消前订单不变化，批准后签发一次性授权并通过 MCP 取消。
- Replay：按原始输入创建新 run，并保存来源 runId。
- Guard：`maxToolCalls` 使用 Redis 原子计数强制执行。

## 核心接口

- `POST /api/agent/chat`
- `GET /api/agent/runs/{runId}`
- `GET /api/agent/runs/{runId}/tool-calls`
- `GET /api/agent/runs/{runId}/model-calls`
- `POST /api/agent/runs/{runId}/replay`
- `GET /api/agent/approvals/pending`
- `POST /api/agent/approvals/{id}/approve|reject`

Swagger：`http://localhost:8050/swagger-ui/index.html`。MCP：`http://localhost:8050/mcp`。

## 验证

- 售后：用户 10086 最近订单满足延迟条件，验证 ticket、run、tool/model logs。
- 取消：订单 10001，验证审批前不变、批准后 CANCELLED、重复取消不新增审批。
- 商品：重建索引后咨询 5000 元轻薄本，验证 ES 结果和 product.searchProducts。

## 测试

```powershell
mvn test
cd agent-chat-ui
npm run build
```

## 后续扩展

可增加消息队列异步 Agent、模型成本统计、OpenTelemetry、细粒度租户权限和独立业务微服务。
