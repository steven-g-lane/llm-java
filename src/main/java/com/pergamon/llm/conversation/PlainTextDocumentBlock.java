package com.pergamon.llm.conversation;

/**
 * A document block that contains plain text content.
 *
 * @param text the plain text content
 * @param mimeType the MIME type of the document (must be "text/plain")
 */
public record PlainTextDocumentBlock(String text, String mimeType) implements DocumentBlock {
}
