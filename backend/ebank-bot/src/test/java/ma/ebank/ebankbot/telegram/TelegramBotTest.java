package ma.ebank.ebankbot.telegram;

import ma.ebank.ebankbot.agent.EBankAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramBotTest {

    TelegramClient client = mock(TelegramClient.class);
    EBankAgent agent = mock(EBankAgent.class);
    TelegramBot bot;

    @BeforeEach
    void setUp() {
        bot = new TelegramBot(client, agent);
    }

    @AfterEach
    void tearDown() {
        bot.shutdown();
    }

    private static Update textUpdate(long chatId, String text) {
        org.telegram.telegrambots.meta.api.objects.message.Message message =
                new org.telegram.telegrambots.meta.api.objects.message.Message();
        message.setChat(Chat.builder().id(chatId).type("private").build());
        message.setText(text);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    @Test
    void forwardsTextToAgentAndRepliesInSameChat() throws Exception {
        when(agent.askAgent("telegram-42", "list customers")).thenReturn("Mohammed, Iman, Yassine");

        bot.consume(textUpdate(42L, "list customers"));

        ArgumentCaptor<SendMessage> sent = ArgumentCaptor.forClass(SendMessage.class);
        verify(client, timeout(2000)).execute(sent.capture());
        assertEquals("42", sent.getValue().getChatId());
        assertEquals("Mohammed, Iman, Yassine", sent.getValue().getText());
        verify(client, timeout(2000).atLeastOnce()).execute(org.mockito.ArgumentMatchers.any(SendChatAction.class));
    }

    @Test
    void answersUnsupportedMessagesWithoutCallingAgent() throws Exception {
        org.telegram.telegrambots.meta.api.objects.message.Message message =
                new org.telegram.telegrambots.meta.api.objects.message.Message();
        message.setChat(Chat.builder().id(7L).type("private").build());
        Update update = new Update();
        update.setMessage(message); // no text (e.g. a photo)

        bot.consume(update);

        verify(agent, never()).askAgent(anyString(), anyString());
        ArgumentCaptor<SendMessage> sent = ArgumentCaptor.forClass(SendMessage.class);
        verify(client).execute(sent.capture());
        assertTrue(sent.getValue().getText().contains("text"));
    }

    @Test
    void repliesWithFriendlyErrorWhenAgentFails() throws Exception {
        when(agent.askAgent(anyString(), anyString())).thenThrow(new RuntimeException("boom"));

        bot.consume(textUpdate(1L, "hi"));

        ArgumentCaptor<SendMessage> sent = ArgumentCaptor.forClass(SendMessage.class);
        verify(client, timeout(2000)).execute(sent.capture());
        assertTrue(sent.getValue().getText().startsWith("Sorry"));
    }
}
