package ma.ebank.ebankbot;

import ma.ebank.ebankbot.agent.EBankAgent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.ai.openai.api-key=test-key"})
class EBankAgentTest {

    @Autowired
    EBankAgent agent;
    @MockitoBean
    ChatModel chatModel;

    @Test
    void usesSystemPromptAndRemembersConversation() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Hello Sara")))));

        assertEquals("Hello Sara", agent.askAgent("conv-1", "My name is Sara"));
        agent.askAgent("conv-1", "What is my name?");

        ArgumentCaptor<Prompt> prompts = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, atLeastOnce()).call(prompts.capture());
        Prompt last = prompts.getValue();

        List<Message> messages = last.getInstructions();
        assertTrue(messages.stream().anyMatch(m -> m.getMessageType() == MessageType.SYSTEM
                && m.getText().contains("bank customers and bank accounts")));
        // second prompt carries the first exchange from memory
        assertTrue(messages.stream().anyMatch(m -> m.getText().contains("My name is Sara")));
        assertTrue(messages.stream().anyMatch(m -> m.getMessageType() == MessageType.ASSISTANT));
    }

    @Test
    void conversationsAreIsolated() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));

        agent.askAgent("conv-a", "secret-from-a");
        agent.askAgent("conv-b", "hello from b");

        ArgumentCaptor<Prompt> prompts = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, atLeastOnce()).call(prompts.capture());
        Prompt last = prompts.getValue();
        assertTrue(last.getInstructions().stream().noneMatch(m -> m.getText().contains("secret-from-a")));
    }
}
