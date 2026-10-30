package com.adria.bank.transaction.web.rest;

import java.math.BigDecimal;

public record CreateTransactionRequestRest(Long accountId, String currency, BigDecimal amount) {
}
