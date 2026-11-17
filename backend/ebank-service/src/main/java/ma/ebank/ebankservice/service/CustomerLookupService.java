package ma.ebank.ebankservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.ebank.ebankservice.clients.CustomerRestClient;
import ma.ebank.ebankservice.model.Customer;
import org.springframework.stereotype.Service;

/**
 * Customer lookup used only to DISPLAY customer details when reading accounts.
 * It is protected by a circuit breaker with a fallback. Account creation does NOT use it:
 * validation there must never be answered by a fallback (see BankAccountService.checkCustomerExists).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerLookupService {

    private final CustomerRestClient customerRestClient;

    @CircuitBreaker(name = "customerService", fallbackMethod = "fallbackCustomer")
    public Customer findCustomer(Long customerId) {
        return customerRestClient.findCustomerById(customerId);
    }

    Customer fallbackCustomer(Long customerId, Throwable cause) {
        log.warn("Customer lookup failed for id {} ({}), returning fallback", customerId, cause.toString());
        return new Customer(customerId, "Not available", "Not available");
    }
}
