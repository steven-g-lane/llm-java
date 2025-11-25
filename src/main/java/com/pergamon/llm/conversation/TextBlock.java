package com.pergamon.llm.conversation;

import java.util.List;

public record TextBlock(TextBlockFormat format, String text, List<TextCitation> citations) implements MessageBlock {

    public TextBlock {
        // Ensure citations list is immutable (convert null to empty list)
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
