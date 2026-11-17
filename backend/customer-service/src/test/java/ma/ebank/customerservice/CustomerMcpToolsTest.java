package ma.ebank.customerservice;

import ma.ebank.customerservice.entity.Customer;
import ma.ebank.customerservice.mcp.CustomerMcpTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "eureka.client.enabled=false")
class CustomerMcpToolsTest {

    @Autowired
    CustomerMcpTools tools;

    @Test
    void toolsDelegateToServiceLayer() {
        assertTrue(tools.listCustomers().size() >= 3);
        assertEquals("Mohammed", tools.findCustomerById(1L).getName());
        Customer saved = tools.saveCustomer("Laila", "laila@gmail.com");
        assertNotNull(saved.getId());
        assertThrows(ResponseStatusException.class, () -> tools.findCustomerById(9999L));
    }
}
