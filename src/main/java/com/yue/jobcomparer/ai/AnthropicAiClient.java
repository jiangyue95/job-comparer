package com.yue.jobcomparer.ai;

import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class AnthropicAiClient implements AiClient{

    private final ChatClient chatClient;

    public AnthropicAiClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String chat(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    @Override
    public String chat(String prompt, int maxTokens) {
        return chatClient.prompt()
                .user(prompt)
                .options(AnthropicChatOptions.builder().maxTokens(maxTokens).build())
                .call()
                .content();
    }
}
