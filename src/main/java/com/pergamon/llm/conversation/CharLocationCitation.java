package com.pergamon.llm.conversation;

import java.util.Optional;

/**
 * Citation that references a character range in a plain text document.
 */
public record CharLocationCitation(
    String citedText,
    String title,
    String type,
    Optional<Long> documentIndex,
    Optional<String> documentTitle,
    Optional<String> fileId,
    Optional<Long> startCharIndex,
    Optional<Long> endCharIndex
) implements TextCitation {

    public CharLocationCitation {
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
        documentIndex = documentIndex == null ? Optional.empty() : documentIndex;
        documentTitle = documentTitle == null ? Optional.empty() : documentTitle;
        fileId = fileId == null ? Optional.empty() : fileId;
        startCharIndex = startCharIndex == null ? Optional.empty() : startCharIndex;
        endCharIndex = endCharIndex == null ? Optional.empty() : endCharIndex;
    }
}
