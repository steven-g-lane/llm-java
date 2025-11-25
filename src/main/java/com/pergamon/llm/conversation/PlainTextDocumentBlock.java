package com.pergamon.llm.conversation;

import java.util.List;

/**
 * A document block that contains plain text content.
 *
 * @param text the plain text content
 * @param mimeType the MIME type of the document (must be "text/plain")
 * @param citations list of citations for this document (empty list if none)
 */
public record PlainTextDocumentBlock(String text, String mimeType, List<TextCitation> citations) implements DocumentBlock {

    public PlainTextDocumentBlock {
        // Ensure citations list is immutable (convert null to empty list)
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
