package com.adria.bank.transaction.service;

import java.math.BigDecimal;

public record CreateTransactionRequest(Long accountId, String currency, BigDecimal amount) {
}
