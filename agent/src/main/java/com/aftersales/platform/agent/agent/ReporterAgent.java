package com.aftersales.platform.agent.agent;

import com.aftersales.platform.agent.domain.Product;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReporterAgent {
    private static final String AFTER_SALES_SYSTEM = """
            你是专业电商客服。请根据订单、商品和售后政策生成简洁中文回复。
            必须说明已创建售后工单、会优先跟进发货或补偿，不得承诺政策之外的赔偿。
            """;
    private static final String PRODUCT_SYSTEM = """
            你是商品顾问。只能基于给定 Elasticsearch 搜索结果推荐，
            说明商品名、价格和适合原因，不得虚构商品或参数。
            """;

    private final AiModelService aiModel;

    public ReporterAgent(AiModelService aiModel) {
        this.aiModel = aiModel;
    }

    /** 汇总售后结果；模型不可用时返回确定性客服文案。 */
    public String afterSalesReply(Long runId, Long orderId, String productName, String policy) {
        String facts = "订单ID：" + orderId + "\n商品：" + productName + "\n售后政策：" + policy;
        return aiModel.generate(runId, "reporter.afterSales", AFTER_SALES_SYSTEM, facts)
                .orElseGet(() -> defaultAfterSalesReply(orderId, productName, policy));
    }

    public String cancelWaiting(Long approvalId, Long orderId) {
        return "订单" + orderId + "属于敏感取消操作，已创建审批单" + approvalId + "。审批通过前不会真正取消订单。";
    }

    public String cancelApproved(Long orderId) {
        return "审批已通过，订单" + orderId + "已取消，状态变为 CANCELLED。";
    }

    public String productAdvice(Long runId, List<Product> products) {
        if (products.isEmpty()) {
            return "没有找到匹配商品，请调整预算或关键词。";
        }

        String facts = products.stream()
                .map(product -> "商品ID=" + product.id()
                        + "，名称=" + product.name()
                        + "，价格=" + product.price()
                        + "，描述=" + product.description())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");

        return aiModel.generate(runId, "reporter.productAdvice", PRODUCT_SYSTEM, facts)
                .orElseGet(() -> defaultProductAdvice(products));
    }

    private String defaultAfterSalesReply(Long orderId, String productName, String policy) {
        return "订单" + orderId + "已超过承诺发货时效，我们已按" + productName
                + "的售后政策创建工单。客服回复：非常抱歉让您久等，我们已为您升级处理并会优先跟进发货或补偿方案。售后政策："
                + policy;
    }

    private String defaultProductAdvice(List<Product> products) {
        StringBuilder builder = new StringBuilder("基于 Elasticsearch 搜索结果，推荐：");
        for (Product product : products) {
            builder.append("\n- ")
                    .append(product.name())
                    .append("，价格 ")
                    .append(product.price())
                    .append("，理由：")
                    .append(product.description());
        }
        return builder.toString();
    }
}
