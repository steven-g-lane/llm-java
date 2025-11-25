package com.pergamon.llm.conversation;

import com.pergamon.llm.config.ApiConfig;
import com.pergamon.llm.config.FileApiConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for image handling in AnthropicConversation.
 * These tests require a valid API key in src/main/resources/api-keys.properties.
 *
 * Note: These tests use fragile external resources (local file and Wikipedia URL)
 * and are intended for manual verification during development.
 */
class AnthropicConversationImageTest {

    private AnthropicConversation conversation;

    @BeforeEach
    void setUp() throws Exception {
        // Load API configuration from properties file
        ApiConfig config = FileApiConfig.fromResource("/api-keys.properties");
        String apiKey = config.getApiKey(Vendor.ANTHROPIC)
                .orElseThrow(() -> new IllegalStateException("Anthropic API key not found in api-keys.properties"));

        conversation = new AnthropicConversation(
                new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-20250514"),
                "Image Test Conversation",
                apiKey
        );
    }

    @Test
    void testSendMessageWithUrlImage() {
        // Create a message with a URL image from Wikipedia Commons
        URI imageUri = URI.create("https://upload.wikimedia.org/wikipedia/commons/4/49/Andrei_Gromyko_1972_%28cropped%29%28b%29%282%29.jpg");
        URLImageBlock urlImage = new URLImageBlock(imageUri, "image/jpeg");

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "What do you see in this image?", List.of()))
                .withBlock(urlImage);

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

        System.out.println("URL Image Test Response: " + responseText.text());
    }

    @Test
    void testSendMessageWithFilePathImage() {
        // Create a message with a local file image
        String localImagePath = "/Users/slane/Desktop/vilna.jpg";
        FilePathImageBlock filePathImage = new FilePathImageBlock(localImagePath, "image/jpeg");

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "Describe what you see in this image.", List.of()))
                .withBlock(filePathImage);

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

        System.out.println("File Path Image Test Response: " + responseText.text());
    }

    @Test
    void testSendMessageWithBase64Image() {
        // Create a simple 1x1 red PNG image in base64
        // This is a minimal valid PNG file
        String base64RedPixel = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==";

        Base64ImageBlock base64Image = new Base64ImageBlock(base64RedPixel, "image/png");

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "What color is this image?", List.of()))
                .withBlock(base64Image);

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

        System.out.println("Base64 Image Test Response: " + responseText.text());
    }

    @Test
    void testSendMessageWithMultipleImages() {
        // Test with multiple image types in one message
        String localImagePath = "/Users/slane/Desktop/vilna.jpg";
        FilePathImageBlock filePathImage = new FilePathImageBlock(localImagePath, "image/jpeg");

        URI imageUri = URI.create("https://upload.wikimedia.org/wikipedia/commons/4/49/Andrei_Gromyko_1972_%28cropped%29%28b%29%282%29.jpg");
        URLImageBlock urlImage = new URLImageBlock(imageUri, "image/jpeg");

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "Compare these two images. What similarities or differences do you notice?", List.of()))
                .withBlock(filePathImage)
                .withBlock(urlImage);

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

        System.out.println("Multiple Images Test Response: " + responseText.text());
    }

    @Test
    void testInvalidFilePathThrowsException() {
        // Test that a non-existent file throws an exception
        String nonExistentPath = "/path/to/nonexistent/image.jpg";
        FilePathImageBlock filePathImage = new FilePathImageBlock(nonExistentPath, "image/jpeg");

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "What's in this image?", List.of()))
                .withBlock(filePathImage);

        // Should throw when trying to convert the message
        assertThrows(IllegalArgumentException.class, () -> {
            conversation.sendMessage(userMessage);
        });
    }

    @Test
    void testInvalidMimeTypeThrowsException() {
        // Test that an unsupported MIME type throws an exception
        String localImagePath = "/Users/slane/Desktop/vilna.jpg";
        FilePathImageBlock filePathImage = new FilePathImageBlock(localImagePath, "image/bmp");

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "What's in this image?", List.of()))
                .withBlock(filePathImage);

        // Should throw when trying to validate the MIME type
        assertThrows(IllegalArgumentException.class, () -> {
            conversation.sendMessage(userMessage);
        });
    }

    @Test
    void testInvalidUriThrowsException() {
        // Test that an invalid URI scheme throws an exception
        URI invalidUri = URI.create("ftp://example.com/image.jpg");
        URLImageBlock urlImage = new URLImageBlock(invalidUri, "image/jpeg");

        Message userMessage = new Message(MessageRole.USER, List.of())
                .withBlock(new TextBlock(TextBlockFormat.PLAIN, "What's in this image?", List.of()))
                .withBlock(urlImage);

        // Should throw when trying to validate the URI
        assertThrows(IllegalArgumentException.class, () -> {
            conversation.sendMessage(userMessage);
        });
    }
}
