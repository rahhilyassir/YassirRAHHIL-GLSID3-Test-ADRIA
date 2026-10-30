package com.adria.bank.account.web.graphql;

public record AccountInput(String customerId, String currency, Double balance) {
}
