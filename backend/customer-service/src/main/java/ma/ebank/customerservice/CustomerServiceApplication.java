package ma.ebank.customerservice;

import ma.ebank.customerservice.entity.Customer;
import ma.ebank.customerservice.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner seedCustomers(CustomerRepository customerRepository) {
        return args -> customerRepository.saveAll(List.of(
                Customer.builder().name("Mohammed").email("mohammed@gmail.com").build(),
                Customer.builder().name("Iman").email("iman@gmail.com").build(),
                Customer.builder().name("Yassine").email("yassine@gmail.com").build()
        ));
    }
}
