package com.pergamon.llm.conversation;

import java.util.List;

/**
 * Shared test fixtures and constants for conversation tests.
 */
public class TestFixtures {

    // Model IDs
    public static final ModelId CLAUDE_SONNET_45 = new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-5");
    public static final ModelId GPT4O = new ModelId(Vendor.OPENAI, "gpt-4o");

    // Sample text content
    public static final String SAMPLE_TEXT = "Hello, World!";

    // Database IDs
    public static final String SAMPLE_DB_UUID = "550e8400-e29b-41d4-a716-446655440000";

    // Conversation names
    public static final String SAMPLE_CONVERSATION_NAME = "Test Conversation";

    // Factory methods

    public static TextBlock createPlainTextBlock(String text) {
        return new TextBlock(TextBlockFormat.PLAIN, text, List.of());
    }

    public static Message createUserMessage(String text) {
        return new Message(MessageRole.USER, List.of(createPlainTextBlock(text)));
    }

    public static Message createAssistantMessage(String text) {
        return new Message(MessageRole.ASSISTANT, List.of(createPlainTextBlock(text)));
    }
}
