package com.pergamon.llm.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pergamon.llm.config.ApiConfig;
import com.pergamon.llm.config.FileApiConfig;
import com.pergamon.llm.conversation.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for logging functionality.
 * Tests that API calls and exceptions are properly logged via AOP aspects.
 * Uses in-memory ListAppender to avoid file handle conflicts between tests.
 */
public class LoggingIntegrationTest {

    private ListAppender<ILoggingEvent> apiListAppender;
    private ListAppender<ILoggingEvent> errorListAppender;
    private Logger apiLogger;
    private Logger rootLogger;

    @BeforeEach
    public void setup() {
        // Get the API logger that the aspect uses
        apiLogger = (Logger) LoggerFactory.getLogger("com.pergamon.llm.api");

        // Create and start the in-memory appender for API logs
        apiListAppender = new ListAppender<>();
        apiListAppender.start();
        apiLogger.addAppender(apiListAppender);

        // Get the root logger for error logging
        rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        // Create and start the in-memory appender for error logs
        errorListAppender = new ListAppender<>();
        errorListAppender.start();
        rootLogger.addAppender(errorListAppender);
    }

    @AfterEach
    public void tearDown() {
        // Detach the appenders after each test
        if (apiLogger != null && apiListAppender != null) {
            apiLogger.detachAppender(apiListAppender);
        }
        if (rootLogger != null && errorListAppender != null) {
            rootLogger.detachAppender(errorListAppender);
        }
    }

    @Test
    public void testApiCallsAreLogged() throws Exception {
        // Load API configuration
        ApiConfig config = FileApiConfig.fromResource("/api-keys.properties");

        // Create a conversation with a Claude model
        ModelId modelId = new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-20250514");

        // Create the conversation using the factory method
        Conversation<?, ?> conversation = Conversation.forModel(modelId, config);
        conversation.rename("Logging Test");

        // Create a simple user message
        Message userMessage = new Message(
            MessageRole.USER,
            List.of(new TextBlock(TextBlockFormat.PLAIN, "Say hello in one word."))
        );

        System.out.println("=== Testing API Logging ===");

        // Send the message - this should trigger API logging via the aspect
        Message response = conversation.sendMessage(userMessage);

        // Verify the message was successful
        assertNotNull(response, "Response should not be null");
        assertEquals(MessageRole.ASSISTANT, response.role(), "Response role should be ASSISTANT");

        // Verify API logging occurred by checking the in-memory appender
        List<ILoggingEvent> logEvents = apiListAppender.list;

        // Check that we have at least 2 log events (request + response)
        assertTrue(logEvents.size() >= 2,
            "Should have at least 2 log events (request + response), but found " + logEvents.size());

        // Check that API log contains ANTHROPIC request
        boolean hasRequest = logEvents.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("[ANTHROPIC] Request:"));
        assertTrue(hasRequest, "API log should contain ANTHROPIC request");

        // Check that API log contains ANTHROPIC response
        boolean hasResponse = logEvents.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("[ANTHROPIC] Response:"));
        assertTrue(hasResponse, "API log should contain ANTHROPIC response");

        System.out.println("API log events captured: " + logEvents.size());
        System.out.println("=== API Logging Test Passed! ===");
    }

    @Test
    public void testExceptionsAreLogged() throws Exception {
        System.out.println("=== Testing Exception Logging ===");

        // Create a conversation with an invalid API key to trigger an error
        ModelId modelId = new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-20250514");

        // Use an invalid API key
        AnthropicConversation conversation = new AnthropicConversation(modelId, "invalid-api-key");
        conversation.rename("Error Test");

        // Create a simple user message
        Message userMessage = new Message(
            MessageRole.USER,
            List.of(new TextBlock(TextBlockFormat.PLAIN, "Test"))
        );

        // Attempt to send the message - this should fail and trigger exception logging
        System.out.println("Attempting to send message with invalid API key...");
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            conversation.sendMessage(userMessage);
        });

        System.out.println("Exception caught: " + exception.getClass().getSimpleName());

        // Verify exception logging occurred by checking the in-memory appender
        List<ILoggingEvent> errorEvents = errorListAppender.list;

        // Check that we have at least one error log entry
        assertTrue(errorEvents.size() > 0,
            "Error log should have at least one entry, but found " + errorEvents.size());

        // Verify the exception was logged from the correct method
        boolean hasMethodError = errorEvents.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("AnthropicConversation.sendConversationToVendor()"));
        assertTrue(hasMethodError,
            "Error log should contain exception from AnthropicConversation.sendConversationToVendor()");

        System.out.println("Error log events captured: " + errorEvents.size());
        System.out.println("=== Exception Logging Test Passed! ===");
    }

    @Test
    public void testMultipleApiCallsLogging() throws Exception {
        System.out.println("=== Testing Multiple API Calls Logging ===");

        // Load API configuration
        ApiConfig config = FileApiConfig.fromResource("/api-keys.properties");
        ModelId modelId = new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-20250514");

        // Create the conversation using the factory method
        Conversation<?, ?> conversation = Conversation.forModel(modelId, config);
        conversation.rename("Multi-call Test");

        // Send multiple messages
        int messageCount = 3;
        for (int i = 1; i <= messageCount; i++) {
            System.out.println("Sending message " + i + " of " + messageCount);
            Message userMessage = new Message(
                MessageRole.USER,
                List.of(new TextBlock(TextBlockFormat.PLAIN, "Count to " + i))
            );
            conversation.sendMessage(userMessage);
        }

        // Verify that multiple API calls were logged
        List<ILoggingEvent> logEvents = apiListAppender.list;

        System.out.println("\n=== API Log Summary ===");
        System.out.println("Total log events: " + logEvents.size());

        // Each API call should generate 2 log entries (request + response)
        int expectedMinEntries = messageCount * 2;
        assertTrue(logEvents.size() >= expectedMinEntries,
            "Should have at least " + expectedMinEntries + " log events (request + response for each call), but found " + logEvents.size());

        // Count requests and responses
        long requestCount = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("[ANTHROPIC] Request:"))
            .count();
        long responseCount = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("[ANTHROPIC] Response:"))
            .count();

        System.out.println("Request events: " + requestCount);
        System.out.println("Response events: " + responseCount);

        assertEquals(messageCount, requestCount, "Should have " + messageCount + " request log entries");
        assertEquals(messageCount, responseCount, "Should have " + messageCount + " response log entries");

        System.out.println("=== Multiple API Calls Logging Test Passed! ===");
    }

    @Test
    public void testMultiTurnConversationContext() throws Exception {
        System.out.println("=== Testing Multi-Turn Conversation Context ===");

        // Load API configuration
        ApiConfig config = FileApiConfig.fromResource("/api-keys.properties");
        ModelId modelId = new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-20250514");

        // Create the conversation using the factory method
        Conversation<?, ?> conversation = Conversation.forModel(modelId, config);
        conversation.rename("Bedtime Story Conversation");

        // Initial prompt
        System.out.println("\n--- Turn 1: Initial Story Request ---");
        Message initialMessage = new Message(
            MessageRole.USER,
            List.of(new TextBlock(TextBlockFormat.PLAIN, "Tell me a bedtime story!"))
        );
        Message response1 = conversation.sendMessage(initialMessage);
        assertNotNull(response1, "First response should not be null");
        assertEquals(MessageRole.ASSISTANT, response1.role());
        System.out.println("Assistant: " + getFirstTextBlock(response1));

        // Verify we have 2 messages in history (user + assistant)
        assertEquals(2, conversation.messages().size(), "Should have 2 messages after first turn");

        // Follow-up with "but why???" 3 times
        for (int i = 1; i <= 3; i++) {
            System.out.println("\n--- Turn " + (i + 1) + ": But Why #" + i + " ---");
            Message followUp = new Message(
                MessageRole.USER,
                List.of(new TextBlock(TextBlockFormat.PLAIN, "But why???"))
            );
            Message response = conversation.sendMessage(followUp);
            assertNotNull(response, "Response " + (i + 1) + " should not be null");
            assertEquals(MessageRole.ASSISTANT, response.role());
            System.out.println("Assistant: " + getFirstTextBlock(response));

            // Verify message history grows correctly (2 initial + 2*i for follow-ups)
            int expectedCount = 2 + (2 * i);
            assertEquals(expectedCount, conversation.messages().size(),
                "Should have " + expectedCount + " messages after turn " + (i + 1));
        }

        // Final verification: should have 8 total messages (1 initial + 1 response + 3 follow-ups + 3 responses)
        assertEquals(8, conversation.messages().size(),
            "Should have 8 total messages (4 user + 4 assistant)");

        // Verify the conversation maintains context by checking message order
        List<Message> messages = conversation.messages();
        assertEquals(MessageRole.USER, messages.get(0).role(), "Message 0 should be USER");
        assertEquals(MessageRole.ASSISTANT, messages.get(1).role(), "Message 1 should be ASSISTANT");
        assertEquals(MessageRole.USER, messages.get(2).role(), "Message 2 should be USER");
        assertEquals(MessageRole.ASSISTANT, messages.get(3).role(), "Message 3 should be ASSISTANT");
        assertEquals(MessageRole.USER, messages.get(4).role(), "Message 4 should be USER");
        assertEquals(MessageRole.ASSISTANT, messages.get(5).role(), "Message 5 should be ASSISTANT");
        assertEquals(MessageRole.USER, messages.get(6).role(), "Message 6 should be USER");
        assertEquals(MessageRole.ASSISTANT, messages.get(7).role(), "Message 7 should be ASSISTANT");

        System.out.println("\n=== Multi-Turn Conversation Context Test Passed! ===");
        System.out.println("Final conversation history: " + conversation.messages().size() + " messages");
    }

    /**
     * Helper method to extract the first text block from a message for display.
     */
    private String getFirstTextBlock(Message message) {
        if (message.blocks().isEmpty()) {
            return "[no content]";
        }
        MessageBlock firstBlock = message.blocks().get(0);
        if (firstBlock instanceof TextBlock textBlock) {
            String text = textBlock.text();
            // Truncate if too long for display
            return text.length() > 100 ? text.substring(0, 100) + "..." : text;
        }
        return "[non-text block]";
    }
}
