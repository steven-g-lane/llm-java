package com.pergamon.llm.conversation;

/**
 * ConversationManager implementation for Anthropic's Claude models.
 *
 * This class handles the conversion between our generic Message format and
 * Anthropic's SDK message format, and manages communication with the Anthropic API.
 */
public class AnthropicConversationManager
        extends ConversationManager<Object, Object> { // TODO: Replace Object with actual Anthropic SDK types

    private final String apiKey;

    /**
     * Creates an Anthropic conversation manager with the specified API key.
     *
     * @param apiKey the Anthropic API key
     */
    public AnthropicConversationManager(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be null or blank");
        }
        this.apiKey = apiKey;
    }

    @Override
    protected Object toVendorMessage(Message message) {
        // TODO: Convert our Message to com.anthropic.sdk.messages.Message
        // This will need to:
        // 1. Convert MessageRole to Anthropic's role enum
        // 2. Convert MessageBlocks (TextBlock, ImageBlock) to Anthropic's ContentBlock types
        // 3. Handle different image formats (URL vs base64)
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    protected Object sendMessageToVendor(Conversation conversation, Object vendorMessage) {
        // TODO: Implement actual Anthropic API call
        // This will need to:
        // 1. Build the API request with:
        //    - Model ID from conversation.modelId()
        //    - Messages from conversation.messages() (convert entire history)
        //    - Optional: system message, max_tokens, temperature, etc.
        // 2. Set vendor conversation ID if present
        // 3. Make the API call using Anthropic SDK
        // 4. Store vendor conversation ID in conversation if returned
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    protected Message fromVendorResponse(Object vendorResponse) {
        // TODO: Convert Anthropic's MessageResponse to our Message
        // This will need to:
        // 1. Extract the role (should be ASSISTANT)
        // 2. Convert ContentBlocks to our MessageBlocks
        // 3. Handle text blocks, image blocks, tool use, etc.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
