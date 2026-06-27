package com.dominikcebula.spring.ai.worker;

import com.dominikcebula.spring.ai.agent.agent.AiModelService;
import com.dominikcebula.spring.ai.agent.agent.PlannerAgent;
import com.dominikcebula.spring.ai.agent.agent.ReporterAgent;
import com.dominikcebula.spring.ai.agent.agent.RiskAgent;
import com.dominikcebula.spring.ai.agent.agent.SupervisorAgent;
import com.dominikcebula.spring.ai.agent.config.HarnessProperties;
import com.dominikcebula.spring.ai.agent.repository.ModelCallTraceRepository;
import com.dominikcebula.spring.ai.agent.service.ModelCallTraceService;
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
