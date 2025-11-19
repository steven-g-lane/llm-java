package com.pergamon.llm.conversation;

import java.net.URI;

/**
 * An image block that references an image by URL.
 *
 * @param uri the URI of the image
 * @param mimeType the MIME type of the image (e.g., "image/png", "image/jpeg")
 */
public record URLImageBlock(URI uri, String mimeType) implements ImageBlock {
}
