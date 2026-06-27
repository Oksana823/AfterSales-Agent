# 智能售后订单 Multi-Agent MCP Harness

基于 Spring AI、MCP 和 Spring Cloud 的智能售后微服务平台。系统将自然语言任务转换为可审计的 Agent 决策、MCP 工具调用、审批流程和业务操作。

## 模块

- `common`：跨服务共享的订单、商品、工单 DTO 与枚举。
- `agent`：任务编排、Trace、Approval、Replay、MCP Client。
- `business-service`：订单、商品、工单、MySQL、Redis、Elasticsearch、MCP Server。
- `agent-worker`：Supervisor、Planner、Risk、Reporter 及 LLM 调用。
- `gateway`：统一入口和 Nacos 负载均衡路由。
- `agent-chat-ui`：任务、轨迹、模型调用和审批控制台。

## 架构

```text
Browser :3000
  -> Gateway :18080
       |-> /api/agent/**    -> Agent :8050
       |-> /api/business/** -> Business :8070

Agent -> Nacos -> Agent Worker :8060
Agent -> Nacos -> Business MCP Server /mcp
Business -> MySQL / Redis / Elasticsearch
```

Gateway 只管理外部 API。Agent 调用 Worker 和 Business 属于内部服务调用，不绕回 Gateway。Business 不可用时 Agent 不会回退本地 Repository，从而保持真实微服务边界。

## 技术栈

JDK 21、Spring Boot 3.5、Spring AI 1.1、MCP Streamable HTTP、MySQL、Redis、Elasticsearch、Nacos、Spring Cloud Gateway、React、Docker Compose。

## 本地启动

1. 创建数据库：`CREATE DATABASE aftersales_agent DEFAULT CHARACTER SET utf8mb4;`
2. 执行 `docker compose up -d`，只启动 Redis、Elasticsearch、Nacos。
3. IDEA Reload Maven。
4. 依次启动 `AgentWorkerApplication`（8060）、`BusinessApplication`（8070）、`AgentApplication`（8050）、`GatewayApplication`（18080）。
5. Worker 和 Agent 配置 `AI_API_KEY`；四个服务本地运行时配置 `SPRING_CLOUD_NACOS_DISCOVERY_IP=127.0.0.1`。
6. `agent-chat-ui` 执行 `npm install`、`npm start`，访问 http://localhost:3000。
7. 初始化 ES：`POST http://localhost:18080/api/business/products/index/rebuild`。

`.run/Run All.run.xml` 已包含四个后端服务。

## Gateway 路由

- `/api/agent/**` -> `lb://aftersales-agent`
- `/api/business/**` -> `lb://aftersales-business`

Worker 和 Business MCP 接口不通过 Gateway 对外暴露。

## Nacos 配置

`aftersales-agent.yaml`：

```yaml
harness:
  llm-enabled: true
  business-service-name: aftersales-business
  mcp-client-enabled: true
  mcp-request-timeout-seconds: 10
  max-tool-calls: 12
  distributed-agents-enabled: true
  agent-worker-url: http://aftersales-agent-worker
  approval-enabled: true
```

`aftersales-business.yaml`：

```yaml
business:
  delayed-shipment-threshold-hours: 48
  redis-cache-seconds: 600
  elasticsearch-index-name: aftersales_products
  elasticsearch-url: http://localhost:19200
```

`aftersales-agent-worker.yaml` 管理模型开关、Base URL、模型名和温度。API Key 只放 IDEA 环境变量。

## 核心流程

- 售后：查询最近订单 -> 判断延迟 -> 查询商品政策 -> 生成回复 -> MCP 创建工单。
- 取消：查询订单 -> Risk 判敏感 -> 创建审批 -> Redis 一次性授权 -> MCP 取消订单。
- 咨询：提取需求 -> MCP 调用 Elasticsearch 搜索 -> Reporter 生成推荐。

## MCP 与 Harness

Business 在 `http://localhost:8070/mcp` 注册 10 个工具。Agent 使用 Nacos发现 Business 实例并建立 Streamable HTTP MCP 连接。

- Trace：`agent_run`、`agent_step`、`tool_call_log`、`model_call_log`。
- Approval：取消订单必须审批，授权只能消费一次。
- Replay：根据旧 run 的原始输入创建新 run。
- Guard：Redis 对每个 run 原子限制 `maxToolCalls`。

## API

- `POST /api/agent/chat`
- `GET /api/agent/runs/{runId}`
- `POST /api/agent/runs/{runId}/replay`
- `GET /api/agent/approvals/pending`
- `POST /api/agent/approvals/{id}/approve|reject`
- `GET /api/business/orders/{id}`
- `GET /api/business/products/search?keyword=...`
- `POST /api/business/products/index/rebuild`

Agent Swagger：http://localhost:8050/swagger-ui/index.html

## 验证

1. 售后任务执行后检查 `ticket`、`agent_run`、`tool_call_log`。
2. 取消任务审批前订单不变，批准后状态为 `CANCELLED`。
3. 商品咨询前重建索引，确认工具日志包含 `product.searchProducts`。
4. Agent Step 中应看到 Worker 调用为 `REMOTE`；Agent 日志不注册 MCP Tool，Business 日志应显示 10 个工具。

## 测试

```powershell
mvn test
cd agent-chat-ui
npm run build
```

## 后续扩展

可继续增加统一鉴权、限流、OpenTelemetry、消息队列、模型成本统计和独立数据库所有权。
