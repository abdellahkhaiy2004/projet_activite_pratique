package ma.ebank.ebankservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.ebank.ebankservice.enums.AccountType;
import ma.ebank.ebankservice.model.Customer;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {
    @Id
    private String id;
    private LocalDateTime createdAt;
    private Double balance;
    @Enumerated(EnumType.STRING)
    private AccountType type;
    /** ID of the owner in customer-service (no cross-service JPA relation). */
    private Long customerId;
    /** Filled at read time for display only; not persisted. */
    @Transient
    private Customer customer;
}
