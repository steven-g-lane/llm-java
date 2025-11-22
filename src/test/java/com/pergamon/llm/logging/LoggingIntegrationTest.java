package com.pergamon.llm.logging;

import com.pergamon.llm.config.ApiConfig;
import com.pergamon.llm.config.FileApiConfig;
import com.pergamon.llm.conversation.*;
import com.pergamon.llm.util.LogTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for logging functionality.
 * Tests that API calls and exceptions are properly logged via AOP aspects.
 */
public class LoggingIntegrationTest {

    @BeforeEach
    public void setup() throws Exception {
        // Clear logs before each test to ensure clean state
        LogTestHelper.clearLogs();
    }

    @Test
    public void testApiCallsAreLogged() throws Exception {
        // Load API configuration
        ApiConfig config = FileApiConfig.fromResource("/api-keys.properties");

        // Create a conversation with a Claude model
        ModelId modelId = new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-20250514");
        Conversation conversation = Conversation.create(modelId, "Logging Test");

        // Create the conversation manager
        ConversationManager<?, ?> manager = ConversationManager.forModel(modelId, config);

        // Create a simple user message
        Message userMessage = new Message(
            MessageRole.USER,
            List.of(new TextBlock(TextBlockFormat.PLAIN, "Say hello in one word."))
        );

        // Get initial log entry count
        long initialApiLogCount = LogTestHelper.getApiLogEntryCount();

        System.out.println("=== Testing API Logging ===");
        System.out.println("Initial API log entries: " + initialApiLogCount);

        // Send the message - this should trigger API logging via the aspect
        Message response = manager.sendMessage(conversation, userMessage);

        // Verify the message was successful
        assertNotNull(response, "Response should not be null");
        assertEquals(MessageRole.ASSISTANT, response.role(), "Response role should be ASSISTANT");

        // Small delay to ensure logs are flushed
        Thread.sleep(500);

        // Verify API logging occurred
        System.out.println("\n=== Verifying API Logs ===");

        // Check that API log now contains ANTHROPIC entries
        assertTrue(LogTestHelper.apiLogContainsRequest("ANTHROPIC"),
            "API log should contain ANTHROPIC request");
        assertTrue(LogTestHelper.apiLogContainsResponse("ANTHROPIC"),
            "API log should contain ANTHROPIC response");

        // Verify log entry count increased
        long finalApiLogCount = LogTestHelper.getApiLogEntryCount();
        System.out.println("Final API log entries: " + finalApiLogCount);
        assertTrue(finalApiLogCount > initialApiLogCount,
            "API log should have new entries (initial: " + initialApiLogCount + ", final: " + finalApiLogCount + ")");

        // Display recent log entries for debugging
        System.out.println("\n=== Recent API Log Entries ===");
        List<String> recentEntries = LogTestHelper.getRecentApiLogEntries(5);
        recentEntries.forEach(System.out::println);

        System.out.println("\n=== API Logging Test Passed! ===");
    }

    @Test
    public void testExceptionsAreLogged() throws Exception {
        System.out.println("=== Testing Exception Logging ===");

        // Get initial error log count
        long initialErrorLogCount = LogTestHelper.getErrorLogEntryCount();
        System.out.println("Initial error log entries: " + initialErrorLogCount);

        // Create a conversation manager with an invalid API key to trigger an error
        ModelId modelId = new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-20250514");
        Conversation conversation = Conversation.create(modelId, "Error Test");

        // Use an invalid API key
        AnthropicConversationManager manager = new AnthropicConversationManager("invalid-api-key");

        // Create a simple user message
        Message userMessage = new Message(
            MessageRole.USER,
            List.of(new TextBlock(TextBlockFormat.PLAIN, "Test"))
        );

        // Attempt to send the message - this should fail and trigger exception logging
        System.out.println("\nAttempting to send message with invalid API key...");
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            manager.sendMessage(conversation, userMessage);
        });

        System.out.println("Exception caught: " + exception.getClass().getSimpleName());
        System.out.println("Exception message: " + exception.getMessage());

        // Small delay to ensure logs are flushed
        Thread.sleep(500);

        // Verify exception logging occurred
        System.out.println("\n=== Verifying Error Logs ===");

        // Check that error log contains the exception
        long finalErrorLogCount = LogTestHelper.getErrorLogEntryCount();
        System.out.println("Final error log entries: " + finalErrorLogCount);

        assertTrue(finalErrorLogCount > initialErrorLogCount,
            "Error log should have new entries (initial: " + initialErrorLogCount + ", final: " + finalErrorLogCount + ")");

        // Verify the exception was logged from the correct method
        assertTrue(LogTestHelper.errorLogContainsMethodError("AnthropicConversationManager", "sendMessageToVendor"),
            "Error log should contain exception from AnthropicConversationManager.sendMessageToVendor()");

        // Display the error log
        System.out.println("\n=== Error Log Contents ===");
        List<String> errorLog = LogTestHelper.readErrorLog();
        errorLog.forEach(System.out::println);

        System.out.println("\n=== Exception Logging Test Passed! ===");
    }

    @Test
    public void testMultipleApiCallsLogging() throws Exception {
        System.out.println("=== Testing Multiple API Calls Logging ===");

        // Load API configuration
        ApiConfig config = FileApiConfig.fromResource("/api-keys.properties");
        ModelId modelId = new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-20250514");
        Conversation conversation = Conversation.create(modelId, "Multi-call Test");
        ConversationManager<?, ?> manager = ConversationManager.forModel(modelId, config);

        // Get initial log count
        long initialApiLogCount = LogTestHelper.getApiLogEntryCount();

        // Send multiple messages
        int messageCount = 3;
        for (int i = 1; i <= messageCount; i++) {
            System.out.println("\nSending message " + i + " of " + messageCount);
            Message userMessage = new Message(
                MessageRole.USER,
                List.of(new TextBlock(TextBlockFormat.PLAIN, "Count to " + i))
            );
            manager.sendMessage(conversation, userMessage);
        }

        // Small delay to ensure logs are flushed
        Thread.sleep(500);

        // Verify that multiple API calls were logged
        long finalApiLogCount = LogTestHelper.getApiLogEntryCount();
        long newEntries = finalApiLogCount - initialApiLogCount;

        System.out.println("\n=== API Log Summary ===");
        System.out.println("Initial entries: " + initialApiLogCount);
        System.out.println("Final entries: " + finalApiLogCount);
        System.out.println("New entries: " + newEntries);

        // Each API call should generate 2 log entries (request + response)
        long expectedMinEntries = messageCount * 2;
        assertTrue(newEntries >= expectedMinEntries,
            "Should have at least " + expectedMinEntries + " new log entries (request + response for each call), but found " + newEntries);

        System.out.println("\n=== Multiple API Calls Logging Test Passed! ===");
    }
}
