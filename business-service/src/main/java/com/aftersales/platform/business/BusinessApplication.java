package com.aftersales.platform.business;

import com.aftersales.platform.business.config.BusinessProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Business 服务启动入口，承载业务数据访问以及 Streamable HTTP MCP Server。
 */
@SpringBootApplication
@EnableConfigurationProperties(BusinessProperties.class)
public class BusinessApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusinessApplication.class, args);
    }
}
