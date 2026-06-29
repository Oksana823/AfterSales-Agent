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
Frontend :3000
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
5. Worker 和 Agent 配置 `AI_API_KEY`；MySQL 默认密码为 `666666`，需要修改时可用 `MYSQL_PASSWORD` 覆盖；四个服务本地运行时配置 `SPRING_CLOUD_NACOS_DISCOVERY_IP=127.0.0.1`。
6. `agent-chat-ui` 执行 `npm install`、`npm start`，访问 http://localhost:3000。
7. 初始化 ES：`POST http://localhost:18080/api/business/products/index/rebuild`。


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

- 售后：查询最近订单 -> 判断延迟 -> 查询商品政策 -> MCP 创建工单 -> 生成并回写客服回复。

  ```mermaid
  sequenceDiagram
      participant UI as Frontend
      participant A as Agent
      participant W as Worker
      participant B as Business MCP
      participant DB as MySQL/Redis

      UI->>A: 自然语言售后任务
      A->>W: Supervisor分类、Planner计划
      A->>B: getLatestOrder
      B->>DB: 查询最近订单
      A->>B: isDelayedShipment
      A->>B: getProduct / getAfterSalesPolicy
      A->>B: createTicket（先真实创建）
      B-->>A: ticketId
      A->>W: Reporter生成客服回复（携带真实ticketId）
      A->>B: updateTicketCustomerReply
      A-->>UI: answer + status + warnings
  ```

- 取消：查询订单 -> Risk 判敏感 -> 创建审批 -> Redis 一次性授权 -> MCP 取消订单。

  ```mermaid
  sequenceDiagram
      participant U as User
      participant A as Agent
      participant W as Worker Risk
      participant R as Redis
      participant B as Business MCP
      participant DB as MySQL

      U->>A: 取消订单
      A->>B: getOrder
      A->>W: sensitive(cancelOrder)
      W-->>A: true
      A->>DB: 创建 approval_request=PENDING
      A-->>U: 等待人工审批
      U->>A: approve
      A->>R: 签发2分钟一次性授权
      A->>B: cancelOrder
      B->>R: Lua原子读取并删除授权
      B->>DB: 条件更新订单为CANCELLED
      A->>DB: approval_request=APPROVED
  ```

- 咨询：提取需求 -> MCP 调用 Elasticsearch 搜索 -> Reporter 生成推荐。

  ```mermaid
  sequenceDiagram
      participant U as User
      participant A as Agent
      participant W as Worker
      participant B as Business MCP
      participant ES as Elasticsearch

      U->>A: 商品需求
      A->>W: 分类与计划
      A->>B: searchProducts
      B->>ES: multi_match搜索
      ES-->>A: 真实商品列表
      A->>W: Reporter根据搜索结果生成文案
      W-->>A: 推荐文案或诚实降级结果
      A-->>U: 商品结果
  ```

## MCP 与 Harness

Business 在 `http://localhost:8070/mcp` 注册 11 个工具。Agent 使用 Nacos发现 Business 实例并建立 Streamable HTTP MCP 连接；新增工单回复回写工具，保证先创建工单再生成客服文案。

Planner 返回结构化 `ExecutionPlan`，Agent 通过 `PlanValidator` 校验任务类型、动作白名单、顺序、条件和审批约束，再由 `PlanExecutor` 逐步调用 MCP/Agent。非法计划会明确记录 Trace 并回退标准计划；取消动作只在审批回调中恢复执行。

- Trace：`agent_run`、`agent_step`、`tool_call_log`、`model_call_log`。
- Approval：取消订单必须审批，授权只能消费一次。
- Replay：根据旧 run 的原始输入创建新 run。
- Guard：Redis 对每个 run 原子限制 `maxToolCalls`。
- Degradation：模型失败按场景诚实降级，run 标记为 `COMPLETED_WITH_WARNINGS`，API 和前端展示 warnings；无法规则分类时返回 503。

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
4. Agent Step 中应看到 Worker 调用为 `REMOTE`；Agent 日志不注册 MCP Tool，Business 日志应显示 11 个工具。

## 测试

```powershell
mvn test
cd agent-chat-ui
npm run build
```

## 后续扩展

可继续增加统一鉴权、限流、OpenTelemetry、消息队列、模型成本统计和独立数据库所有权。
