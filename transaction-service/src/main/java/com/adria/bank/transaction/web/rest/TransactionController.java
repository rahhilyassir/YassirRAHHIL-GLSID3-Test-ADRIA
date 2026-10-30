package com.adria.bank.transaction.web.rest;

import com.adria.bank.transaction.domain.Transaction;
import com.adria.bank.transaction.service.CreateTransactionRequest;
import com.adria.bank.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction create(@RequestBody CreateTransactionRequestRest request) {
        return transactionService.create(new CreateTransactionRequest(request.accountId(), request.currency(), request.amount()));
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Transaction> transfer(@RequestBody TransferRequestRest request) {
        return transactionService.transfer(request.fromAccountId(), request.toAccountId(), request.amount(), request.currency());
    }

    @GetMapping
    public List<Transaction> findAll(@RequestParam(required = false) Long accountId) {
        if (accountId != null) {
            return transactionService.findByAccountId(accountId);
        }
        return transactionService.findAll();
    }

    @GetMapping("/{id}")
    public Transaction findById(@PathVariable Long id) {
        return transactionService.findById(id);
    }
}
