package com.adria.bank.transaction.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionMcpConfiguration {

    @Bean
    public ToolCallbackProvider transactionToolCallbackProvider(TransactionMcpToolService transactionMcpToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(transactionMcpToolService)
                .build();
    }
}
