package ma.ebank.ebankbot.chat;

import java.util.ArrayList;
import java.util.List;

/** Splits long answers into chunks that fit a chat platform's message size limit. */
public final class MessageSplitter {

    private MessageSplitter() {
    }

    /** Splits on line breaks (then spaces) when possible; hard-cuts only as a last resort. */
    public static List<String> split(String text, int limit) {
        List<String> parts = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return parts;
        }
        String rest = text.strip();
        while (rest.length() > limit) {
            int cut = rest.lastIndexOf('\n', limit);
            if (cut < limit / 2) {
                cut = rest.lastIndexOf(' ', limit);
            }
            if (cut < limit / 2) {
                cut = limit;
            }
            parts.add(rest.substring(0, cut).strip());
            rest = rest.substring(cut).strip();
        }
        if (!rest.isEmpty()) {
            parts.add(rest);
        }
        return parts;
    }
}
