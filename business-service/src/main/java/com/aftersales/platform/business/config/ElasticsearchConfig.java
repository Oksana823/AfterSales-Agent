package com.aftersales.platform.business.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * 根据业务配置创建 Elasticsearch 低级 RestClient，并由 Spring 管理连接生命周期。
 */
@Configuration
public class ElasticsearchConfig {
    @Bean
    RestClient restClient(BusinessProperties properties) {
        URI uri = URI.create(properties.getElasticsearchUrl());
        int port = uri.getPort() > 0 ? uri.getPort() : 9200;
        return RestClient.builder(new HttpHost(uri.getHost(), port, uri.getScheme())).build();
    }
}
