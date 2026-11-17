package ma.ebank.ebankservice.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import ma.ebank.ebankservice.clients.CustomerRestClient;
import ma.ebank.ebankservice.entity.BankAccount;
import ma.ebank.ebankservice.model.Customer;
import ma.ebank.ebankservice.repository.BankAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final CustomerRestClient customerRestClient;

    public List<BankAccount> findAll() {
        return enrich(bankAccountRepository.findAll());
    }

    public BankAccount findById(String id) {
        BankAccount account = bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found with id " + id));
        account.setCustomer(customerRestClient.findCustomerById(account.getCustomerId()));
        return account;
    }

    public List<BankAccount> findByCustomerId(Long customerId) {
        return enrich(bankAccountRepository.findByCustomerId(customerId));
    }

    /** Id and creation date are generated here; the caller provides balance, type and customerId. */
    public BankAccount save(BankAccount request) {
        if (request.getCustomerId() == null || request.getType() == null || request.getBalance() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "balance, type and customerId are required");
        }
        checkCustomerExists(request.getCustomerId());
        BankAccount account = BankAccount.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .balance(request.getBalance())
                .type(request.getType())
                .customerId(request.getCustomerId())
                .build();
        return bankAccountRepository.save(account);
    }

    /**
     * The customer must exist in customer-service before an account is created.
     * No fallback here: if the check cannot be completed, creation fails safely.
     */
    private void checkCustomerExists(Long customerId) {
        try {
            customerRestClient.findCustomerById(customerId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot create account: customer " + customerId + " does not exist");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Cannot create account: customer service is unavailable, customer could not be verified");
        }
    }

    /** Attaches customer details to each account, calling customer-service once per distinct customer. */
    private List<BankAccount> enrich(List<BankAccount> accounts) {
        Map<Long, Customer> customers = new HashMap<>();
        accounts.forEach(account -> account.setCustomer(
                customers.computeIfAbsent(account.getCustomerId(), customerRestClient::findCustomerById)));
        return accounts;
    }
}
