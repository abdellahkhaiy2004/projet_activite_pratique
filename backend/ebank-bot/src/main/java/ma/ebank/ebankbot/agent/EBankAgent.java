package ma.ebank.ebankbot.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * The single AI agent of the application. REST, Telegram and Discord all call this class,
 * so the prompt, memory and tools live in one place.
 */
@Slf4j
@Component
public class EBankAgent {

    public static final String DEFAULT_CONVERSATION_ID = "default";

    static final String SYSTEM_PROMPT = """
            You are the assistant of the eBank bank. You only help with two topics:
            bank customers and bank accounts.
            If the user asks about anything else, answer briefly that you do not know and that
            the question is outside the scope of the eBank assistant.
            Always use the available tools to read or create customers and accounts.
            Never invent customers, accounts or balances: only report data returned by the tools.
            Create a customer or an account only when the user clearly asks for it and gave all
            the required information; otherwise ask for the missing information.
            Answer in the language used by the user and keep answers short and clear.
            """;

    private final ChatClient chatClient;

    /**
     * @param toolCallbackProvider tools discovered at startup from the customer and account MCP servers
     */
    public EBankAgent(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory,
                      ToolCallbackProvider toolCallbackProvider) {
        ToolCallback[] tools = toolCallbackProvider.getToolCallbacks();
        for (ToolCallback tool : tools) {
            log.info("MCP tool available to the agent: {}", tool.getToolDefinition().name());
        }
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(tools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /** Answers in the default (shared) conversation. */
    public String askAgent(String query) {
        return askAgent(DEFAULT_CONVERSATION_ID, query);
    }

    /** Answers inside one conversation; each conversation id has its own memory. */
    public String askAgent(String conversationId, String query) {
        return chatClient.prompt()
                .user(query)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
