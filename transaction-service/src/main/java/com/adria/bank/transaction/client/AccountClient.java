package com.adria.bank.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "account-service")
public interface AccountClient {

    @GetMapping("/accounts/{id}")
    AccountDto getAccount(@PathVariable Long id);

    @PostMapping("/accounts/{id}/debit")
    AccountDto debit(@PathVariable Long id, @RequestBody AccountDebitRequest request);

    @PostMapping("/accounts/{id}/credit")
    AccountDto credit(@PathVariable Long id, @RequestBody AccountDebitRequest request);

    record AccountDebitRequest(BigDecimal amount) {
    }
}
