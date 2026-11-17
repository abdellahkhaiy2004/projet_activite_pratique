package ma.ebank.ebankservice.controller;

import lombok.RequiredArgsConstructor;
import ma.ebank.ebankservice.entity.BankAccount;
import ma.ebank.ebankservice.service.BankAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping("/accounts")
    public List<BankAccount> accounts() {
        return bankAccountService.findAll();
    }

    @GetMapping("/accounts/{id}")
    public BankAccount accountById(@PathVariable String id) {
        return bankAccountService.findById(id);
    }

    @GetMapping("/accounts/customer/{customerId}")
    public List<BankAccount> accountsByCustomer(@PathVariable Long customerId) {
        return bankAccountService.findByCustomerId(customerId);
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccount save(@RequestBody BankAccount account) {
        return bankAccountService.save(account);
    }
}
