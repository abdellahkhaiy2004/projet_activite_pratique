package ma.ebank.ebankservice.service;

import lombok.RequiredArgsConstructor;
import ma.ebank.ebankservice.entity.BankAccount;
import ma.ebank.ebankservice.repository.BankAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    public List<BankAccount> findAll() {
        return bankAccountRepository.findAll();
    }

    public BankAccount findById(String id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found with id " + id));
    }

    public List<BankAccount> findByCustomerId(Long customerId) {
        return bankAccountRepository.findByCustomerId(customerId);
    }

    /** Id and creation date are generated here; the caller provides balance, type and customerId. */
    public BankAccount save(BankAccount request) {
        if (request.getCustomerId() == null || request.getType() == null || request.getBalance() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "balance, type and customerId are required");
        }
        BankAccount account = BankAccount.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .balance(request.getBalance())
                .type(request.getType())
                .customerId(request.getCustomerId())
                .build();
        return bankAccountRepository.save(account);
    }
}
