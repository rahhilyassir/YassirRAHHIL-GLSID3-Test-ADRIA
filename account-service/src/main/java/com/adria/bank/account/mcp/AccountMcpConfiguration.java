package com.adria.bank.account.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountMcpConfiguration {

    @Bean
    public ToolCallbackProvider accountToolCallbackProvider(AccountMcpToolService accountMcpToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(accountMcpToolService)
                .build();
    }
}
