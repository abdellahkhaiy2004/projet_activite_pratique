package ma.ebank.customerservice.service;

import lombok.RequiredArgsConstructor;
import ma.ebank.customerservice.entity.Customer;
import ma.ebank.customerservice.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Customer not found with id " + id));
    }

    public Customer save(Customer customer) {
        customer.setId(null); // always create a new customer
        return customerRepository.save(customer);
    }
}
