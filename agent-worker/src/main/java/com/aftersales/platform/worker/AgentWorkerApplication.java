package com.aftersales.platform.worker;

import com.aftersales.platform.agent.agent.AiModelService;
import com.aftersales.platform.agent.agent.PlannerAgent;
import com.aftersales.platform.agent.agent.ReporterAgent;
import com.aftersales.platform.agent.agent.RiskAgent;
import com.aftersales.platform.agent.agent.SupervisorAgent;
import com.aftersales.platform.agent.config.HarnessProperties;
import com.aftersales.platform.agent.repository.ModelCallTraceRepository;
import com.aftersales.platform.agent.service.ModelCallTraceService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableConfigurationProperties(HarnessProperties.class)
@Import({
        AiModelService.class,
        SupervisorAgent.class,
        PlannerAgent.class,
        RiskAgent.class,
        ReporterAgent.class,
        ModelCallTraceRepository.class,
        ModelCallTraceService.class
})
/**
 * Agent Worker 服务启动入口，仅承载可独立部署的 Agent 角色与模型调用能力。
 */
public class AgentWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentWorkerApplication.class, args);
    }
}
