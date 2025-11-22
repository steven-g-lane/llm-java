package com.pergamon.llm.conversation;

import org.junit.jupiter.api.Test;

import static com.pergamon.llm.conversation.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class ConversationTest {

    /**
     * Test implementation of Conversation for unit testing.
     * This stub implementation allows us to test the base Conversation functionality
     * without requiring actual vendor API calls.
     */
    private static class TestConversation extends Conversation<String, String> {
        TestConversation(ModelId modelId) {
            super(modelId);
        }

        TestConversation(ModelId modelId, String name) {
            super(modelId, name);
        }

        @Override
        protected String toVendorMessage(Message message) {
            return "vendor-message";
        }

        @Override
        protected String sendMessageToVendor(String vendorMessage) {
            return "vendor-response";
        }

        @Override
        protected String vendorResponseToVendorMessage(String vendorResponse) {
            return "vendor-response-message";
        }

        @Override
        protected Message fromVendorResponse(String vendorResponse) {
            return new Message(MessageRole.ASSISTANT, java.util.List.of(
                new TextBlock(TextBlockFormat.PLAIN, "test response")
            ));
        }
    }


    @Test
    void testCreateConversationWithModelId() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);

        assertEquals(CLAUDE_SONNET_45, conversation.modelId());
        assertFalse(conversation.id().isPresent(), "ID should be null until set by database");
        assertFalse(conversation.name().isBlank(), "Should have generated default name");
        assertTrue(conversation.name().startsWith("Untitled-"), "Default name should start with 'Untitled-'");
        assertFalse(conversation.isStarred());
        assertTrue(conversation.messages().isEmpty());
    }

    @Test
    void testCreateConversationWithCustomName() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45, SAMPLE_CONVERSATION_NAME);

        assertEquals(SAMPLE_CONVERSATION_NAME, conversation.name());
        assertEquals(CLAUDE_SONNET_45, conversation.modelId());
    }

    @Test
    void testCreateConversationWithNullNameGeneratesDefault() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45, null);

        assertNotNull(conversation.name());
        assertTrue(conversation.name().startsWith("Untitled-"));
    }

    @Test
    void testCreateConversationWithBlankNameGeneratesDefault() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45, "   ");

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
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);

        assertFalse(conversation.id().isPresent(), "ID should be absent initially");

        conversation.setId(SAMPLE_DB_UUID);

        assertTrue(conversation.id().isPresent());
        assertEquals(SAMPLE_DB_UUID, conversation.id().get());
    }

    @Test
    void testRenameConversation() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45, "Original Name");

        conversation.rename("New Name");

        assertEquals("New Name", conversation.name());
    }

    @Test
    void testRenameWithNullThrows() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);

        assertThrows(IllegalArgumentException.class, () -> {
            conversation.rename(null);
        }, "Should throw when renaming to null");
    }

    @Test
    void testRenameWithBlankThrows() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);

        assertThrows(IllegalArgumentException.class, () -> {
            conversation.rename("   ");
        }, "Should throw when renaming to blank string");
    }

    @Test
    void testStarConversation() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);

        assertFalse(conversation.isStarred(), "Should not be starred initially");

        conversation.star();

        assertTrue(conversation.isStarred());
    }

    @Test
    void testUnstarConversation() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);
        conversation.star();

        assertTrue(conversation.isStarred());

        conversation.unstar();

        assertFalse(conversation.isStarred());
    }

    @Test
    void testSetStarred() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);

        conversation.setStarred(true);
        assertTrue(conversation.isStarred());

        conversation.setStarred(false);
        assertFalse(conversation.isStarred());
    }

    @Test
    void testToggleStar() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);

        assertFalse(conversation.isStarred());

        conversation.toggleStar();
        assertTrue(conversation.isStarred());

        conversation.toggleStar();
        assertFalse(conversation.isStarred());
    }

    @Test
    void testVendorConversationId() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);

        assertFalse(conversation.vendorConversationId().isPresent(), "Should be absent initially");

        conversation.setVendorConversationId("vendor-conv-123");

        assertTrue(conversation.vendorConversationId().isPresent());
        assertEquals("vendor-conv-123", conversation.vendorConversationId().get());
    }

    @Test
    void testVendorProjectId() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);

        assertFalse(conversation.vendorProjectId().isPresent(), "Should be absent initially");

        conversation.setVendorProjectId("vendor-project-456");

        assertTrue(conversation.vendorProjectId().isPresent());
        assertEquals("vendor-project-456", conversation.vendorProjectId().get());
    }

    @Test
    void testAppendMessage() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);
        Message message = createUserMessage(SAMPLE_TEXT);

        conversation.append(message);

        assertEquals(1, conversation.messages().size());
        assertEquals(message, conversation.messages().get(0));
    }

    @Test
    void testAppendMultipleMessages() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);

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
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);

        assertThrows(IllegalArgumentException.class, () -> {
            conversation.append(null);
        }, "Should throw when appending null message");
    }

    @Test
    void testMessagesListIsUnmodifiable() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);
        Message message = createUserMessage(SAMPLE_TEXT);
        conversation.append(message);

        assertThrows(UnsupportedOperationException.class, () -> {
            conversation.messages().add(createAssistantMessage("Another message"));
        }, "Messages list should be unmodifiable");
    }

    @Test
    void testClearMessages() {
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45);

        conversation.append(createUserMessage("Message 1"));
        conversation.append(createAssistantMessage("Message 2"));

        assertEquals(2, conversation.messages().size());

        conversation.clearMessages();

        assertTrue(conversation.messages().isEmpty());
    }

    @Test
    void testDefaultNameIsUnique() {
        Conversation conv1 = new TestConversation(CLAUDE_SONNET_45);
        Conversation conv2 = new TestConversation(CLAUDE_SONNET_45);

        assertNotEquals(conv1.name(), conv2.name(),
                "Default names should be unique (different timestamps/random suffixes)");
    }

    @Test
    void testFullConversationWorkflow() {
        // Create conversation
        Conversation conversation = new TestConversation(CLAUDE_SONNET_45, SAMPLE_CONVERSATION_NAME);

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
}
