package com.pergamon.llm.conversation;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Utility methods for conversation processing, including text format detection.
 */
public class ConversationUtils {

    private static final Parser MARKDOWN_PARSER = Parser.builder().build();

    /**
     * Detects the format of a text string by checking for HTML, Markdown, or plain text.
     * HTML is checked first since it has a more specific signature (angle brackets with tags).
     *
     * @param text the text to analyze
     * @return the detected TextBlockFormat
     */
    public static TextBlockFormat detectTextFormat(String text) {
        if (text == null || text.isBlank()) {
            return TextBlockFormat.PLAIN;
        }

        // Check for HTML first (has most specific signature)
        if (isWellFormedHtml(text)) {
            return TextBlockFormat.HTML;
        }

        // Check for Markdown (most common for LLM responses)
        if (isMarkdown(text)) {
            return TextBlockFormat.MARKDOWN;
        }

        // Default to plain text
        return TextBlockFormat.PLAIN;
    }

    /**
     * Assesses if a text string is likely Markdown by checking the structure
     * of the generated Abstract Syntax Tree (AST).
     * Note: This assumes HTML has already been ruled out by isWellFormedHtml().
     *
     * @param text the text to check
     * @return true if the text is likely Markdown
     */
    public static boolean isMarkdown(String text) {
        // 1. Parse the text into the AST root node
        Node document = MARKDOWN_PARSER.parse(text);

        // 2. Get the first (and potentially only) top-level element
        Node firstChild = document.getFirstChild();

        // --- Foolproof Checks ---

        // A. Is there more than one top-level element? (e.g., a Heading AND a Paragraph)
        // If the first child is NOT the last child, it's structured Markdown.
        // However, we need to check if these are just plain paragraphs (multiline plain text)
        if (document.getLastChild() != firstChild) {
            // Check if all children are just plain paragraphs with no formatting
            Node current = firstChild;
            while (current != null) {
                // If any child is NOT a paragraph, it's markdown
                if (!(current instanceof Paragraph)) {
                    return true;
                }
                // If the paragraph has inline markdown formatting, it's markdown
                if (hasMarkdownFormatting((Paragraph) current)) {
                    return true;
                }
                current = current.getNext();
            }
            // All children were plain paragraphs, so it's just multi-line plain text
            return false;
        }

        // B. If there's only one top-level element, is it NOT a simple Paragraph?
        // (e.g., it's a List, BlockQuote, CodeBlock, FencedCodeBlock, or Heading)
        if (firstChild != null && !(firstChild instanceof Paragraph)) {
            return true;
        }

        // C. If it IS a single Paragraph, does it contain any *inline* formatting?
        // Inline formatting (bold, italic, links) causes the Paragraph node to have
        // multiple child nodes (e.g., plain text node + strong node + plain text node).
        // However, soft line breaks (from newlines) also create multiple children.
        // We need to distinguish between formatting and just newlines.
        if (firstChild != null && firstChild instanceof Paragraph) {
            if (hasMarkdownFormatting((Paragraph) firstChild)) {
                return true;
            }
        }

        // If it passes all checks, it's just a single, unformatted paragraph (plain text).
        return false;
    }

    /**
     * Checks if a paragraph contains markdown formatting (like bold, italic, links, code)
     * or if it's just plain text with soft line breaks.
     *
     * @param paragraph the paragraph to check
     * @return true if it has markdown formatting, false if it's just plain text
     */
    private static boolean hasMarkdownFormatting(Paragraph paragraph) {
        Node child = paragraph.getFirstChild();
        while (child != null) {
            // If it's NOT a Text or SoftLineBreak, then it's markdown formatting
            if (!(child instanceof Text) && !(child instanceof SoftLineBreak)) {
                return true;
            }
            child = child.getNext();
        }
        return false;
    }

    /**
     * Determines if the text is well-formed HTML by checking if Jsoup
     * successfully parses the input and finds a known tag.
     *
     * @param text the string to check
     * @return true if the text is successfully parsed and contains at least
     *         one structural element recognized by Jsoup
     */
    public static boolean isWellFormedHtml(String text) {
        // 1. Quick pre-check for the presence of angle brackets.
        if (text == null || text.isBlank() || !text.contains("<")) {
            return false;
        }

        // 2. Check if the text contains an HTML tag pattern (opening or closing tag)
        // This regex matches: < followed by optional /, then a letter, then word chars, then >
        // Examples: <div>, </div>, <p>, <b>, <h1>, etc.
        if (text.matches("(?s).*</?[a-zA-Z][a-zA-Z0-9]*[^>]*>.*")) {
            // The text contains what looks like an HTML tag, now validate with Jsoup
            Document doc = Jsoup.parseBodyFragment(text);
            Element body = doc.body();

            // If Jsoup successfully parsed it and created structure, it's HTML
            // (even if it's just a single element)
            return body.childrenSize() > 0;
        }

        // If no HTML tag pattern found, it's not HTML (e.g., "2 < 3" contains < but no tags)
        return false;
    }
}
