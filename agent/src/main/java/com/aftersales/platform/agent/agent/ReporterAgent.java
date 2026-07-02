package com.aftersales.platform.agent.agent;

import com.aftersales.platform.common.domain.Product;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reporter 角色，将可信业务结果生成客服回复或商品推荐，并在模型失败时诚实降级。
 */
@Component
public class ReporterAgent {
    private static final String AFTER_SALES_SYSTEM = """
            你是专业电商客服。工单已经真实创建，请根据订单、工单、商品和售后政策生成简洁中文回复。
            只能陈述给定事实，不得虚构补偿、处理进度或承诺政策之外的结果。
            """;
    private static final String PRODUCT_SYSTEM = """
            你是商品顾问。只能基于给定 Elasticsearch 搜索结果推荐，
            说明商品名、价格和适合原因，不得虚构商品或参数。
            """;
    private final AiModelService aiModel;

    public ReporterAgent(AiModelService aiModel) {
        this.aiModel = aiModel;
    }

    public String afterSalesReply(Long runId, Long ticketId, Long orderId, String productName, String policy) {
        String nl = System.lineSeparator();
        String facts = "工单ID：" + ticketId + nl + "订单ID：" + orderId + nl + "商品：" + productName + nl + "售后政策：" +
                policy;
        AiModelService.ModelResult result = aiModel.generate(runId, "reporter.afterSales", AFTER_SALES_SYSTEM, facts);
        return result.success() ? result.content() : defaultAfterSalesReply(ticketId, orderId, productName, policy);
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
        String nl = System.lineSeparator();
        String facts = products.stream().map(
                p -> "商品ID=" + p.id() + "，名称=" + p.name() + "，价格=" + p.price() + "，描述=" +
                        p.description()).reduce((a, b) -> a + nl + b).orElse("");
        AiModelService.ModelResult result = aiModel.generate(runId, "reporter.productAdvice", PRODUCT_SYSTEM, facts);
        return result.success() ? result.content() : defaultProductAdvice(products);
    }

    private String defaultAfterSalesReply(Long ticketId, Long orderId, String productName, String policy) {
        return "【系统提示：智能客服暂时不可用，以下为系统已确认的处理结果】" + System.lineSeparator()
                + "订单" + orderId + "（" + productName + "）已创建售后工单" + ticketId
                + "，后续由人工客服处理。售后政策：" + policy;
    }

    private String defaultProductAdvice(List<Product> products) {
        StringBuilder text = new StringBuilder(
                "【系统提示：智能推荐暂时不可用，以下为 Elasticsearch 检索结果，未经过 AI 分析】");
        for (Product p : products) {
            text.append(System.lineSeparator()).append("■ ").append(p.name()).append("  ￥").append(p.price()).append(
                    " | ").append(p.description());
        }
        return text.toString();
    }
}
