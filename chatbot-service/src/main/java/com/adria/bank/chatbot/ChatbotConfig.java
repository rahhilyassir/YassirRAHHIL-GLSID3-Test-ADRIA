package com.adria.bank.chatbot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
public class ChatbotConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ObjectProvider<ToolCallbackProvider> toolCallbackProviders) {
        ToolCallback[] callbacks = toolCallbackProviders.orderedStream()
                .flatMap(provider -> Arrays.stream(provider.getToolCallbacks()))
                .toArray(ToolCallback[]::new);

        return ChatClient.builder(chatModel)
                .defaultSystem("You are a bank assistant. Use the tools to answer. Never invent data. Ask for missing information (account id, amount, currency) before executing any operation. Confirm the result.")
                .defaultTools(callbacks)
                .build();
    }
}
