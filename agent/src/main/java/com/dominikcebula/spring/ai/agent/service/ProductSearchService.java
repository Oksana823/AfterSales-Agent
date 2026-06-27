package com.dominikcebula.spring.ai.agent.service;

import com.dominikcebula.spring.ai.agent.config.HarnessProperties;
import com.dominikcebula.spring.ai.agent.domain.Product;
import com.dominikcebula.spring.ai.agent.repository.ProductRepository;
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

@Service
public class ProductSearchService {
    private final ProductRepository repository;
    private final RestClient restClient;
    private final HarnessProperties properties;
    private final ObjectMapper mapper;

    public ProductSearchService(ProductRepository repository, RestClient restClient, HarnessProperties properties, ObjectMapper mapper) {
        this.repository = repository;
        this.restClient = restClient;
        this.properties = properties;
        this.mapper = mapper;
    }

    /** 商品咨询必须走 Elasticsearch，这里只解析商品搜索结果，不回退到 MySQL。 */
    public List<Product> search(String keyword) {
        try {
            Map<String, Object> query = Map.of(
                    "query", Map.of(
                            "multi_match", Map.of(
                                    "query", keyword,
                                    "fields", List.of("name^3", "category", "brand", "tags^2", "description")
                            )
                    ),
                    "size", 5
            );

            Request request = new Request("POST", "/" + properties.getElasticsearchIndexName() + "/_search");
            request.setJsonEntity(mapper.writeValueAsString(query));

            Response response = restClient.performRequest(request);
            Map<String, Object> responseBody = mapper.readValue(EntityUtils.toString(response.getEntity()), new TypeReference<>() {});
            Map<String, Object> hitsBody = (Map<String, Object>) responseBody.get("hits");
            List<Map<String, Object>> hits = (List<Map<String, Object>>) hitsBody.get("hits");

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

    /** 将 MySQL 中的测试商品同步到 Elasticsearch，便于本地一键验证商品咨询流程。 */
    public void rebuildIndex() {
        try {
            restClient.performRequest(new Request("PUT", "/" + properties.getElasticsearchIndexName()));
        } catch (Exception ignored) {
            // 索引已存在时继续覆盖文档即可。
        }

        for (Product product : repository.findAll()) {
            try {
                Request request = new Request("PUT", "/" + properties.getElasticsearchIndexName() + "/_doc/" + product.id());
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
