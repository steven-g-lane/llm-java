package com.pergamon.llm.conversation;

import com.pergamon.llm.config.ApiConfig;
import com.pergamon.llm.config.FileApiConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for document handling in AnthropicConversation.
 * These tests require a valid API key in src/main/resources/api-keys.properties.
 *
 * Note: These tests use external resources (local files and URLs)
 * and are intended for manual verification during development.
 */
class AnthropicConversationDocumentTest {

    private AnthropicConversation conversation;

    @BeforeEach
    void setUp() throws Exception {
        // Load API configuration from properties file
        ApiConfig config = FileApiConfig.fromResource("/api-keys.properties");
        String apiKey = config.getApiKey(Vendor.ANTHROPIC)
                .orElseThrow(() -> new IllegalStateException("Anthropic API key not found in api-keys.properties"));

        conversation = new AnthropicConversation(
                new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-20250514"),
                "Document Test Conversation",
                apiKey
        );
    }

    @Test
    void testSendMessageWithUrlPdfDocument() {
        // Create a message with a URL PDF document from arXiv
        URI pdfUri = URI.create("https://arxiv.org/pdf/2511.16660");
        URLPDFDocumentBlock urlPdf = new URLPDFDocumentBlock(pdfUri, "application/pdf", List.of());

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "Please summarize this PDF document.", List.of()))
                .withBlock(urlPdf);

        // Send the message
        Message response = conversation.sendMessage(userMessage);

        // Verify we got a response
        assertNotNull(response);
        assertEquals(MessageRole.ASSISTANT, response.role());
        assertFalse(response.blocks().isEmpty());
        assertTrue(response.blocks().get(0) instanceof TextBlock);

        TextBlock responseText = (TextBlock) response.blocks().get(0);
        assertNotNull(responseText.text());
        assertFalse(responseText.text().isBlank());

        System.out.println("URL PDF Test Response: " + responseText.text());
    }

    @Test
    void testSendMessageWithFilePathPdfDocument() {
        // Create a message with a local PDF file
        String localPdfPath = "/Users/slane/Desktop/2402.00159v2.pdf";
        FilePathPDFDocumentBlock filePathPdf = new FilePathPDFDocumentBlock(localPdfPath, "application/pdf", List.of());

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "What are the main topics covered in this document?", List.of()))
                .withBlock(filePathPdf);

        // Send the message
        Message response = conversation.sendMessage(userMessage);

        // Verify we got a response
        assertNotNull(response);
        assertEquals(MessageRole.ASSISTANT, response.role());
        assertFalse(response.blocks().isEmpty());
        assertTrue(response.blocks().get(0) instanceof TextBlock);

        TextBlock responseText = (TextBlock) response.blocks().get(0);
        assertNotNull(responseText.text());
        assertFalse(responseText.text().isBlank());

        System.out.println("File Path PDF Test Response: " + responseText.text());
    }

    @Test
    void testSendMessageWithBase64PdfDocument() throws IOException {
        // Create a message with a base64-encoded PDF
        // Read the local PDF and encode it to base64
        String localPdfPath = "/Users/slane/Desktop/2402.00159v2.pdf";
        byte[] pdfBytes = Files.readAllBytes(Path.of(localPdfPath));
        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

        Base64PDFDocumentBlock base64PdfBlock = new Base64PDFDocumentBlock(base64Pdf, "application/pdf", List.of());

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "What does this PDF say?", List.of()))
                .withBlock(base64PdfBlock);

        // Send the message
        Message response = conversation.sendMessage(userMessage);

        // Verify we got a response
        assertNotNull(response);
        assertEquals(MessageRole.ASSISTANT, response.role());
        assertFalse(response.blocks().isEmpty());
        assertTrue(response.blocks().get(0) instanceof TextBlock);

        TextBlock responseText = (TextBlock) response.blocks().get(0);
        assertNotNull(responseText.text());
        assertFalse(responseText.text().isBlank());

        System.out.println("Base64 PDF Test Response: " + responseText.text());
    }

    @Test
    void testSendMessageWithPlainTextDocument() throws IOException {
        // Create a message with a plain text document (package.json)
        String textDocPath = "/Users/slane/Documents/Code/electron/ai-client/package.json";
        String plainTextContent = Files.readString(Path.of(textDocPath));
        PlainTextDocumentBlock plainTextDoc = new PlainTextDocumentBlock(plainTextContent, "text/plain", List.of());

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "Please summarize this text document.", List.of()))
                .withBlock(plainTextDoc);

        // Send the message
        Message response = conversation.sendMessage(userMessage);

        // Verify we got a response
        assertNotNull(response);
        assertEquals(MessageRole.ASSISTANT, response.role());
        assertFalse(response.blocks().isEmpty());
        assertTrue(response.blocks().get(0) instanceof TextBlock);

        TextBlock responseText = (TextBlock) response.blocks().get(0);
        assertNotNull(responseText.text());
        assertFalse(responseText.text().isBlank());

        System.out.println("Plain Text Document Test Response: " + responseText.text());
    }

    @Test
    void testSendMessageWithMultipleDocuments() throws IOException {
        // Test with multiple document types in one message
        String textDocPath = "/Users/slane/Documents/Code/electron/ai-client/package.json";
        String plainTextContent = Files.readString(Path.of(textDocPath));
        PlainTextDocumentBlock plainTextDoc = new PlainTextDocumentBlock(plainTextContent, "text/plain", List.of());

        String localPdfPath = "/Users/slane/Desktop/2402.00159v2.pdf";
        FilePathPDFDocumentBlock filePathPdf = new FilePathPDFDocumentBlock(localPdfPath, "application/pdf", List.of());

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "I'm providing two documents. The first is a package.json file, and the second is a research paper. Please identify any connections between them.", List.of()))
                .withBlock(plainTextDoc)
                .withBlock(filePathPdf);

        // Send the message
        Message response = conversation.sendMessage(userMessage);

        // Verify we got a response
        assertNotNull(response);
        assertEquals(MessageRole.ASSISTANT, response.role());
        assertFalse(response.blocks().isEmpty());
        assertTrue(response.blocks().get(0) instanceof TextBlock);

        TextBlock responseText = (TextBlock) response.blocks().get(0);
        assertNotNull(responseText.text());
        assertFalse(responseText.text().isBlank());

        System.out.println("Multiple Documents Test Response: " + responseText.text());
    }

    @Test
    void testInvalidPdfFilePathThrowsException() {
        // Test that a non-existent PDF file throws an exception
        String nonExistentPath = "/path/to/nonexistent/document.pdf";
        FilePathPDFDocumentBlock filePathPdf = new FilePathPDFDocumentBlock(nonExistentPath, "application/pdf", List.of());

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "Summarize this document.", List.of()))
                .withBlock(filePathPdf);

        // Should throw when trying to convert the message
        assertThrows(IllegalArgumentException.class, () -> {
            conversation.sendMessage(userMessage);
        });
    }

    @Test
    void testInvalidDocumentMimeTypeThrowsException() {
        // Test that an unsupported MIME type throws an exception
        String plainTextContent = "This is a test document.";
        PlainTextDocumentBlock plainTextDoc = new PlainTextDocumentBlock(plainTextContent, "application/msword", List.of());

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "Summarize this document.", List.of()))
                .withBlock(plainTextDoc);

        // Should throw when trying to validate the MIME type
        assertThrows(IllegalArgumentException.class, () -> {
            conversation.sendMessage(userMessage);
        });
    }

    @Test
    void testInvalidDocumentUriThrowsException() {
        // Test that an invalid URI scheme throws an exception
        URI invalidUri = URI.create("ftp://example.com/document.pdf");
        URLPDFDocumentBlock urlPdf = new URLPDFDocumentBlock(invalidUri, "application/pdf", List.of());

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "Summarize this document.", List.of()))
                .withBlock(urlPdf);

        // Should throw when trying to validate the URI
        assertThrows(IllegalArgumentException.class, () -> {
            conversation.sendMessage(userMessage);
        });
    }

    @Test
    void testPdfDocumentWithCitations() {
        // Test that sending a PDF with a request for citations returns citations in the response
        String localPdfPath = "/Users/slane/Desktop/10.48550_arxiv.2509.04664.pdf";
        FilePathPDFDocumentBlock filePathPdf = new FilePathPDFDocumentBlock(localPdfPath, "application/pdf", List.of());

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "Please provide a summary of this document with citations to specific sections.", List.of()))
                .withBlock(filePathPdf);

        // Send the message
        Message response = conversation.sendMessage(userMessage);

        // Verify we got a response
        assertNotNull(response);
        assertEquals(MessageRole.ASSISTANT, response.role());
        assertFalse(response.blocks().isEmpty());

        // Collect all text blocks and citations from the response
        StringBuilder fullText = new StringBuilder();
        List<TextCitation> allCitations = new java.util.ArrayList<>();

        for (MessageBlock block : response.blocks()) {
            if (block instanceof TextBlock textBlock) {
                fullText.append(textBlock.text());
                allCitations.addAll(textBlock.citations());
            }
        }

        // Verify we got text content
        assertFalse(fullText.toString().isBlank(), "Response text should not be blank");

        // Check for citations - they MUST be returned when requesting a summary with citations
        assertFalse(allCitations.isEmpty(),
            "Expected at least one citation when requesting a summary with citations");

        System.out.println("PDF Citation Test Response: " + fullText.toString());
        System.out.println("Number of citations: " + allCitations.size());

        // Print citation details
        System.out.println("\nCitations found:");
        for (int i = 0; i < allCitations.size(); i++) {
            TextCitation citation = allCitations.get(i);
            System.out.println("  Citation " + (i + 1) + ":");
            System.out.println("    Type: " + citation.getClass().getSimpleName());
            System.out.println("    Cited text: " + citation.citedText());
            System.out.println("    Title: " + citation.title());

            // Print type-specific details
            switch (citation) {
                case PageLocationCitation pageCitation -> {
                    System.out.println("    Document index: " + pageCitation.documentIndex());
                    System.out.println("    Start page: " + pageCitation.startPageNumber());
                    System.out.println("    End page: " + pageCitation.endPageNumber());
                }
                case CharLocationCitation charCitation -> {
                    System.out.println("    Document index: " + charCitation.documentIndex());
                    System.out.println("    Start char: " + charCitation.startCharIndex());
                    System.out.println("    End char: " + charCitation.endCharIndex());
                }
                case ContentBlockLocationCitation blockCitation -> {
                    System.out.println("    Document index: " + blockCitation.documentIndex());
                    System.out.println("    Start block: " + blockCitation.startBlockIndex());
                    System.out.println("    End block: " + blockCitation.endBlockIndex());
                }
                case WebSearchResultCitation webCitation -> {
                    System.out.println("    URL: " + webCitation.url());
                }
                case SearchResultCitation searchCitation -> {
                    System.out.println("    Source: " + searchCitation.source());
                }
                case UnknownCitation unknownCitation -> {
                    System.out.println("    Raw data: " + unknownCitation.rawData());
                }
            }
        }
    }

    @Test
    void testBlankPlainTextThrowsException() {
        // Test that blank plain text content throws an exception
        String blankText = "   ";
        PlainTextDocumentBlock plainTextDoc = new PlainTextDocumentBlock(blankText, "text/plain", List.of());

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "Summarize this document.", List.of()))
                .withBlock(plainTextDoc);

        // Should throw when trying to validate the plain text
        assertThrows(IllegalArgumentException.class, () -> {
            conversation.sendMessage(userMessage);
        });
    }
}
