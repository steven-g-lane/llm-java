package com.pergamon.llm.conversation;

import java.util.List;

/**
 * A message block that represents a PDF document loaded from a file path.
 * The file will be read and base64-encoded when converting to vendor format.
 *
 * @param filePath the path to the PDF file
 * @param mimeType the MIME type of the document (must be "application/pdf")
 * @param citations list of citations for this document (empty list if none)
 */
public record FilePathPDFDocumentBlock(String filePath, String mimeType, List<TextCitation> citations) implements DocumentBlock {
    public FilePathPDFDocumentBlock {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("File path cannot be null or blank");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("MIME type cannot be null or blank");
        }
        // Ensure citations list is immutable (convert null to empty list)
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
