package com.adria.bank.transaction.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BankTransactionProperties.class)
@EnableFeignClients(basePackages = "com.adria.bank.transaction.client")
public class TransactionConfiguration {
}
