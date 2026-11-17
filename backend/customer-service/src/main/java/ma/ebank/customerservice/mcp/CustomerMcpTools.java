package ma.ebank.customerservice.mcp;

import lombok.RequiredArgsConstructor;
import ma.ebank.customerservice.entity.Customer;
import ma.ebank.customerservice.service.CustomerService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/** Exposes the customer service-layer operations as MCP tools (REST controllers stay unchanged). */
@Component
@RequiredArgsConstructor
public class CustomerMcpTools {

    private final CustomerService customerService;

    @McpTool(name = "list_customers",
            description = "List all bank customers (id, name, email).")
    public List<Customer> listCustomers() {
        return customerService.findAll();
    }

    @McpTool(name = "find_customer_by_id",
            description = "Find one bank customer by its numeric id. Fails if the customer does not exist.")
    public Customer findCustomerById(
            @McpToolParam(description = "Numeric id of the customer", required = true) Long id) {
        return customerService.findById(id);
    }

    @McpTool(name = "save_customer",
            description = "Create a new bank customer with a name and an email and return the saved customer with its generated id.")
    public Customer saveCustomer(
            @McpToolParam(description = "Full name of the customer", required = true) String name,
            @McpToolParam(description = "Email address of the customer", required = true) String email) {
        return customerService.save(Customer.builder().name(name).email(email).build());
    }
}
