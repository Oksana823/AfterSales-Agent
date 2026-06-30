package com.aftersales.platform.business.mcp;

import com.aftersales.platform.common.domain.Product;
import com.aftersales.platform.business.service.ProductService;
import com.aftersales.platform.business.service.BusinessToolLogService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProductTools {
    private final ProductService productService;
    private final BusinessToolLogService log;

    public ProductTools(ProductService productService, BusinessToolLogService log) {
        this.productService = productService;
        this.log = log;
    }

    @Tool(description = "通过 Elasticsearch 搜索商品")
    public List<Product> searchProducts(@ToolParam(description = "Agent run id") Long runId,
                                        @ToolParam(description = "搜索关键词") String keyword) {
        return log.call(runId, "product.searchProducts", Map.of("keyword", keyword),
                () -> productService.search(keyword));
    }

    @Tool(description = "查询商品详情")
    public Product getProduct(@ToolParam(description = "Agent run id") Long runId,
                              @ToolParam(description = "商品ID") Long productId) {
        return log.call(runId, "product.getProduct", Map.of("productId", productId),
                () -> productService.getProduct(productId));
    }

    @Tool(description = "查询商品售后政策")
    public String getAfterSalesPolicy(@ToolParam(description = "Agent run id") Long runId,
                                      @ToolParam(description = "商品ID") Long productId) {
        return log.call(runId, "product.getAfterSalesPolicy", Map.of("productId", productId),
                () -> productService.policy(productId));
    }
}
