package com.pergamon.llm.conversation;

import java.util.List;

/**
 * A document block that contains base64-encoded PDF data.
 *
 * @param base64Data the base64-encoded PDF data
 * @param mimeType the MIME type of the document (must be "application/pdf")
 * @param citations list of citations for this document (empty list if none)
 */
public record Base64PDFDocumentBlock(String base64Data, String mimeType, List<TextCitation> citations) implements DocumentBlock {

    public Base64PDFDocumentBlock {
        // Ensure citations list is immutable (convert null to empty list)
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
