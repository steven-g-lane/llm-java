package com.pergamon.llm.conversation;

/**
 * Sealed interface representing an image in a message.
 * Images can be provided either as a URL or as base64-encoded data.
 */
public sealed interface ImageBlock extends MessageBlock
    permits URLImageBlock, Base64ImageBlock {

    /**
     * Returns the MIME type of the image (e.g., "image/png", "image/jpeg").
     */
    String mimeType();
}
