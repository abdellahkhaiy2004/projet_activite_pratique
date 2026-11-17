package ma.ebank.ebankservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import feign.Request;
import ma.ebank.ebankservice.clients.CustomerRestClient;
import ma.ebank.ebankservice.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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
    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;
    @MockitoBean
    CustomerRestClient customerRestClient;

    @BeforeEach
    void stubCustomerService() {
        circuitBreakerRegistry.circuitBreaker("customerService").reset();
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

    @Test
    void rejectsAccountForUnknownCustomer() throws Exception {
        Request request = Request.create(Request.HttpMethod.GET, "/customers/999",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        when(customerRestClient.findCustomerById(999L))
                .thenThrow(new FeignException.NotFound("not found", request, null, Collections.emptyMap()));
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":100,\"type\":\"CURRENT_ACCOUNT\",\"customerId\":999}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/accounts/customer/999"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void failsSafelyWhenCustomerServiceIsDown() throws Exception {
        when(customerRestClient.findCustomerById(2L)).thenThrow(new RuntimeException("connection refused"));
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":100,\"type\":\"CURRENT_ACCOUNT\",\"customerId\":2}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void returnsFallbackCustomerWhenCustomerServiceIsDown() throws Exception {
        when(customerRestClient.findCustomerById(anyLong())).thenThrow(new RuntimeException("connection refused"));
        mockMvc.perform(get("/accounts/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].customerId").value(1))
                .andExpect(jsonPath("$[0].customer.name").value("Not available"));
    }

    @Test
    void circuitOpensAfterRepeatedFailuresAndStillAnswers() throws Exception {
        when(customerRestClient.findCustomerById(anyLong())).thenThrow(new RuntimeException("connection refused"));
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(get("/accounts/customer/1")).andExpect(status().isOk());
        }
        org.junit.jupiter.api.Assertions.assertEquals(CircuitBreaker.State.OPEN,
                circuitBreakerRegistry.circuitBreaker("customerService").getState());
        mockMvc.perform(get("/accounts/customer/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customer.name").value("Not available"));
    }
}
