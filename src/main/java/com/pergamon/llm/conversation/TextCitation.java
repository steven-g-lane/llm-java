package com.pergamon.llm.conversation;

/**
 * Sealed interface representing a citation in a text or document block.
 * Citations provide source attribution when Claude references documents or search results.
 */
public sealed interface TextCitation
    permits CharLocationCitation, PageLocationCitation, ContentBlockLocationCitation,
            WebSearchResultCitation, SearchResultCitation, UnknownCitation {

    /**
     * Returns the cited text excerpt.
     */
    String citedText();

    /**
     * Returns the title of the citation source.
     */
    String title();
}
