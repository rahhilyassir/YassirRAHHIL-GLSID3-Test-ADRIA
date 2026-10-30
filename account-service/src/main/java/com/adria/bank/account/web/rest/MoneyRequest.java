package com.adria.bank.account.web.rest;

import java.math.BigDecimal;

public record MoneyRequest(BigDecimal amount) {
}
