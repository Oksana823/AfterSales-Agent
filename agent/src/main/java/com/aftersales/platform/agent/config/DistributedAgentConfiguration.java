package com.aftersales.platform.agent.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 分布式 Agent 配置，为 RestClient 注入 Spring Cloud 负载均衡能力。
 */
@Configuration
public class DistributedAgentConfiguration {

    @Bean
    @LoadBalanced
    public RestClient.Builder agentRestClientBuilder() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        return RestClient.builder().requestFactory(requestFactory);
    }
}
