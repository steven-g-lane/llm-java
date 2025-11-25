package com.pergamon.llm.conversation;

import java.util.Optional;

/**
 * Citation that references a page range in a PDF document.
 */
public record PageLocationCitation(
    String citedText,
    String title,
    String type,
    Optional<Long> documentIndex,
    Optional<String> documentTitle,
    Optional<String> fileId,
    Optional<Long> startPageNumber,
    Optional<Long> endPageNumber
) implements TextCitation {

    public PageLocationCitation {
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
        startPageNumber = startPageNumber == null ? Optional.empty() : startPageNumber;
        endPageNumber = endPageNumber == null ? Optional.empty() : endPageNumber;
    }
}
