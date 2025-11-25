package com.pergamon.llm.conversation;

import com.pergamon.llm.config.ApiConfig;
import com.pergamon.llm.config.FileApiConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for AnthropicConversation.
 *
 * This test requires a valid API key in src/main/resources/api-keys.properties
 * and will make actual API calls to Anthropic.
 */
public class AnthropicIntegrationTest {

    @Test
    public void testSendMessageToAnthropic() throws Exception {
        // Load API configuration
        ApiConfig config = FileApiConfig.fromResource("/api-keys.properties");

        // Create a conversation with a Claude model
        ModelId modelId = new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-20250514");

        // Create the conversation using the factory method
        Conversation<?, ?> conversation = Conversation.forModel(modelId, config);
        conversation.rename("Integration Test Conversation");

        // Create a richer user message
        String prompt = """
                Please write a haiku about programming in Java.
                Make it whimsical and fun!
                """;

        Message userMessage = new Message(
            MessageRole.USER,
            List.of(new TextBlock(TextBlockFormat.PLAIN, prompt, List.of()))
        );

        System.out.println("=== Sending message to Anthropic ===");
        System.out.println("Model: " + modelId);
        System.out.println("Prompt: " + prompt);
        System.out.println();

        // Send the message and verify the full round-trip conversion
        Message response = conversation.sendMessage(userMessage);

        System.out.println("=== Response received ===");
        System.out.println("Role: " + response.role());
        System.out.println("Blocks: " + response.blocks().size());

        // Verify the response structure
        assertNotNull(response, "Response should not be null");
        assertEquals(MessageRole.ASSISTANT, response.role(), "Response role should be ASSISTANT");
        assertFalse(response.blocks().isEmpty(), "Response should have at least one block");

        // Verify and display each block
        for (int i = 0; i < response.blocks().size(); i++) {
            MessageBlock block = response.blocks().get(i);
            System.out.println("\n=== Block " + (i + 1) + " ===");

            if (block instanceof TextBlock textBlock) {
                System.out.println("Type: TextBlock");
                System.out.println("Format: " + textBlock.format());
                System.out.println("Text: " + textBlock.text());

                // Verify text block properties
                assertNotNull(textBlock.text(), "Text should not be null");
                assertNotNull(textBlock.format(), "Format should not be null");

                // Anthropic typically returns markdown, but we should accept any format
                assertTrue(
                    textBlock.format() == TextBlockFormat.MARKDOWN ||
                    textBlock.format() == TextBlockFormat.PLAIN ||
                    textBlock.format() == TextBlockFormat.HTML,
                    "Format should be MARKDOWN, PLAIN, or HTML"
                );

            } else if (block instanceof UnknownBlock unknownBlock) {
                System.out.println("Type: UnknownBlock");
                System.out.println("Data: " + unknownBlock.vendorData());

                // We don't expect unknown blocks for a simple text response, but it's not an error
                System.out.println("WARNING: Received UnknownBlock - this might indicate a new content type");

            } else {
                fail("Unexpected block type: " + block.getClass().getName());
            }
        }

        System.out.println("\n=== Round-trip conversion successful! ===");
    }
}
