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
public class AgentWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentWorkerApplication.class, args);
    }
}
