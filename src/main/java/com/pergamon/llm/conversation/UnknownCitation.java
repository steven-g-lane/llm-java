package com.pergamon.llm.conversation;

/**
 * Citation type for handling unknown or future citation types from the Anthropic API.
 * This provides forward compatibility when new citation types are introduced.
 */
public record UnknownCitation(
    String citedText,
    String title,
    String type,
    String rawData
) implements TextCitation {

    public UnknownCitation {
        if (citedText == null || citedText.isBlank()) {
            throw new IllegalArgumentException("citedText cannot be null or blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title cannot be null or blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type cannot be null or blank");
        }
        if (rawData == null || rawData.isBlank()) {
            throw new IllegalArgumentException("rawData cannot be null or blank");
        }
    }
}
