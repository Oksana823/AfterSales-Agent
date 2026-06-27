package com.aftersales.platform.agent.agent;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NlpExtractor {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    public Long firstNumber(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("输入不能为空");
        }

        Matcher matcher = NUMBER_PATTERN.matcher(input);

        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }

        throw new IllegalArgumentException("未识别到数字ID");
    }

    public String cancelReason(String input) {
        if (input == null || input.isBlank()) {
            return "用户申请取消";
        }

        int index = input.indexOf("理由");

        if (index >= 0) {
            return input.substring(index)
                    .replace("理由是", "")
                    .replace("理由", "")
                    .trim();
        }

        return "用户申请取消";
    }

    public String productKeyword(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        return input
                .replace("帮我", "")
                .replace("找一个", "")
                .replace("适合", "")
                .trim();
    }
}