package com.yue.jobcomparer.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

// DeepSeek exposes an OpenAI-compatible API, so we reuse Spring AI's
// OpenAI client with base-url pointed at api.deepseek.com.
@Component
public class DeepSeekAiClient implements AiClient {

    private final ChatClient chatClient;

    public DeepSeekAiClient(OpenAiChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.DEEPSEEK;
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
                .options(OpenAiChatOptions.builder().maxTokens(maxTokens).build())
                .call()
                .content();
    }
}
