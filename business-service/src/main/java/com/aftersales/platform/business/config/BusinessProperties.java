package com.aftersales.platform.business.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Business 可变配置模型，集中管理延迟阈值、缓存时间和 ES 连接参数。
 */
@ConfigurationProperties(prefix = "business")
public class BusinessProperties {
    // 已付款未发货超过该阈值，才满足延迟发货条件。
    private int delayedShipmentThresholdHours = 48;
    // 商品、订单缓存以及工单幂等键的默认有效时间。
    private long redisCacheSeconds = 600;
    // 商品搜索使用的 Elasticsearch 索引和服务地址。
    private String elasticsearchIndexName = "aftersales_products";
    private String elasticsearchUrl = "http://localhost:19200";

    public int getDelayedShipmentThresholdHours() {
        return delayedShipmentThresholdHours;
    }

    public void setDelayedShipmentThresholdHours(int value) {
        this.delayedShipmentThresholdHours = value;
    }

    public long getRedisCacheSeconds() {
        return redisCacheSeconds;
    }

    public void setRedisCacheSeconds(long value) {
        this.redisCacheSeconds = value;
    }

    public String getElasticsearchIndexName() {
        return elasticsearchIndexName;
    }

    public void setElasticsearchIndexName(String value) {
        this.elasticsearchIndexName = value;
    }

    public String getElasticsearchUrl() {
        return elasticsearchUrl;
    }

    public void setElasticsearchUrl(String value) {
        this.elasticsearchUrl = value;
    }
}
