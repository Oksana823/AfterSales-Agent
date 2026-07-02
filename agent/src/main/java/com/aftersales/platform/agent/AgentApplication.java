package com.aftersales.platform.agent;

import com.aftersales.platform.agent.config.HarnessProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Agent 服务启动入口，承载任务编排、Trace、Approval、Replay 和 MCP Client。
 */
@SpringBootApplication
@EnableConfigurationProperties(HarnessProperties.class)
public class AgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
