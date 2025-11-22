package com.pergamon.llm.conversation;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.TextBlockParam;

import java.util.ArrayList;
import java.util.List;

/**
 * ConversationManager implementation for Anthropic's Claude models.
 *
 * This class handles the conversion between our generic Message format and
 * Anthropic's SDK message format, and manages communication with the Anthropic API.
 *
 * Type parameters:
 * - V: MessageParam (the vendor-specific message type)
 * - R: com.anthropic.models.messages.Message (the response type, disambiguated from our Message class)
 */
public class AnthropicConversationManager
        extends ConversationManager<MessageParam, com.anthropic.models.messages.Message> {

    private static final int MAX_TOKENS = 4096;

    private final String apiKey;
    private final AnthropicClient client;

    /**
     * Creates an Anthropic conversation manager with the specified API key.
     * Instantiates one AnthropicOkHttpClient per manager instance.
     *
     * @param apiKey the Anthropic API key
     */
    public AnthropicConversationManager(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be null or blank");
        }
        this.apiKey = apiKey;
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    protected MessageParam toVendorMessage(Message message) {
        // Convert role using Anthropic's Role constants
        MessageParam.Role role = switch (message.role()) {
            case USER -> MessageParam.Role.USER;
            case ASSISTANT -> MessageParam.Role.ASSISTANT;
            default -> throw new UnsupportedOperationException(
                    "Unsupported role: " + message.role());
        };

        // Convert our MessageBlocks to Anthropic ContentBlockParams
        List<ContentBlockParam> contentBlocks = new ArrayList<>();
        for (MessageBlock block : message.blocks()) {
            contentBlocks.add(toVendorMessageBlock(block));
        }

        return MessageParam.builder()
                .role(role)
                .content(MessageParam.Content.ofBlockParams(contentBlocks))
                .build();
    }

    /**
     * Converts our MessageBlock to Anthropic's ContentBlockParam.
     * Uses pattern matching to safely handle different block types.
     *
     * @param block our message block
     * @return Anthropic ContentBlockParam
     * @throws UnsupportedOperationException for non-TextBlock types
     */
    protected ContentBlockParam toVendorMessageBlock(MessageBlock block) {
        return switch (block) {
            case TextBlock textBlock -> toVendorTextBlock(textBlock);
            case ImageBlock imageBlock -> throw new UnsupportedOperationException(
                    "ImageBlock conversion not yet implemented");
            case UnknownBlock unknownBlock -> throw new UnsupportedOperationException(
                    "UnknownBlock cannot be converted");
            default -> throw new UnsupportedOperationException(
                    "Unsupported block type: " + block.getClass().getName());
        };
    }

    /**
     * Converts our TextBlock to Anthropic's ContentBlockParam.
     *
     * @param textBlock our text block
     * @return Anthropic ContentBlockParam wrapping a TextBlockParam
     */
    protected ContentBlockParam toVendorTextBlock(TextBlock textBlock) {
        TextBlockParam textBlockParam = TextBlockParam.builder()
                .text(textBlock.text())
                .build();
        return ContentBlockParam.ofText(textBlockParam);
    }

    @Override
    protected com.anthropic.models.messages.Message sendMessageToVendor(
            Conversation conversation,
            MessageParam vendorMessage) {
        try {
            // Get model ID from conversation
            String model = conversation.modelId().apiModelName();

            // Build MessageCreateParams with model, maxTokens, and the message
            MessageCreateParams createParams = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(MAX_TOKENS)
                    .addMessage(vendorMessage)
                    .build();

            // Make the synchronous (non-streaming) API call
            com.anthropic.models.messages.Message response = client.messages().create(createParams);

            // Debug: Print the raw response
            System.out.println("=== Raw Anthropic Response ===");
            System.out.println("ID: " + response.id());
            System.out.println("Model: " + response.model());
            System.out.println("Stop Reason: " + response.stopReason());
            System.out.println("Usage: " + response.usage());
            System.out.println("Content blocks: " + response.content().size());
            System.out.println();

            response.content().forEach(block -> {
                System.out.println("Content Block:");
                System.out.println("  toString: " + block);
                block.text().ifPresent(text -> System.out.println("  Text: " + text.text()));
            });
            System.out.println();

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send message to Anthropic", e);
        }
    }

    @Override
    protected Message fromVendorResponse(com.anthropic.models.messages.Message vendorResponse) {
        // TODO: Convert Anthropic's Message to our Message
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
