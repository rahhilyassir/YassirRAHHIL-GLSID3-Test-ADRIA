package com.adria.bank.account.web.graphql;

import com.adria.bank.account.domain.Account;
import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AccountGraphqlController {

    private final AccountService accountService;

    @QueryMapping
    public List<AccountGraphql> accounts() {
        return accountService.findAll().stream()
                .map(this::toGraphql)
                .toList();
    }

    @QueryMapping
    public AccountGraphql account(@Argument("id") Long id) {
        return toGraphql(accountService.findById(id));
    }

    @MutationMapping
    public AccountGraphql createAccount(@Argument("accountInput") AccountInput input) {
        Account account = new Account();
        account.setCustomerId(input.customerId());
        account.setCurrency(input.currency());
        account.setBalance(BigDecimal.valueOf(input.balance() == null ? 0d : input.balance()));
        return toGraphql(accountService.create(account));
    }

    @MutationMapping
    public AccountGraphql updateAccountStatus(@Argument("id") Long id, @Argument("status") AccountStatus status) {
        return toGraphql(accountService.updateStatus(id, status));
    }

    private AccountGraphql toGraphql(Account account) {
        return new AccountGraphql(
                account.getId(),
                account.getCustomerId(),
                account.getCurrency(),
                account.getBalance().doubleValue(),
                account.getStatus()
        );
    }
}
