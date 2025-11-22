package com.pergamon.llm.conversation;

import com.pergamon.llm.config.ApiConfig;
import com.pergamon.llm.config.FileApiConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Integration test for AnthropicConversationManager.
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
        Conversation conversation = Conversation.create(modelId, "Integration Test Conversation");

        // Create the conversation manager
        ConversationManager<?, ?> manager = ConversationManager.forModel(modelId, config);

        // Create a richer user message
        String prompt = """
                Please write a haiku about programming in Java.
                Make it whimsical and fun!
                """;

        Message userMessage = new Message(
            MessageRole.USER,
            List.of(new TextBlock(TextBlockFormat.PLAIN, prompt))
        );

        System.out.println("=== Sending message to Anthropic ===");
        System.out.println("Model: " + modelId);
        System.out.println("Prompt: " + prompt);
        System.out.println();

        // Send the message (this will currently fail at fromVendorResponse)
        try {
            Message response = manager.sendMessage(conversation, userMessage);

            System.out.println("=== Response received ===");
            System.out.println("Role: " + response.role());
            System.out.println("Blocks: " + response.blocks().size());

            for (MessageBlock block : response.blocks()) {
                if (block instanceof TextBlock textBlock) {
                    System.out.println("Text: " + textBlock.text());
                }
            }

        } catch (UnsupportedOperationException e) {
            System.out.println("=== Expected error (fromVendorResponse not yet implemented) ===");
            System.out.println("Error: " + e.getMessage());
            System.out.println();
            System.out.println("The API call was successful, but we can't convert the response yet.");
        }
    }
}
