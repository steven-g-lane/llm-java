package com.pergamon.llm.conversation;

import java.net.URI;

/**
 * A document block that references a PDF by URL.
 *
 * @param uri the URI of the PDF document
 * @param mimeType the MIME type of the document (must be "application/pdf")
 */
public record URLPDFDocumentBlock(URI uri, String mimeType) implements DocumentBlock {
}
