package com.anyui.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import java.util.List;
import java.util.Map;

@Service
public class AiAuditService {

    // ✅ Spring AI 自动注入的核心客户端
    private final ChatClient chatClient;

    @Autowired
    public AiAuditService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 审核文本内容 (修复版)
     */
    public AuditResult auditText(String content) {
        // 1. 定义系统提示词 (这里的大括号 {} 不会被解析为变量，安全！)
        String systemText = """
            你是一个严厉的内容审核员。
            请判断以下内容是否违规（色情、暴力、政治、辱骂、广告）。
            请严格只返回一个JSON对象，格式为：{"pass": true, "reason": "通过"} 或 {"pass": false, "reason": "具体的拒绝原因"}。
            不要返回Markdown格式，直接返回纯JSON字符串。
            """;

        // 2. 构造消息列表
        // SystemMessage: 设定 AI 角色和规则
        Message systemMessage = new SystemMessage(systemText);
        // UserMessage: 传入用户的实际内容
        Message userMessage = new UserMessage("待审核内容：" + content);

        try {
            // 3. 构造 Prompt 并调用 (这里改用了 List<Message>)
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
            String response = chatClient.call(prompt).getResult().getOutput().getContent();

            // 4. 清洗和解析 (保持不变)
            String cleanJson = response.replace("```json", "").replace("```", "").trim();

            boolean pass = cleanJson.contains("\"pass\": true") || cleanJson.contains("\"pass\":true");
            String reason = "审核通过";

            if (!pass) {
                try {
                    int start = cleanJson.indexOf("\"reason\":");
                    if (start != -1) {
                        String sub = cleanJson.substring(start + 9);
                        int firstQuote = sub.indexOf("\"");
                        int secondQuote = sub.indexOf("\"", firstQuote + 1);
                        if (firstQuote != -1 && secondQuote != -1) {
                            reason = sub.substring(firstQuote + 1, secondQuote);
                        }
                    }
                } catch (Exception e) {
                    reason = "内容违规";
                }
            }

            return new AuditResult(pass, reason);

        } catch (Exception e) {
            e.printStackTrace();
            // 记得要抛出异常，触发 Service 层的降级逻辑 (转人工)
            throw new RuntimeException("AI服务调用失败", e);
        }
    }

    // 简单的结果内部类
    public static class AuditResult {
        public boolean pass;
        public String reason;

        public AuditResult(boolean pass, String reason) {
            this.pass = pass;
            this.reason = reason;
        }
    }
}