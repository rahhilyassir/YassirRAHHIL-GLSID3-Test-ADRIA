package com.adria.bank.account.mcp;

import com.adria.bank.account.domain.Account;
import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountMcpToolService {

    private final AccountService accountService;

    @Tool(description = "List all bank accounts currently registered in the system.")
    public List<Account> listAccounts() {
        return accountService.findAll();
    }

    @Tool(description = "Fetch a single bank account by its unique identifier.")
    public Account getAccount(@ToolParam(description = "The unique identifier of the account to retrieve.") Long id) {
        return accountService.findById(id);
    }

    @Tool(description = "Create a new bank account with a customer identifier, currency, and starting balance.")
    public Account createAccount(
            @ToolParam(description = "The unique customer identifier for the new account.") String customerId,
            @ToolParam(description = "The ISO currency code for the account, such as EUR or USD.") String currency,
            @ToolParam(description = "The opening balance to credit to the new account.") BigDecimal balance) {
        Account account = new Account();
        account.setCustomerId(customerId);
        account.setCurrency(currency);
        account.setBalance(balance == null ? BigDecimal.ZERO : balance);
        account.setStatus(AccountStatus.ACTIVE);
        return accountService.create(account);
    }

    @Tool(description = "Update the status of an existing bank account.")
    public Account updateAccountStatus(
            @ToolParam(description = "The account identifier of the account whose status should be changed.") Long id,
            @ToolParam(description = "The new account status, for example ACTIVE or BLOCKED.") AccountStatus status) {
        return accountService.updateStatus(id, status);
    }
}
