package com.pergamon.llm.conversation;

import com.pergamon.llm.config.ApiConfig;
import com.pergamon.llm.config.FileApiConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Anthropic prompt caching functionality.
 *
 * This test demonstrates the token savings achieved by using prompt caching
 * with large context (Pride and Prejudice novel text).
 */
public class AnthropicCachingTest {

    @Test
    public void testCachingReducesTokenUsage() throws Exception {
        // Load API configuration
        ApiConfig config = FileApiConfig.fromResource("/api-keys.properties");
        ModelId modelId = new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-20250514");

        // Load Pride and Prejudice text from resources
        Path ppPath = Path.of("src/test/resources/pp.txt");
        String fullText = Files.readString(ppPath);

        // Use only the first ~40% of the text to stay well under the 200k token limit
        // (Full text is ~213k tokens, so first 40% should be around 85k tokens)
        int targetLength = (int) (fullText.length() * 0.4);
        String prideAndPrejudiceText = fullText.substring(0, targetLength);

        System.out.println("\n=== Pride and Prejudice Text Loaded ===");
        System.out.println("Characters: " + prideAndPrejudiceText.length());
        System.out.println("Lines: " + prideAndPrejudiceText.split("\n").length);

        // Define our questions about the novel
        String[] questions = {
            "Who are the main characters in this novel?",
            "What is the relationship between Elizabeth and Mr. Darcy?",
            "What role does Mr. Wickham play in the story?",
            "How does the novel end?"
        };

        // ===== FIRST CONVERSATION: WITHOUT CACHING =====
        System.out.println("\n=== CONVERSATION 1: WITHOUT CACHING ===");

        long startTimeNoCache = System.currentTimeMillis();

        AnthropicConversation conversationWithoutCache =
            (AnthropicConversation) Conversation.forModel(modelId, config);
        conversationWithoutCache.rename("Pride and Prejudice - No Cache");

        // Send the novel as a document block with the first question
        Message firstMessageNoCache = new Message(
            MessageRole.USER,
            List.of(
                new PlainTextDocumentBlock(prideAndPrejudiceText, "text/plain", List.of()),
                new TextBlock(TextBlockFormat.PLAIN, questions[0], List.of())
            )
        );

        Message response1NoCache = conversationWithoutCache.sendMessage(firstMessageNoCache, false);
        System.out.println("\nQuestion 1 (no cache):");
        System.out.println("  Input tokens: " + response1NoCache.inputTokens());
        System.out.println("  Output tokens: " + response1NoCache.outputTokens());

        // Ask follow-up questions (novel text is resent with each API call)
        for (int i = 1; i < questions.length; i++) {
            Message questionMessage = new Message(
                MessageRole.USER,
                List.of(new TextBlock(TextBlockFormat.PLAIN, questions[i], List.of()))
            );

            Message response = conversationWithoutCache.sendMessage(questionMessage, false);
            System.out.println("\nQuestion " + (i + 1) + " (no cache):");
            System.out.println("  Input tokens: " + response.inputTokens());
            System.out.println("  Output tokens: " + response.outputTokens());
        }

        long totalInputTokensNoCache = conversationWithoutCache.getTotalInputTokens();
        long totalOutputTokensNoCache = conversationWithoutCache.getTotalOutputTokens();
        long totalTokensNoCache = totalInputTokensNoCache + totalOutputTokensNoCache;
        long elapsedTimeNoCache = System.currentTimeMillis() - startTimeNoCache;

        System.out.println("\n--- Totals WITHOUT caching ---");
        System.out.println("Total input tokens: " + totalInputTokensNoCache);
        System.out.println("Total output tokens: " + totalOutputTokensNoCache);
        System.out.println("Total tokens: " + totalTokensNoCache);
        System.out.println("Elapsed time: " + elapsedTimeNoCache + " ms (" +
            String.format("%.1f", elapsedTimeNoCache / 1000.0) + " seconds)");

        // ===== SECOND CONVERSATION: WITH CACHING =====
        System.out.println("\n\n=== CONVERSATION 2: WITH CACHING ===");

        long startTimeWithCache = System.currentTimeMillis();

        AnthropicConversation conversationWithCache =
            (AnthropicConversation) Conversation.forModel(modelId, config);
        conversationWithCache.rename("Pride and Prejudice - With Cache");

        // Send the same novel with caching enabled
        Message firstMessageWithCache = new Message(
            MessageRole.USER,
            List.of(
                new PlainTextDocumentBlock(prideAndPrejudiceText, "text/plain", List.of()),
                new TextBlock(TextBlockFormat.PLAIN, questions[0], List.of())
            )
        );

        Message response1WithCache = conversationWithCache.sendMessage(firstMessageWithCache, true);
        System.out.println("\nQuestion 1 (with cache):");
        System.out.println("  Input tokens: " + response1WithCache.inputTokens());
        System.out.println("  Output tokens: " + response1WithCache.outputTokens());
        System.out.println("  Cache creation tokens: " + conversationWithCache.getTotalCacheCreationInputTokens());
        System.out.println("  Cache read tokens: " + conversationWithCache.getTotalCacheReadInputTokens());

        // Ask follow-up questions with caching (novel should be read from cache)
        for (int i = 1; i < questions.length; i++) {
            Message questionMessage = new Message(
                MessageRole.USER,
                List.of(new TextBlock(TextBlockFormat.PLAIN, questions[i], List.of()))
            );

            Message response = conversationWithCache.sendMessage(questionMessage, true);
            System.out.println("\nQuestion " + (i + 1) + " (with cache):");
            System.out.println("  Input tokens: " + response.inputTokens());
            System.out.println("  Output tokens: " + response.outputTokens());
            System.out.println("  Cache creation tokens: " + conversationWithCache.getTotalCacheCreationInputTokens());
            System.out.println("  Cache read tokens: " + conversationWithCache.getTotalCacheReadInputTokens());
        }

        long totalInputTokensWithCache = conversationWithCache.getTotalInputTokens();
        long totalOutputTokensWithCache = conversationWithCache.getTotalOutputTokens();
        long totalCacheCreationTokens = conversationWithCache.getTotalCacheCreationInputTokens();
        long totalCacheReadTokens = conversationWithCache.getTotalCacheReadInputTokens();
        long totalTokensWithCache = totalInputTokensWithCache + totalOutputTokensWithCache;
        long elapsedTimeWithCache = System.currentTimeMillis() - startTimeWithCache;

        System.out.println("\n--- Totals WITH caching ---");
        System.out.println("Total input tokens: " + totalInputTokensWithCache);
        System.out.println("Total output tokens: " + totalOutputTokensWithCache);
        System.out.println("Total cache creation tokens: " + totalCacheCreationTokens);
        System.out.println("Total cache read tokens: " + totalCacheReadTokens);
        System.out.println("Total tokens: " + totalTokensWithCache);
        System.out.println("Elapsed time: " + elapsedTimeWithCache + " ms (" +
            String.format("%.1f", elapsedTimeWithCache / 1000.0) + " seconds)");

        // ===== VERIFY CACHING WORKED =====
        System.out.println("\n\n=== CACHING EFFECTIVENESS ===");

        // Cache creation should happen on the first request
        assertTrue(totalCacheCreationTokens > 0,
            "Cache creation tokens should be > 0 (cache was created on first request)");

        // Cache reads should happen on subsequent requests
        assertTrue(totalCacheReadTokens > 0,
            "Cache read tokens should be > 0 (cache was used on follow-up requests)");

        // Input tokens with caching should be significantly less than without caching
        // This is because the novel text is cached and reused
        assertTrue(totalInputTokensWithCache < totalInputTokensNoCache,
            "Total input tokens with caching should be less than without caching");

        // Calculate token savings
        long tokenSavings = totalInputTokensNoCache - totalInputTokensWithCache;
        double savingsPercentage = (tokenSavings * 100.0) / totalInputTokensNoCache;

        // Calculate time difference
        long timeDifference = elapsedTimeNoCache - elapsedTimeWithCache;
        double timePercentage = (timeDifference * 100.0) / elapsedTimeNoCache;

        System.out.println("Input token savings: " + tokenSavings + " tokens (" +
            String.format("%.1f", savingsPercentage) + "%)");
        System.out.println("Time difference: " + timeDifference + " ms (" +
            String.format("%.1f", timePercentage) + "% " +
            (timeDifference > 0 ? "faster" : "slower") + " with caching)");

        // We expect at least 50% savings with caching for this use case
        assertTrue(savingsPercentage > 50.0,
            "Expected at least 50% token savings with caching, got " +
            String.format("%.1f", savingsPercentage) + "%");

        System.out.println("\n=== Caching test PASSED! ===");
    }
}
