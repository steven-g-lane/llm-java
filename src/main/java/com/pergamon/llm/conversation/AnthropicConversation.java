package com.pergamon.llm.conversation;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.UrlImageSource;
// Note: TextBlock from Anthropic SDK accessed via fully qualified name to avoid conflict with our TextBlock

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Conversation implementation for Anthropic's Claude models.
 *
 * This class handles the conversion between our generic Message format and
 * Anthropic's SDK message format, and manages communication with the Anthropic API.
 *
 * Each API call sends the full conversation history (all messages from vendorMessages),
 * enabling Claude to maintain context across multiple turns in the conversation.
 *
 * Type parameters:
 * - V: MessageParam (the vendor-specific message type)
 * - R: com.anthropic.models.messages.Message (the response type, disambiguated from our Message class)
 */
public class AnthropicConversation
        extends Conversation<MessageParam, com.anthropic.models.messages.Message> {

    private static final int MAX_TOKENS = 4096;

    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".webp"
    );

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp"
    );

    private final String apiKey;
    private final AnthropicClient client;

    /**
     * Creates an Anthropic conversation with the specified model ID and API key.
     * Instantiates one AnthropicOkHttpClient per conversation instance.
     *
     * @param modelId the model ID
     * @param apiKey the Anthropic API key
     */
    public AnthropicConversation(ModelId modelId, String apiKey) {
        super(modelId);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be null or blank");
        }
        this.apiKey = apiKey;
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * Creates an Anthropic conversation with the specified model ID, name, and API key.
     *
     * @param modelId the model ID
     * @param name the conversation name
     * @param apiKey the Anthropic API key
     */
    public AnthropicConversation(ModelId modelId, String name, String apiKey) {
        super(modelId, name);
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
     * @throws UnsupportedOperationException for UnknownBlock
     */
    protected ContentBlockParam toVendorMessageBlock(MessageBlock block) {
        return switch (block) {
            case TextBlock textBlock -> toVendorTextBlock(textBlock);
            case ImageBlock imageBlock -> toVendorImageBlock(imageBlock);
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

    /**
     * Converts our ImageBlock to Anthropic's ContentBlockParam.
     * Handles URL, Base64, and file path image sources.
     *
     * @param imageBlock our image block
     * @return Anthropic ContentBlockParam wrapping an ImageBlockParam
     */
    protected ContentBlockParam toVendorImageBlock(ImageBlock imageBlock) {
        return switch (imageBlock) {
            case URLImageBlock urlImageBlock -> toVendorUrlImageBlock(urlImageBlock);
            case Base64ImageBlock base64ImageBlock -> toVendorBase64ImageBlock(base64ImageBlock);
            case FilePathImageBlock filePathImageBlock -> toVendorFilePathImageBlock(filePathImageBlock);
            default -> throw new UnsupportedOperationException(
                    "Unsupported image block type: " + imageBlock.getClass().getName());
        };
    }

    /**
     * Converts URLImageBlock to Anthropic's ImageBlockParam with UrlImageSource.
     *
     * @param urlImageBlock our URL image block
     * @return Anthropic ContentBlockParam wrapping an ImageBlockParam
     */
    protected ContentBlockParam toVendorUrlImageBlock(URLImageBlock urlImageBlock) {
        ConversationUtils.validateUri(urlImageBlock.uri());
        ConversationUtils.validateMimeType(urlImageBlock.mimeType(), SUPPORTED_MIME_TYPES);

        UrlImageSource urlSource = UrlImageSource.builder()
                .url(urlImageBlock.uri().toString())
                .build();

        ImageBlockParam imageBlockParam = ImageBlockParam.builder()
                .source(urlSource)
                .build();

        return ContentBlockParam.ofImage(imageBlockParam);
    }

    /**
     * Converts Base64ImageBlock to Anthropic's ImageBlockParam with Base64ImageSource.
     *
     * @param base64ImageBlock our base64 image block
     * @return Anthropic ContentBlockParam wrapping an ImageBlockParam
     */
    protected ContentBlockParam toVendorBase64ImageBlock(Base64ImageBlock base64ImageBlock) {
        ConversationUtils.validateBase64Data(base64ImageBlock.base64Data());
        ConversationUtils.validateMimeType(base64ImageBlock.mimeType(), SUPPORTED_MIME_TYPES);

        Base64ImageSource.MediaType mediaType = mapToAnthropicMediaType(base64ImageBlock.mimeType());

        Base64ImageSource base64Source = Base64ImageSource.builder()
                .data(base64ImageBlock.base64Data())
                .mediaType(mediaType)
                .build();

        ImageBlockParam imageBlockParam = ImageBlockParam.builder()
                .source(base64Source)
                .build();

        return ContentBlockParam.ofImage(imageBlockParam);
    }

    /**
     * Converts FilePathImageBlock to Anthropic's ImageBlockParam with Base64ImageSource.
     * Reads the file, encodes it to base64, and creates a Base64ImageSource.
     *
     * @param filePathImageBlock our file path image block
     * @return Anthropic ContentBlockParam wrapping an ImageBlockParam
     */
    protected ContentBlockParam toVendorFilePathImageBlock(FilePathImageBlock filePathImageBlock) {
        ConversationUtils.validateFilePath(filePathImageBlock.filePath(), SUPPORTED_IMAGE_EXTENSIONS);
        ConversationUtils.validateMimeType(filePathImageBlock.mimeType(), SUPPORTED_MIME_TYPES);

        try {
            Path path = Path.of(filePathImageBlock.filePath());
            byte[] fileBytes = Files.readAllBytes(path);
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);

            Base64ImageSource.MediaType mediaType = mapToAnthropicMediaType(filePathImageBlock.mimeType());

            Base64ImageSource base64Source = Base64ImageSource.builder()
                    .data(base64Data)
                    .mediaType(mediaType)
                    .build();

            ImageBlockParam imageBlockParam = ImageBlockParam.builder()
                    .source(base64Source)
                    .build();

            return ContentBlockParam.ofImage(imageBlockParam);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image file: " + filePathImageBlock.filePath(), e);
        }
    }

    @Override
    protected com.anthropic.models.messages.Message sendConversationToVendor() {
        try {
            // Get model ID
            String model = modelId().apiModelName();

            // Build MessageCreateParams with model, maxTokens, and all messages in history
            MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(MAX_TOKENS);

            // Add all vendor messages (the entire conversation history)
            for (MessageParam msg : vendorMessages) {
                paramsBuilder.addMessage(msg);
            }

            // Make the synchronous (non-streaming) API call with full conversation context
            return client.messages().create(paramsBuilder.build());

        } catch (Exception e) {
            throw new RuntimeException("Failed to send conversation to Anthropic", e);
        }
    }

    @Override
    protected MessageParam vendorResponseToVendorMessage(com.anthropic.models.messages.Message vendorResponse) {
        // Convert the response ContentBlocks to ContentBlockParams for storage in history
        // The response role is always "assistant"
        List<ContentBlockParam> contentBlockParams = new ArrayList<>();

        for (ContentBlock contentBlock : vendorResponse.content()) {
            // Convert each ContentBlock to a ContentBlockParam
            if (contentBlock.text().isPresent()) {
                // It's a text block
                com.anthropic.models.messages.TextBlock textBlock = contentBlock.text().get();
                TextBlockParam textBlockParam = TextBlockParam.builder()
                        .text(textBlock.text())
                        .build();
                contentBlockParams.add(ContentBlockParam.ofText(textBlockParam));
            }
            // Add support for other block types (image, etc.) as needed
        }

        return MessageParam.builder()
                .role(MessageParam.Role.ASSISTANT)
                .content(MessageParam.Content.ofBlockParams(contentBlockParams))
                .build();
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

    /**
     * Maps our MIME type strings to Anthropic's MediaType enum.
     * This is Anthropic-specific and stays in this class.
     *
     * @param mimeType the MIME type string
     * @return the corresponding Anthropic MediaType
     * @throws IllegalArgumentException if the MIME type is not supported
     */
    private static Base64ImageSource.MediaType mapToAnthropicMediaType(String mimeType) {
        return switch (mimeType.toLowerCase()) {
            case "image/png" -> Base64ImageSource.MediaType.IMAGE_PNG;
            case "image/jpeg", "image/jpg" -> Base64ImageSource.MediaType.IMAGE_JPEG;
            case "image/gif" -> Base64ImageSource.MediaType.IMAGE_GIF;
            case "image/webp" -> Base64ImageSource.MediaType.IMAGE_WEBP;
            default -> throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
        };
    }
}
