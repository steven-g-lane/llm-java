package com.pergamon.llm.conversation;

/**
 * Sealed interface representing an image in a message.
 * Images can be provided as a URL, base64-encoded data, or a file path.
 */
public sealed interface ImageBlock extends MessageBlock
    permits URLImageBlock, Base64ImageBlock, FilePathImageBlock {

    /**
     * Returns the MIME type of the image (e.g., "image/png", "image/jpeg").
     */
    String mimeType();
}
