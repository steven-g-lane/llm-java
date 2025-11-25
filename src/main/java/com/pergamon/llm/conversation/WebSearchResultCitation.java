package com.pergamon.llm.conversation;

import java.util.Optional;

/**
 * Citation that references a web search result.
 */
public record WebSearchResultCitation(
    String citedText,
    String title,
    String type,
    Optional<String> url,
    Optional<String> encryptedIndex
) implements TextCitation {

    public WebSearchResultCitation {
        if (citedText == null || citedText.isBlank()) {
            throw new IllegalArgumentException("citedText cannot be null or blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title cannot be null or blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type cannot be null or blank");
        }

        // Ensure Optional fields are never null
        url = url == null ? Optional.empty() : url;
        encryptedIndex = encryptedIndex == null ? Optional.empty() : encryptedIndex;
    }
}
