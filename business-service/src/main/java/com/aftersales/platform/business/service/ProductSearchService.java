package com.aftersales.platform.business.service;

import com.aftersales.platform.business.config.BusinessProperties;
import com.aftersales.platform.common.domain.Product;
import com.aftersales.platform.business.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 商品检索服务，负责构造查询、解析结果和重建商品索引。
 */
@Service
public class ProductSearchService {
    private final ProductRepository repository;
    private final RestClient restClient;
    private final BusinessProperties properties;
    private final ObjectMapper mapper;

    public ProductSearchService(ProductRepository repository, RestClient restClient, BusinessProperties properties,
                                ObjectMapper mapper) {
        this.repository = repository;
        this.restClient = restClient;
        this.properties = properties;
        this.mapper = mapper;
    }

    /**
     * 商品咨询必须走 Elasticsearch，这里只解析商品搜索结果，不回退到 MySQL。
     */
    public List<Product> search(String keyword) {
        try {
            // ===== 1) 构造 multi_match 查询，并通过字段权重提升名称和标签的相关性 =====
            Map<String, Object> query = Map.of(
                    "query", Map.of(
                            "multi_match", Map.of(
                                    "query", keyword,
                                    "fields", List.of("name^3", "category", "brand", "tags^2", "description")
                            )
                    ),
                    "size", 5
            );

            // ===== 2) 商品咨询只访问 Elasticsearch，不允许悄悄回退 MySQL 冒充搜索结果 =====
            Request request = new Request("POST", "/" + properties.getElasticsearchIndexName() + "/_search");
            request.setJsonEntity(mapper.writeValueAsString(query));

            Response response = restClient.performRequest(request);
            // ===== 3) 解析 ES hits，并显式映射成跨服务 Product 模型 =====
            Map<String, Object> responseBody = mapper.readValue(EntityUtils.toString(response.getEntity()),
                    new TypeReference<>() {
                    });
            Map<String, Object> hitsBody = (Map<String, Object>) responseBody.get("hits");
            List<Map<String, Object>> hits = (List<Map<String, Object>>) hitsBody.get("hits");

            // ===== 4) 只返回 _source 中的可信字段，Reporter 不自行补造商品参数 =====
            List<Product> products = new ArrayList<>();
            for (Map<String, Object> hit : hits) {
                Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                products.add(new Product(
                        ((Number) source.get("productId")).longValue(),
                        (String) source.get("name"),
                        (String) source.get("category"),
                        (String) source.get("brand"),
                        new BigDecimal(String.valueOf(source.get("price"))),
                        (String) source.get("tags"),
                        (String) source.get("description"),
                        (String) source.get("afterSalesPolicy")
                ));
            }
            return products;
        } catch (Exception e) {
            throw new IllegalStateException("Elasticsearch 商品搜索失败，请确认索引已初始化: " + e.getMessage(), e);
        }
    }

    /**
     * 将 MySQL 中的测试商品同步到 Elasticsearch，便于本地一键验证商品咨询流程。
     */
    public void rebuildIndex() {
        // ===== 1) 先确保索引存在；已存在时报错可忽略，随后按商品 ID 覆盖文档 =====
        try {
            restClient.performRequest(new Request("PUT", "/" + properties.getElasticsearchIndexName()));
        } catch (Exception ignored) {
            // 索引已存在时继续覆盖文档即可。
        }

        // ===== 将 MySQL 商品作为索引数据源，逐条同步到 Elasticsearch =====
        for (Product product : repository.findAll()) {
            try {
                Request request = new Request("PUT",
                        "/" + properties.getElasticsearchIndexName() + "/_doc/" + product.id());
                request.setJsonEntity(mapper.writeValueAsString(Map.of(
                        "productId", product.id(),
                        "name", product.name(),
                        "category", product.category(),
                        "brand", product.brand(),
                        "price", product.price(),
                        "tags", product.tags(),
                        "description", product.description(),
                        "afterSalesPolicy", product.afterSalesPolicy()
                )));
                restClient.performRequest(request);
            } catch (Exception e) {
                throw new IllegalStateException("同步商品索引失败: " + e.getMessage(), e);
            }
        }
    }
}
