package com.pergamon.llm.conversation;

import com.pergamon.llm.config.ApiConfig;

/**
 * Abstract base class for managing conversations with different LLM vendors.
 *
 * This class implements the Template Method pattern to handle the common flow of:
 * 1. Converting our Message to vendor-specific format
 * 2. Sending to the vendor API
 * 3. Converting the vendor response back to our Message format
 * 4. Automatically appending messages to the conversation
 *
 * @param <V> The vendor-specific message type (e.g., com.anthropic.sdk.messages.Message)
 * @param <R> The vendor-specific response type (e.g., com.anthropic.sdk.messages.MessageResponse)
 */
public abstract class ConversationManager<V, R> {

    /**
     * Sends a message in the conversation and returns the response.
     *
     * This method implements the template pattern:
     * 1. Appends the user message to the conversation
     * 2. Converts to vendor-specific format
     * 3. Calls the vendor API
     * 4. Converts response back to our Message format
     * 5. Appends the response to the conversation
     *
     * @param conversation the conversation to send the message in
     * @param message the message to send
     * @return the response message from the LLM
     */
    public Message sendMessage(Conversation conversation, Message message) {
        // 1. Append the user message to the conversation
        conversation.append(message);

        // 2. Convert our Message to vendor-specific format
        V vendorMessage = toVendorMessage(message);

        // 3. Call vendor-specific send implementation
        R vendorResponse = sendMessageToVendor(conversation, vendorMessage);

        // 4. Convert vendor response back to our Message format
        Message responseMessage = fromVendorResponse(vendorResponse);

        // 5. Append the response to the conversation
        conversation.append(responseMessage);

        return responseMessage;
    }

    /**
     * Converts our generic Message to the vendor-specific message format.
     * Subclasses implement this to handle vendor-specific message structure.
     *
     * @param message our generic message
     * @return the vendor-specific message object
     */
    protected abstract V toVendorMessage(Message message);

    /**
     * Sends the vendor-specific message to the vendor API and returns the response.
     * This is where the actual API call happens.
     *
     * @param conversation the conversation context (may contain history, vendor IDs, etc.)
     * @param vendorMessage the vendor-specific message to send
     * @return the vendor-specific response
     */
    protected abstract R sendMessageToVendor(Conversation conversation, V vendorMessage);

    /**
     * Converts a vendor-specific response back to our generic Message format.
     *
     * @param vendorResponse the vendor-specific response
     * @return our generic message
     */
    protected abstract Message fromVendorResponse(R vendorResponse);

    /**
     * Factory method to create a ConversationManager for a specific model.
     *
     * @param modelId the model to create a manager for
     * @param config the API configuration containing vendor API keys
     * @return a ConversationManager instance for the model's vendor
     * @throws IllegalArgumentException if the vendor is not supported
     */
    public static ConversationManager<?, ?> forModel(ModelId modelId, ApiConfig config) {
        return switch (modelId.vendor()) {
            case ANTHROPIC -> createAnthropicManager(config);
            case OPENAI -> createOpenAIManager(config);
            case GOOGLE -> createGoogleManager(config);
        };
    }

    private static ConversationManager<?, ?> createAnthropicManager(ApiConfig config) {
        String apiKey = config.getApiKeyOrThrow(Vendor.ANTHROPIC);
        return new AnthropicConversationManager(apiKey);
    }

    private static ConversationManager<?, ?> createOpenAIManager(ApiConfig config) {
        // TODO: Implement OpenAI manager
        throw new UnsupportedOperationException("OpenAI manager not yet implemented");
    }

    private static ConversationManager<?, ?> createGoogleManager(ApiConfig config) {
        // TODO: Implement Google manager
        throw new UnsupportedOperationException("Google manager not yet implemented");
    }
}
