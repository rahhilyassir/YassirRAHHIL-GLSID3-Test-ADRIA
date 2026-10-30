package com.adria.bank.transaction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "bank.transaction")
@RefreshScope
@Getter
@Setter
public class BankTransactionProperties {
    private BigDecimal maxAmount = new BigDecimal("1000");
}
