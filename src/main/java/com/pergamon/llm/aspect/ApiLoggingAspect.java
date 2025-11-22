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
 * Intercepts sendMessageToVendor calls across all Conversation implementations.
 */
@Aspect
public class ApiLoggingAspect {
    private static final Logger API_LOGGER = LoggerFactory.getLogger("com.pergamon.llm.api");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Around advice for sendMessageToVendor method.
     * Logs the vendor-specific request before the call and the response after.
     *
     * @param joinPoint the join point representing the method execution
     * @return the vendor response object
     * @throws Throwable if the underlying method throws an exception
     */
    @Around("execution(protected * com.pergamon.llm.conversation.Conversation+.sendMessageToVendor(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get the vendor name from the class name (e.g., "AnthropicConversation" -> "ANTHROPIC")
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String vendorName = className.replace("Conversation", "").toUpperCase();

        // Get the vendor message (first argument to sendMessageToVendor)
        Object[] args = joinPoint.getArgs();
        Object vendorMessage = args.length > 0 ? args[0] : null;

        // Log the request
        if (vendorMessage != null) {
            logRequest(vendorName, vendorMessage);
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
