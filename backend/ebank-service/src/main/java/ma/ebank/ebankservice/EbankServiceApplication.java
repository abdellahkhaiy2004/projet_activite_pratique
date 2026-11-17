package ma.ebank.ebankservice;

import ma.ebank.ebankservice.entity.BankAccount;
import ma.ebank.ebankservice.enums.AccountType;
import ma.ebank.ebankservice.repository.BankAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@SpringBootApplication
public class EbankServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EbankServiceApplication.class, args);
    }

    /** 3 customers (ids 1..3 in customer-service) x 5 accounts each. */
    @Bean
    CommandLineRunner seedAccounts(BankAccountRepository bankAccountRepository) {
        return args -> {
            Random random = new Random();
            for (long customerId = 1; customerId <= 3; customerId++) {
                for (int i = 0; i < 5; i++) {
                    bankAccountRepository.save(BankAccount.builder()
                            .id(UUID.randomUUID().toString())
                            .createdAt(LocalDateTime.now())
                            .balance(Math.round((1000 + random.nextDouble() * 9000) * 100) / 100.0)
                            .type(random.nextBoolean() ? AccountType.CURRENT_ACCOUNT : AccountType.SAVING_ACCOUNT)
                            .customerId(customerId)
                            .build());
                }
            }
        };
    }
}
