package com.dominikcebula.spring.ai.agent.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;

@Configuration
public class ElasticsearchConfig {
    @Bean
    RestClient restClient(HarnessProperties properties) {
        URI uri = URI.create(properties.getElasticsearchUrl());
        int port = uri.getPort() > 0 ? uri.getPort() : 9200;
        return RestClient.builder(new HttpHost(uri.getHost(), port, uri.getScheme())).build();
    }
}
