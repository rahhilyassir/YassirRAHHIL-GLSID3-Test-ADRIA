package com.adria.bank.account.web.rest;

import com.adria.bank.account.domain.Account;
import com.adria.bank.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account create(@RequestBody Account account) {
        return accountService.create(account);
    }

    @GetMapping
    public List<Account> findAll() {
        return accountService.findAll();
    }

    @GetMapping("/{id}")
    public Account findById(@PathVariable Long id) {
        return accountService.findById(id);
    }

    @PatchMapping("/{id}/status")
    public Account updateStatus(@PathVariable Long id, @RequestBody AccountStatusUpdateRequest request) {
        return accountService.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/debit")
    public Account debit(@PathVariable Long id, @RequestBody MoneyRequest request) {
        return accountService.debit(id, request.amount());
    }

    @PostMapping("/{id}/credit")
    public Account credit(@PathVariable Long id, @RequestBody MoneyRequest request) {
        return accountService.credit(id, request.amount());
    }
}
