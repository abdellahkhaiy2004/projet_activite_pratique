package ma.ebank.ebankservice.mcp;

import lombok.RequiredArgsConstructor;
import ma.ebank.ebankservice.entity.BankAccount;
import ma.ebank.ebankservice.enums.AccountType;
import ma.ebank.ebankservice.service.BankAccountService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/** Exposes the bank-account service-layer operations as MCP tools (REST controllers stay unchanged). */
@Component
@RequiredArgsConstructor
public class BankAccountMcpTools {

    private final BankAccountService bankAccountService;

    @McpTool(name = "list_accounts",
            description = "List all bank accounts (id, creation date, balance, type, customerId and owner details).")
    public List<BankAccount> listAccounts() {
        return bankAccountService.findAll();
    }

    @McpTool(name = "find_account_by_id",
            description = "Find one bank account by its id (a UUID string). Fails if the account does not exist.")
    public BankAccount findAccountById(
            @McpToolParam(description = "UUID of the bank account", required = true) String id) {
        return bankAccountService.findById(id);
    }

    @McpTool(name = "find_accounts_by_customer_id",
            description = "List all bank accounts that belong to one customer, given the customer's numeric id.")
    public List<BankAccount> findAccountsByCustomerId(
            @McpToolParam(description = "Numeric id of the customer", required = true) Long customerId) {
        return bankAccountService.findByCustomerId(customerId);
    }

    @McpTool(name = "save_account",
            description = "Create a new bank account for an existing customer. The id and creation date are generated. "
                    + "Fails if the customer does not exist or cannot be verified.")
    public BankAccount saveAccount(
            @McpToolParam(description = "Initial balance of the account", required = true) Double balance,
            @McpToolParam(description = "Account type: CURRENT_ACCOUNT or SAVING_ACCOUNT", required = true) AccountType type,
            @McpToolParam(description = "Numeric id of the existing customer who owns the account", required = true) Long customerId) {
        return bankAccountService.save(BankAccount.builder()
                .balance(balance).type(type).customerId(customerId).build());
    }
}
