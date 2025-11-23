package com.pergamon.llm.conversation;

/**
 * A document block that contains base64-encoded PDF data.
 *
 * @param base64Data the base64-encoded PDF data
 * @param mimeType the MIME type of the document (must be "application/pdf")
 */
public record Base64PDFDocumentBlock(String base64Data, String mimeType) implements DocumentBlock {
}
