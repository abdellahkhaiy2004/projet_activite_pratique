package ma.ebank.ebankbot;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.ai.openai.api-key=test-key",
        "spring.ai.mcp.client.enabled=false"})
@AutoConfigureMockMvc
class ChatControllerTest {

    @TestConfiguration
    static class NoToolsConfig {
        @Bean
        ToolCallbackProvider testToolCallbackProvider() {
            return () -> new ToolCallback[0];
        }
    }

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    ChatModel chatModel;

    private static ChatResponse reply(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    void chatReturnsPlainTextAnswer() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenReturn(reply("You have 3 customers."));
        mockMvc.perform(get("/chat").param("query", "how many customers?"))
                .andExpect(status().isOk())
                .andExpect(content().string("You have 3 customers."));
    }

    @Test
    void chatRequiresQuery() throws Exception {
        mockMvc.perform(get("/chat")).andExpect(status().isBadRequest());
    }

    @Test
    void streamSendsAnswerInSeveralChunks() throws Exception {
        when(chatModel.stream(any(Prompt.class)))
                .thenReturn(Flux.just(reply("Hello"), reply(" from"), reply(" eBank")));
        MvcResult started = mockMvc.perform(get("/chat/stream").param("query", "hi")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("data:Hello"),
                        org.hamcrest.Matchers.containsString("data: from"),
                        org.hamcrest.Matchers.containsString("data: eBank"))));
    }
}
