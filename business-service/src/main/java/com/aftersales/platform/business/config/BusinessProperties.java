package com.aftersales.platform.business.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "business")
public class BusinessProperties {
    private int delayedShipmentThresholdHours = 48;
    private long redisCacheSeconds = 600;
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
