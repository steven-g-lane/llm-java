package com.pergamon.llm.conversation;

import java.util.Optional;

/**
 * Citation that references a generic search result.
 */
public record SearchResultCitation(
    String citedText,
    String title,
    String type,
    Optional<String> source,
    Optional<Long> startBlockIndex,
    Optional<Long> endBlockIndex,
    Optional<Long> searchResultIndex
) implements TextCitation {

    public SearchResultCitation {
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
        source = source == null ? Optional.empty() : source;
        startBlockIndex = startBlockIndex == null ? Optional.empty() : startBlockIndex;
        endBlockIndex = endBlockIndex == null ? Optional.empty() : endBlockIndex;
        searchResultIndex = searchResultIndex == null ? Optional.empty() : searchResultIndex;
    }
}
