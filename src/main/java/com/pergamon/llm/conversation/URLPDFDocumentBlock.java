package com.pergamon.llm.conversation;

import java.net.URI;
import java.util.List;

/**
 * A document block that references a PDF by URL.
 *
 * @param uri the URI of the PDF document
 * @param mimeType the MIME type of the document (must be "application/pdf")
 * @param citations list of citations for this document (empty list if none)
 */
public record URLPDFDocumentBlock(URI uri, String mimeType, List<TextCitation> citations) implements DocumentBlock {

    public URLPDFDocumentBlock {
        // Ensure citations list is immutable (convert null to empty list)
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
