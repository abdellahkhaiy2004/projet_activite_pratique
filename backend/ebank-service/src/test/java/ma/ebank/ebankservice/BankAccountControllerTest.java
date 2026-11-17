package ma.ebank.ebankservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.ebank.ebankservice.clients.CustomerRestClient;
import ma.ebank.ebankservice.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "eureka.client.enabled=false")
@AutoConfigureMockMvc
class BankAccountControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    CustomerRestClient customerRestClient;

    @BeforeEach
    void stubCustomerService() {
        when(customerRestClient.findCustomerById(anyLong()))
                .thenAnswer(inv -> new Customer(inv.getArgument(0), "Customer " + inv.getArgument(0), "c@mail.com"));
    }

    @Test
    void accountIsEnrichedWithCustomerFromFeignClient() throws Exception {
        String id = objectMapper.readTree(mockMvc.perform(get("/accounts/customer/2"))
                .andReturn().getResponse().getContentAsString()).get(0).get("id").asText();
        mockMvc.perform(get("/accounts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.id").value(2))
                .andExpect(jsonPath("$.customer.name").value("Customer 2"));
        verify(customerRestClient, atLeastOnce()).findCustomerById(2L);
    }

    @Test
    void listsSeededAccounts() throws Exception {
        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(15))));
    }

    @Test
    void listsFiveAccountsPerSeededCustomer() throws Exception {
        mockMvc.perform(get("/accounts/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void createsThenRetrievesAccount() throws Exception {
        String body = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":2500.5,\"type\":\"SAVING_ACCOUNT\",\"customerId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(body);

        mockMvc.perform(get("/accounts/" + created.get("id").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(2))
                .andExpect(jsonPath("$.type").value("SAVING_ACCOUNT"));
    }

    @Test
    void returns404WhenAccountMissing() throws Exception {
        mockMvc.perform(get("/accounts/unknown-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsIncompleteAccount() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":10}"))
                .andExpect(status().isBadRequest());
    }
}
