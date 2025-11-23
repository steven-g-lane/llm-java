package com.pergamon.llm.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aspect that logs all LLM API requests and responses.
 * Intercepts sendConversationToVendor calls across all Conversation implementations.
 */
@Aspect
public class ApiLoggingAspect {
    private static final Logger API_LOGGER = LoggerFactory.getLogger("com.pergamon.llm.api");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Around advice for sendConversationToVendor method.
     * Logs the most recent user message before the call and the response after.
     * Note: The full conversation context is sent with each call.
     *
     * @param joinPoint the join point representing the method execution
     * @return the vendor response object
     * @throws Throwable if the underlying method throws an exception
     */
    @Around("execution(protected * com.pergamon.llm.conversation.Conversation+.sendConversationToVendor(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get the vendor name from the class name (e.g., "AnthropicConversation" -> "ANTHROPIC")
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String vendorName = className.replace("Conversation", "").toUpperCase();

        // Get the Conversation instance to access vendorMessages
        Object target = joinPoint.getTarget();

        // Use reflection to access the vendorMessages list and get the most recent message
        try {
            java.lang.reflect.Field vendorMessagesField = target.getClass().getSuperclass().getDeclaredField("vendorMessages");
            vendorMessagesField.setAccessible(true);
            Object vendorMessagesObj = vendorMessagesField.get(target);

            if (vendorMessagesObj instanceof java.util.List<?> vendorMessages) {
                // Log the most recent message (the one that triggered this API call)
                if (!vendorMessages.isEmpty()) {
                    Object mostRecentMessage = vendorMessages.get(vendorMessages.size() - 1);
                    logRequest(vendorName, mostRecentMessage);
                }
            }
        } catch (Exception e) {
            // If we can't access vendorMessages, just log that we're sending
            API_LOGGER.info("[{}] Sending conversation to vendor API", vendorName);
        }

        // Proceed with the actual API call
        Object response = joinPoint.proceed();

        // Log the response
        if (response != null) {
            logResponse(vendorName, response);
        }

        return response;
    }

    /**
     * Logs an LLM API request.
     */
    private void logRequest(String vendorName, Object request) {
        try {
            String serialized = OBJECT_MAPPER.writeValueAsString(request);
            API_LOGGER.info("[{}] Request: {}", vendorName, serialized);
        } catch (JsonProcessingException e) {
            API_LOGGER.error("[{}] Failed to serialize request: {}", vendorName, e.getMessage());
        }
    }

    /**
     * Logs an LLM API response.
     */
    private void logResponse(String vendorName, Object response) {
        try {
            String serialized = OBJECT_MAPPER.writeValueAsString(response);
            API_LOGGER.info("[{}] Response: {}", vendorName, serialized);
        } catch (JsonProcessingException e) {
            API_LOGGER.error("[{}] Failed to serialize response: {}", vendorName, e.getMessage());
        }
    }
}
