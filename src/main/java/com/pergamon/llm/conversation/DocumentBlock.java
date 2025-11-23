package com.pergamon.llm.conversation;

/**
 * Sealed interface representing a document in a message.
 * Documents can be provided as PDFs (URL, base64-encoded, or file path) or plain text.
 */
public sealed interface DocumentBlock extends MessageBlock
    permits Base64PDFDocumentBlock, URLPDFDocumentBlock, FilePathPDFDocumentBlock, PlainTextDocumentBlock {

    /**
     * Returns the MIME type of the document (e.g., "application/pdf", "text/plain").
     */
    String mimeType();
}
