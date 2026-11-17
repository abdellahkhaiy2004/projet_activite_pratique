package ma.ebank.ebankbot.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageSplitterTest {

    @Test
    void shortTextIsOnePart() {
        assertEquals(List.of("hello"), MessageSplitter.split("hello", 10));
    }

    @Test
    void longTextIsSplitWithinLimitWithoutLosingWords() {
        String text = "word ".repeat(100).strip();
        List<String> parts = MessageSplitter.split(text, 50);
        assertTrue(parts.size() > 1);
        assertTrue(parts.stream().allMatch(p -> p.length() <= 50));
        assertEquals(text, String.join(" ", parts));
    }

    @Test
    void unbrokenTextIsHardCut() {
        List<String> parts = MessageSplitter.split("a".repeat(25), 10);
        assertEquals(List.of("a".repeat(10), "a".repeat(10), "a".repeat(5)), parts);
    }

    @Test
    void blankTextGivesNoParts() {
        assertTrue(MessageSplitter.split("  ", 10).isEmpty());
    }
}
