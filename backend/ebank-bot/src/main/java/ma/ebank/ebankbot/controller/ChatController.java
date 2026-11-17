package ma.ebank.ebankbot.controller;

import lombok.RequiredArgsConstructor;
import ma.ebank.ebankbot.agent.EBankAgent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/** Thin REST layer: all the logic lives in {@link EBankAgent}. */
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final EBankAgent agent;

    /** Normal chat: the full answer as plain text. */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public String chat(@RequestParam String query) {
        return agent.askAgent(query);
    }

    /**
     * Streamed chat as Server-Sent Events: each event's data is the next piece of the answer
     * (Spring writes "data:" immediately followed by the chunk, so spaces are preserved).
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String query) {
        return agent.streamAgent(EBankAgent.DEFAULT_CONVERSATION_ID, query);
    }
}
