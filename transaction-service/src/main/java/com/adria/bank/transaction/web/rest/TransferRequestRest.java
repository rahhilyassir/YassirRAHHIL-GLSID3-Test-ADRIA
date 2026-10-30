package com.adria.bank.transaction.web.rest;

import java.math.BigDecimal;

public record TransferRequestRest(Long fromAccountId, Long toAccountId, BigDecimal amount, String currency) {
}
