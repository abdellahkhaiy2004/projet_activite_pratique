package ma.ebank.ebankservice;

import ma.ebank.ebankservice.clients.CustomerRestClient;
import ma.ebank.ebankservice.entity.BankAccount;
import ma.ebank.ebankservice.enums.AccountType;
import ma.ebank.ebankservice.mcp.BankAccountMcpTools;
import ma.ebank.ebankservice.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "eureka.client.enabled=false")
class BankAccountMcpToolsTest {

    @Autowired
    BankAccountMcpTools tools;
    @MockitoBean
    CustomerRestClient customerRestClient;

    @BeforeEach
    void stubCustomerService() {
        when(customerRestClient.findCustomerById(anyLong()))
                .thenAnswer(inv -> new Customer(inv.getArgument(0), "Customer " + inv.getArgument(0), "c@mail.com"));
    }

    @Test
    void toolsDelegateToServiceLayer() {
        assertTrue(tools.listAccounts().size() >= 15);
        assertTrue(tools.findAccountsByCustomerId(1L).size() >= 5);

        BankAccount saved = tools.saveAccount(1234.5, AccountType.SAVING_ACCOUNT, 3L);
        assertEquals(3L, tools.findAccountById(saved.getId()).getCustomerId());
        assertEquals("Customer 3", tools.findAccountById(saved.getId()).getCustomer().getName());
        assertThrows(ResponseStatusException.class, () -> tools.findAccountById("nope"));
    }
}
