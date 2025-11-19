package com.pergamon.llm.conversation;

/**
 * An image block that contains base64-encoded image data.
 *
 * @param base64Data the base64-encoded image data
 * @param mimeType the MIME type of the image (e.g., "image/png", "image/jpeg")
 */
public record Base64ImageBlock(String base64Data, String mimeType) implements ImageBlock {
}
