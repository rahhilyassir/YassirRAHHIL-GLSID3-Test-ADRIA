package com.adria.bank.account.web.rest;

import com.adria.bank.account.domain.AccountStatus;

public record AccountStatusUpdateRequest(AccountStatus status) {
}
