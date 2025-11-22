package com.pergamon.llm.conversation;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.TextBlockParam;
// Note: TextBlock from Anthropic SDK accessed via fully qualified name to avoid conflict with our TextBlock

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
            return client.messages().create(createParams);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send message to Anthropic", e);
        }
    }

    @Override
    protected Message fromVendorResponse(com.anthropic.models.messages.Message vendorResponse) {
        // Convert role from Anthropic to our MessageRole
        // The _role() method returns a JsonValue that should always be "assistant"
        // Handle the raw Optional type from asString()
        String roleString;
        var optionalRole = vendorResponse._role().asString();
        if (optionalRole.isPresent()) {
            roleString = (String) optionalRole.get();
        } else {
            roleString = "assistant";  // Default fallback
        }
        MessageRole role = fromVendorRole(roleString);

        // Convert Anthropic ContentBlocks to our MessageBlocks
        List<MessageBlock> blocks = new ArrayList<>();
        for (ContentBlock contentBlock : vendorResponse.content()) {
            blocks.add(fromVendorContentBlock(contentBlock));
        }

        return new Message(role, blocks);
    }

    /**
     * Converts Anthropic's role string to our MessageRole.
     *
     * @param roleString the Anthropic role string
     * @return our MessageRole
     * @throws UnsupportedOperationException for unsupported roles
     */
    protected MessageRole fromVendorRole(String roleString) {
        return switch (roleString) {
            case "assistant" -> MessageRole.ASSISTANT;
            case "user" -> MessageRole.USER;
            default -> throw new UnsupportedOperationException("Unsupported Anthropic role: " + roleString);
        };
    }

    /**
     * Converts Anthropic's ContentBlock to our MessageBlock.
     * Uses the Optional getters to detect which type of block this is.
     *
     * @param contentBlock Anthropic's ContentBlock
     * @return our MessageBlock
     */
    protected MessageBlock fromVendorContentBlock(ContentBlock contentBlock) {
        // Check if it's a text block
        if (contentBlock.text().isPresent()) {
            return fromVendorTextBlock(contentBlock.text().get());
        }

        // If we don't recognize the block type, wrap it as UnknownBlock
        return new UnknownBlock(contentBlock.toString());
    }

    /**
     * Converts Anthropic's TextBlock to our TextBlock.
     * Uses ConversationUtils to programmatically detect the text format.
     *
     * @param anthropicTextBlock Anthropic's TextBlock
     * @return our TextBlock
     */
    protected MessageBlock fromVendorTextBlock(com.anthropic.models.messages.TextBlock anthropicTextBlock) {
        String text = anthropicTextBlock.text();
        TextBlockFormat format = ConversationUtils.detectTextFormat(text);
        return new TextBlock(format, text);
    }
}
