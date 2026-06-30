package com.aftersales.platform.business.service;

import com.aftersales.platform.business.config.BusinessProperties;
import com.aftersales.platform.common.domain.Product;
import com.aftersales.platform.business.repository.ProductRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductService {
    private final ProductRepository repository;
    private final ProductSearchService searchService;
    private final StringRedisTemplate redis;
    private final BusinessProperties properties;
    private final JsonService json;

    public ProductService(ProductRepository repository, ProductSearchService searchService, StringRedisTemplate redis,
                          BusinessProperties properties, JsonService json) {
        this.repository = repository;
        this.searchService = searchService;
        this.redis = redis;
        this.properties = properties;
        this.json = json;
    }

    public Product getProduct(Long productId) {
        Product product = repository.findById(productId).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        redis.opsForValue().set("cache:product:" + productId, json.toJson(product), properties.getRedisCacheSeconds(),
                TimeUnit.SECONDS);
        return product;
    }

    public String policy(Long productId) {
        return getProduct(productId).afterSalesPolicy();
    }

    public List<Product> search(String keyword) {
        return searchService.search(keyword);
    }

    public void rebuildIndex() {
        searchService.rebuildIndex();
    }
}
