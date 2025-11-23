package com.pergamon.llm.conversation;

/**
 * A message block that represents an image loaded from a file path.
 * The file will be read and base64-encoded when converting to vendor format.
 *
 * @param filePath the path to the image file
 * @param mimeType the MIME type of the image (e.g., "image/png", "image/jpeg")
 */
public record FilePathImageBlock(String filePath, String mimeType) implements ImageBlock {
    public FilePathImageBlock {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("File path cannot be null or blank");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("MIME type cannot be null or blank");
        }
    }
}
