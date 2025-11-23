package com.pergamon.llm.conversation;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

/**
 * Utility methods for conversation processing, including text format detection and validation.
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

    // ==================== Validation Methods ====================

    /**
     * Validates that a file path points to an existing readable file with a supported extension.
     *
     * @param filePath the file path to validate
     * @param supportedExtensions collection of supported file extensions (e.g., ".png", ".jpg")
     * @throws IllegalArgumentException if the file doesn't exist, is not readable, or has an unsupported extension
     */
    public static void validateFilePath(String filePath, Collection<String> supportedExtensions) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("File path cannot be null or blank");
        }

        Path path = Path.of(filePath);

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Path is not a regular file: " + filePath);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("File is not readable: " + filePath);
        }

        String fileName = path.getFileName().toString().toLowerCase();
        boolean hasSupportedExtension = supportedExtensions.stream()
                .anyMatch(ext -> fileName.endsWith(ext.toLowerCase()));

        if (!hasSupportedExtension) {
            throw new IllegalArgumentException(
                    "File does not have a supported extension " + supportedExtensions + ": " + filePath
            );
        }
    }

    /**
     * Validates that a URI is well-formed and uses http/https scheme.
     *
     * @param uri the URI to validate
     * @throws IllegalArgumentException if the URI is not well-formed
     */
    public static void validateUri(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("URI cannot be null");
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("URI must use http or https scheme: " + uri);
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("URI must have a valid host: " + uri);
        }
    }

    /**
     * Validates that a MIME type is in the collection of supported types.
     *
     * @param mimeType the MIME type to validate
     * @param supportedMimeTypes collection of supported MIME types (e.g., "image/png", "image/jpeg")
     * @throws IllegalArgumentException if the MIME type is not supported
     */
    public static void validateMimeType(String mimeType, Collection<String> supportedMimeTypes) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("MIME type cannot be null or blank");
        }

        boolean isSupported = supportedMimeTypes.stream()
                .anyMatch(supported -> supported.equalsIgnoreCase(mimeType));

        if (!isSupported) {
            throw new IllegalArgumentException(
                    "Unsupported MIME type: " + mimeType + ". Supported types: " + supportedMimeTypes
            );
        }
    }

    /**
     * Validates that base64-encoded data is not null or blank.
     *
     * @param base64Data the base64 data to validate
     * @throws IllegalArgumentException if the data is null or blank
     */
    public static void validateBase64Data(String base64Data) {
        if (base64Data == null || base64Data.isBlank()) {
            throw new IllegalArgumentException("Base64 data cannot be null or blank");
        }
    }

    /**
     * Validates that a PDF file path points to an existing readable file with .pdf extension.
     *
     * @param filePath the file path to validate
     * @throws IllegalArgumentException if the file doesn't exist, is not readable, or doesn't have .pdf extension
     */
    public static void validatePdfFilePath(String filePath) {
        validateFilePath(filePath, Set.of(".pdf"));
    }

    /**
     * Validates that plain text content is not null or blank.
     *
     * @param text the text to validate
     * @throws IllegalArgumentException if the text is null or blank
     */
    public static void validatePlainText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Plain text cannot be null or blank");
        }
    }

    /**
     * Validates that a document MIME type is supported (application/pdf or text/plain).
     *
     * @param mimeType the MIME type to validate
     * @throws IllegalArgumentException if the MIME type is not supported
     */
    public static void validateDocumentMimeType(String mimeType) {
        validateMimeType(mimeType, Set.of("application/pdf", "text/plain"));
    }
}
