package com.pergamon.llm.conversation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.pergamon.llm.conversation.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class ConversationTest {

    /**
     * Test implementation of Conversation for unit testing.
     * This stub implementation allows us to test the base Conversation functionality
     * without requiring actual vendor API calls.
     */
    private static class TestConversation extends Conversation<String, String> {
        private long nextInputTokens = 100L;
        private long nextOutputTokens = 20L;

        TestConversation(ModelId modelId) {
            super(modelId);
        }

        TestConversation(ModelId modelId, String name) {
            super(modelId, name);
        }

        /**
         * Sets the token values that will be returned in the next response message.
         */
        void setNextTokenCounts(long inputTokens, long outputTokens) {
            this.nextInputTokens = inputTokens;
            this.nextOutputTokens = outputTokens;
        }

        @Override
        protected String toVendorMessage(Message message) {
            return "vendor-message";
        }

        @Override
        protected String sendConversationToVendor() {
            return "vendor-response";
        }

        @Override
        protected String vendorResponseToVendorMessage(String vendorResponse) {
            return "vendor-response-message";
        }

        @Override
        protected Message fromVendorResponse(String vendorResponse) {
            return new Message(
                MessageRole.ASSISTANT,
                java.util.List.of(new TextBlock(TextBlockFormat.PLAIN, "test response", List.of())),
                nextInputTokens,
                nextOutputTokens
            );
        }

        @Override
        protected boolean isCacheable() {
            return false; // Test conversation doesn't support caching
        }

        @Override
        protected void configureCaching() {
            // No-op for test conversation
        }
    }


    @Test
    void testCreateConversationWithModelId() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);

        assertEquals(CLAUDE_SONNET_45, conversation.modelId());
        assertFalse(conversation.id().isPresent(), "ID should be null until set by database");
        assertFalse(conversation.name().isBlank(), "Should have generated default name");
        assertTrue(conversation.name().startsWith("Untitled-"), "Default name should start with 'Untitled-'");
        assertFalse(conversation.isStarred());
        assertTrue(conversation.messages().isEmpty());
    }

    @Test
    void testCreateConversationWithCustomName() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45, SAMPLE_CONVERSATION_NAME);

        assertEquals(SAMPLE_CONVERSATION_NAME, conversation.name());
        assertEquals(CLAUDE_SONNET_45, conversation.modelId());
    }

    @Test
    void testCreateConversationWithNullNameGeneratesDefault() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45, null);

        assertNotNull(conversation.name());
        assertTrue(conversation.name().startsWith("Untitled-"));
    }

    @Test
    void testCreateConversationWithBlankNameGeneratesDefault() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45, "   ");

        assertNotNull(conversation.name());
        assertTrue(conversation.name().startsWith("Untitled-"));
    }

    @Test
    void testCreateConversationWithNullModelIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TestConversation(null);
        }, "Should throw when modelId is null");
    }

    @Test
    void testSetIdByPersistenceLayer() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);

        assertFalse(conversation.id().isPresent(), "ID should be absent initially");

        conversation.setId(SAMPLE_DB_UUID);

        assertTrue(conversation.id().isPresent());
        assertEquals(SAMPLE_DB_UUID, conversation.id().get());
    }

    @Test
    void testRenameConversation() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45, "Original Name");

        conversation.rename("New Name");

        assertEquals("New Name", conversation.name());
    }

    @Test
    void testRenameWithNullThrows() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);

        assertThrows(IllegalArgumentException.class, () -> {
            conversation.rename(null);
        }, "Should throw when renaming to null");
    }

    @Test
    void testRenameWithBlankThrows() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);

        assertThrows(IllegalArgumentException.class, () -> {
            conversation.rename("   ");
        }, "Should throw when renaming to blank string");
    }

    @Test
    void testStarConversation() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);

        assertFalse(conversation.isStarred(), "Should not be starred initially");

        conversation.star();

        assertTrue(conversation.isStarred());
    }

    @Test
    void testUnstarConversation() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);
        conversation.star();

        assertTrue(conversation.isStarred());

        conversation.unstar();

        assertFalse(conversation.isStarred());
    }

    @Test
    void testSetStarred() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);

        conversation.setStarred(true);
        assertTrue(conversation.isStarred());

        conversation.setStarred(false);
        assertFalse(conversation.isStarred());
    }

    @Test
    void testToggleStar() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);

        assertFalse(conversation.isStarred());

        conversation.toggleStar();
        assertTrue(conversation.isStarred());

        conversation.toggleStar();
        assertFalse(conversation.isStarred());
    }

    @Test
    void testVendorConversationId() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);

        assertFalse(conversation.vendorConversationId().isPresent(), "Should be absent initially");

        conversation.setVendorConversationId("vendor-conv-123");

        assertTrue(conversation.vendorConversationId().isPresent());
        assertEquals("vendor-conv-123", conversation.vendorConversationId().get());
    }

    @Test
    void testVendorProjectId() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);

        assertFalse(conversation.vendorProjectId().isPresent(), "Should be absent initially");

        conversation.setVendorProjectId("vendor-project-456");

        assertTrue(conversation.vendorProjectId().isPresent());
        assertEquals("vendor-project-456", conversation.vendorProjectId().get());
    }

    @Test
    void testAppendMessage() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);
        Message message = createUserMessage(SAMPLE_TEXT);

        conversation.append(message);

        assertEquals(1, conversation.messages().size());
        assertEquals(message, conversation.messages().get(0));
    }

    @Test
    void testAppendMultipleMessages() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);

        Message msg1 = createUserMessage("First message");
        Message msg2 = createAssistantMessage("Response");
        Message msg3 = createUserMessage("Follow-up");

        conversation.append(msg1);
        conversation.append(msg2);
        conversation.append(msg3);

        assertEquals(3, conversation.messages().size());
        assertEquals(msg1, conversation.messages().get(0));
        assertEquals(msg2, conversation.messages().get(1));
        assertEquals(msg3, conversation.messages().get(2));
    }

    @Test
    void testAppendNullMessageThrows() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);

        assertThrows(IllegalArgumentException.class, () -> {
            conversation.append(null);
        }, "Should throw when appending null message");
    }

    @Test
    void testMessagesListIsUnmodifiable() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);
        Message message = createUserMessage(SAMPLE_TEXT);
        conversation.append(message);

        assertThrows(UnsupportedOperationException.class, () -> {
            conversation.messages().add(createAssistantMessage("Another message"));
        }, "Messages list should be unmodifiable");
    }

    @Test
    void testClearMessages() {
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45);

        conversation.append(createUserMessage("Message 1"));
        conversation.append(createAssistantMessage("Message 2"));

        assertEquals(2, conversation.messages().size());

        conversation.clearMessages();

        assertTrue(conversation.messages().isEmpty());
    }

    @Test
    void testDefaultNameIsUnique() {
        Conversation<String, String> conv1 = new TestConversation(CLAUDE_SONNET_45);
        Conversation<String, String> conv2 = new TestConversation(CLAUDE_SONNET_45);

        assertNotEquals(conv1.name(), conv2.name(),
                "Default names should be unique (different timestamps/random suffixes)");
    }

    @Test
    void testFullConversationWorkflow() {
        // Create conversation
        Conversation<String, String> conversation = new TestConversation(CLAUDE_SONNET_45, SAMPLE_CONVERSATION_NAME);

        // Simulate database persistence
        conversation.setId(SAMPLE_DB_UUID);

        // Set vendor metadata
        conversation.setVendorConversationId("anthropic-conv-abc");
        conversation.setVendorProjectId("anthropic-project-xyz");

        // Add messages
        conversation.append(createUserMessage("How do I reset my password?"));
        conversation.append(createAssistantMessage("Here are the steps..."));

        // Star the conversation
        conversation.star();

        // Verify final state
        assertEquals(SAMPLE_CONVERSATION_NAME, conversation.name());
        assertEquals(SAMPLE_DB_UUID, conversation.id().get());
        assertEquals("anthropic-conv-abc", conversation.vendorConversationId().get());
        assertEquals("anthropic-project-xyz", conversation.vendorProjectId().get());
        assertEquals(2, conversation.messages().size());
        assertTrue(conversation.isStarred());
        assertEquals(CLAUDE_SONNET_45, conversation.modelId());
    }

    @Test
    void testTokenCountsInitiallyZero() {
        TestConversation conversation = new TestConversation(CLAUDE_SONNET_45);

        assertEquals(0L, conversation.getTotalInputTokens(), "Initial input tokens should be 0");
        assertEquals(0L, conversation.getTotalOutputTokens(), "Initial output tokens should be 0");
    }

    @Test
    void testSendMessageAccumulatesTokens() {
        TestConversation conversation = new TestConversation(CLAUDE_SONNET_45);
        conversation.setNextTokenCounts(150L, 25L);

        Message userMessage = createUserMessage("Hello");
        Message response = conversation.sendMessage(userMessage);

        assertEquals(150L, response.inputTokens());
        assertEquals(25L, response.outputTokens());
        assertEquals(150L, conversation.getTotalInputTokens());
        assertEquals(25L, conversation.getTotalOutputTokens());
    }

    @Test
    void testMultipleSendMessageAccumulatesTokens() {
        TestConversation conversation = new TestConversation(CLAUDE_SONNET_45);

        // First message
        conversation.setNextTokenCounts(150L, 25L);
        conversation.sendMessage(createUserMessage("First message"));

        // Second message with different token counts
        conversation.setNextTokenCounts(200L, 35L);
        conversation.sendMessage(createUserMessage("Second message"));

        // Third message
        conversation.setNextTokenCounts(175L, 30L);
        conversation.sendMessage(createUserMessage("Third message"));

        // Verify totals are accumulated
        assertEquals(525L, conversation.getTotalInputTokens(), "Total input tokens should be 150 + 200 + 175");
        assertEquals(90L, conversation.getTotalOutputTokens(), "Total output tokens should be 25 + 35 + 30");
    }

    @Test
    void testClearMessagesResetTokenCounters() {
        TestConversation conversation = new TestConversation(CLAUDE_SONNET_45);

        // Send some messages to accumulate tokens
        conversation.setNextTokenCounts(150L, 25L);
        conversation.sendMessage(createUserMessage("First message"));
        conversation.setNextTokenCounts(200L, 35L);
        conversation.sendMessage(createUserMessage("Second message"));

        assertEquals(350L, conversation.getTotalInputTokens());
        assertEquals(60L, conversation.getTotalOutputTokens());

        // Clear messages
        conversation.clearMessages();

        // Verify tokens are reset
        assertEquals(0L, conversation.getTotalInputTokens(), "Input tokens should be reset to 0");
        assertEquals(0L, conversation.getTotalOutputTokens(), "Output tokens should be reset to 0");
        assertTrue(conversation.messages().isEmpty(), "Messages should be cleared");
    }

    @Test
    void testUserMessageHasZeroTokens() {
        Message userMessage = createUserMessage("Hello");

        assertEquals(0L, userMessage.inputTokens(), "User messages should have 0 input tokens");
        assertEquals(0L, userMessage.outputTokens(), "User messages should have 0 output tokens");
    }

    @Test
    void testAssistantMessageFromVendorHasTokens() {
        TestConversation conversation = new TestConversation(CLAUDE_SONNET_45);
        conversation.setNextTokenCounts(150L, 25L);

        Message response = conversation.sendMessage(createUserMessage("Hello"));

        assertEquals(MessageRole.ASSISTANT, response.role());
        assertEquals(150L, response.inputTokens(), "Assistant response should have input tokens from API");
        assertEquals(25L, response.outputTokens(), "Assistant response should have output tokens from API");
    }
}
