package com.adria.bank.account.web.graphql;

import com.adria.bank.account.domain.AccountStatus;

public record AccountGraphql(
        Long id,
        String customerId,
        String currency,
        Double balance,
        AccountStatus status
) {
}
