package com.pergamon.llm.conversation;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.CitationCharLocation;
import com.anthropic.models.messages.CitationCharLocationParam;
import com.anthropic.models.messages.CitationContentBlockLocation;
import com.anthropic.models.messages.CitationContentBlockLocationParam;
import com.anthropic.models.messages.CitationPageLocation;
import com.anthropic.models.messages.CitationPageLocationParam;
import com.anthropic.models.messages.CitationSearchResultLocationParam;
import com.anthropic.models.messages.CitationWebSearchResultLocationParam;
import com.anthropic.models.messages.CitationsConfigParam;
import com.anthropic.models.messages.CitationsSearchResultLocation;
import com.anthropic.models.messages.CitationsWebSearchResultLocation;
import com.anthropic.models.messages.TextCitationParam;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ToolUnion;
import com.anthropic.models.messages.UrlImageSource;
import com.anthropic.models.messages.WebSearchTool20250305;
// Note: TextBlock from Anthropic SDK accessed via fully qualified name to avoid conflict with our TextBlock

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
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
    private static final boolean CACHING_AVAILABLE = true;

    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".webp"
    );

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp"
    );

    private static final Set<String> SUPPORTED_DOCUMENT_EXTENSIONS = Set.of(".pdf");

    private static final Set<String> SUPPORTED_DOCUMENT_MIME_TYPES = Set.of(
            "application/pdf", "text/plain"
    );

    private final String apiKey;
    private final AnthropicClient client;

    /**
     * Cumulative cache creation input tokens across all messages in the conversation.
     * These represent tokens used when creating new cache entries.
     */
    private long totalCacheCreationInputTokens = 0L;

    /**
     * Cumulative cache read input tokens across all messages in the conversation.
     * These represent tokens read from existing cache entries (at reduced cost).
     */
    private long totalCacheReadInputTokens = 0L;

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
            case DocumentBlock documentBlock -> toVendorDocumentBlock(documentBlock);
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

    /**
     * Converts our DocumentBlock to Anthropic's ContentBlockParam.
     * Handles PDF (URL, Base64, file path) and plain text document sources.
     *
     * @param documentBlock our document block
     * @return Anthropic ContentBlockParam wrapping a DocumentBlockParam
     */
    protected ContentBlockParam toVendorDocumentBlock(DocumentBlock documentBlock) {
        return switch (documentBlock) {
            case URLPDFDocumentBlock urlPdfDocumentBlock -> toVendorUrlPdfDocumentBlock(urlPdfDocumentBlock);
            case Base64PDFDocumentBlock base64PdfDocumentBlock -> toVendorBase64PdfDocumentBlock(base64PdfDocumentBlock);
            case FilePathPDFDocumentBlock filePathPdfDocumentBlock -> toVendorFilePathPdfDocumentBlock(filePathPdfDocumentBlock);
            case PlainTextDocumentBlock plainTextDocumentBlock -> toVendorPlainTextDocumentBlock(plainTextDocumentBlock);
            default -> throw new UnsupportedOperationException(
                    "Unsupported document block type: " + documentBlock.getClass().getName());
        };
    }

    /**
     * Converts URLPDFDocumentBlock to Anthropic's DocumentBlockParam with URL source.
     *
     * @param urlPdfDocumentBlock our URL PDF document block
     * @return Anthropic ContentBlockParam wrapping a DocumentBlockParam
     */
    protected ContentBlockParam toVendorUrlPdfDocumentBlock(URLPDFDocumentBlock urlPdfDocumentBlock) {
        ConversationUtils.validateUri(urlPdfDocumentBlock.uri());
        ConversationUtils.validateMimeType(urlPdfDocumentBlock.mimeType(), SUPPORTED_DOCUMENT_MIME_TYPES);

        // Enable citations for document blocks
        CitationsConfigParam citationsConfig = CitationsConfigParam.builder()
                .enabled(true)
                .build();

        DocumentBlockParam documentBlockParam = DocumentBlockParam.builder()
                .urlSource(urlPdfDocumentBlock.uri().toString())
                .citations(citationsConfig)
                .build();

        return ContentBlockParam.ofDocument(documentBlockParam);
    }

    /**
     * Converts Base64PDFDocumentBlock to Anthropic's DocumentBlockParam with base64 source.
     *
     * @param base64PdfDocumentBlock our base64 PDF document block
     * @return Anthropic ContentBlockParam wrapping a DocumentBlockParam
     */
    protected ContentBlockParam toVendorBase64PdfDocumentBlock(Base64PDFDocumentBlock base64PdfDocumentBlock) {
        ConversationUtils.validateBase64Data(base64PdfDocumentBlock.base64Data());
        ConversationUtils.validateMimeType(base64PdfDocumentBlock.mimeType(), SUPPORTED_DOCUMENT_MIME_TYPES);

        // Enable citations for document blocks
        CitationsConfigParam citationsConfig = CitationsConfigParam.builder()
                .enabled(true)
                .build();

        DocumentBlockParam documentBlockParam = DocumentBlockParam.builder()
                .base64Source(base64PdfDocumentBlock.base64Data())
                .citations(citationsConfig)
                .build();

        return ContentBlockParam.ofDocument(documentBlockParam);
    }

    /**
     * Converts FilePathPDFDocumentBlock to Anthropic's DocumentBlockParam with base64 source.
     * Reads the PDF file, encodes it to base64, and creates a base64 source.
     *
     * @param filePathPdfDocumentBlock our file path PDF document block
     * @return Anthropic ContentBlockParam wrapping a DocumentBlockParam
     */
    protected ContentBlockParam toVendorFilePathPdfDocumentBlock(FilePathPDFDocumentBlock filePathPdfDocumentBlock) {
        ConversationUtils.validateFilePath(filePathPdfDocumentBlock.filePath(), SUPPORTED_DOCUMENT_EXTENSIONS);
        ConversationUtils.validateMimeType(filePathPdfDocumentBlock.mimeType(), SUPPORTED_DOCUMENT_MIME_TYPES);

        try {
            Path path = Path.of(filePathPdfDocumentBlock.filePath());
            byte[] fileBytes = Files.readAllBytes(path);
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);

            // Enable citations for document blocks
            CitationsConfigParam citationsConfig = CitationsConfigParam.builder()
                    .enabled(true)
                    .build();

            DocumentBlockParam documentBlockParam = DocumentBlockParam.builder()
                    .base64Source(base64Data)
                    .citations(citationsConfig)
                    .build();

            return ContentBlockParam.ofDocument(documentBlockParam);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read PDF file: " + filePathPdfDocumentBlock.filePath(), e);
        }
    }

    /**
     * Converts PlainTextDocumentBlock to Anthropic's DocumentBlockParam with text source.
     *
     * @param plainTextDocumentBlock our plain text document block
     * @return Anthropic ContentBlockParam wrapping a DocumentBlockParam
     */
    protected ContentBlockParam toVendorPlainTextDocumentBlock(PlainTextDocumentBlock plainTextDocumentBlock) {
        ConversationUtils.validatePlainText(plainTextDocumentBlock.text());
        ConversationUtils.validateMimeType(plainTextDocumentBlock.mimeType(), SUPPORTED_DOCUMENT_MIME_TYPES);

        // Enable citations for document blocks
        CitationsConfigParam citationsConfig = CitationsConfigParam.builder()
                .enabled(true)
                .build();

        DocumentBlockParam documentBlockParam = DocumentBlockParam.builder()
                .textSource(plainTextDocumentBlock.text())
                .citations(citationsConfig)
                .build();

        return ContentBlockParam.ofDocument(documentBlockParam);
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

            // Enable web search for all requests
            WebSearchTool20250305 webSearchTool = WebSearchTool20250305.builder()
                    .maxUses(5L)
                    .build();
            paramsBuilder.addTool(ToolUnion.ofWebSearchTool20250305(webSearchTool));

            // Add all vendor messages (the entire conversation history)
            for (MessageParam msg : vendorMessages) {
                paramsBuilder.addMessage(msg);
            }

            // Build the final params
            MessageCreateParams params = paramsBuilder.build();

            // Log the full payload being sent (including all conversation history)
            System.out.println("\n=== FULL API REQUEST PAYLOAD ===");
            System.out.println("Model: " + params.model());
            System.out.println("Max Tokens: " + params.maxTokens());
            System.out.println("Tools: " + params.tools());
            System.out.println("Message Count: " + params.messages().size());
            for (int i = 0; i < params.messages().size(); i++) {
                MessageParam msg = params.messages().get(i);
                System.out.println("\n--- Message " + i + " (Role: " + msg.role() + ") ---");
                System.out.println(msg.toString());
            }
            System.out.println("=== END PAYLOAD ===\n");

            // Make the synchronous (non-streaming) API call with full conversation context
            return client.messages().create(params);

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
            // Use the built-in toParam() method which handles all block types including:
            // - TextBlock (with citations)
            // - ServerToolUseBlock (web search tool invocations)
            // - WebSearchToolResultBlock (search results that citations reference)
            // - ThinkingBlock, ToolUseBlock, etc.
            // This ensures web search results are preserved in conversation history so citations work
            contentBlockParams.add(contentBlock.toParam());
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

        // Extract token usage from the vendor response
        long inputTokens = vendorResponse.usage().inputTokens();
        long outputTokens = vendorResponse.usage().outputTokens();

        // Extract cache token usage if present
        long cacheCreationTokens = vendorResponse.usage().cacheCreationInputTokens().orElse(0L);
        long cacheReadTokens = vendorResponse.usage().cacheReadInputTokens().orElse(0L);

        // Accumulate cache tokens
        totalCacheCreationInputTokens += cacheCreationTokens;
        totalCacheReadInputTokens += cacheReadTokens;

        return new Message(role, blocks, inputTokens, outputTokens);
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

        // Convert citations if present
        List<TextCitation> citations = new ArrayList<>();
        if (anthropicTextBlock.citations().isPresent()) {
            for (var anthropicCitation : anthropicTextBlock.citations().get()) {
                citations.add(fromVendorCitation(anthropicCitation));
            }
        }

        return new TextBlock(format, text, citations);
    }

    /**
     * Converts Anthropic's TextCitation to our TextCitation.
     * The Anthropic SDK's TextCitation has optional fields for each citation type;
     * exactly one will be present.
     *
     * @param anthropicCitation Anthropic's TextCitation
     * @return our TextCitation
     */
    protected TextCitation fromVendorCitation(com.anthropic.models.messages.TextCitation anthropicCitation) {
        // Check for char location citation
        if (anthropicCitation.charLocation().isPresent()) {
            CitationCharLocation charLoc = anthropicCitation.charLocation().get();
            return new CharLocationCitation(
                charLoc.citedText(),
                charLoc.documentTitle().orElse("Untitled"),
                "char_location",
                Optional.of(charLoc.documentIndex()),
                charLoc.documentTitle(),
                charLoc.fileId(),
                Optional.of(charLoc.startCharIndex()),
                Optional.of(charLoc.endCharIndex())
            );
        }

        // Check for page location citation
        if (anthropicCitation.pageLocation().isPresent()) {
            CitationPageLocation pageLoc = anthropicCitation.pageLocation().get();
            return new PageLocationCitation(
                pageLoc.citedText(),
                pageLoc.documentTitle().orElse("Untitled"),
                "page_location",
                Optional.of(pageLoc.documentIndex()),
                pageLoc.documentTitle(),
                pageLoc.fileId(),
                Optional.of(pageLoc.startPageNumber()),
                Optional.of(pageLoc.endPageNumber())
            );
        }

        // Check for content block location citation
        if (anthropicCitation.contentBlockLocation().isPresent()) {
            CitationContentBlockLocation blockLoc = anthropicCitation.contentBlockLocation().get();
            return new ContentBlockLocationCitation(
                blockLoc.citedText(),
                blockLoc.documentTitle().orElse("Untitled"),
                "content_block_location",
                Optional.of(blockLoc.documentIndex()),
                blockLoc.documentTitle(),
                blockLoc.fileId(),
                Optional.of(blockLoc.startBlockIndex()),
                Optional.of(blockLoc.endBlockIndex())
            );
        }

        // Check for web search result location citation
        if (anthropicCitation.webSearchResultLocation().isPresent()) {
            CitationsWebSearchResultLocation webLoc = anthropicCitation.webSearchResultLocation().get();
            return new WebSearchResultCitation(
                webLoc.citedText(),
                webLoc.title().orElse("Untitled"),
                "web_search_result_location",
                Optional.of(webLoc.url()),
                Optional.of(webLoc.encryptedIndex())
            );
        }

        // Check for search result location citation
        if (anthropicCitation.searchResultLocation().isPresent()) {
            CitationsSearchResultLocation searchLoc = anthropicCitation.searchResultLocation().get();
            return new SearchResultCitation(
                searchLoc.citedText(),
                searchLoc.title().orElse("Untitled"),
                "search_result_location",
                Optional.of(searchLoc.source()),
                Optional.of(searchLoc.startBlockIndex()),
                Optional.of(searchLoc.endBlockIndex()),
                Optional.of(searchLoc.searchResultIndex())
            );
        }

        // Unknown citation type - store raw data for future compatibility
        return new UnknownCitation(
            "Unknown citation",
            "Unknown",
            "unknown",
            anthropicCitation.toString()
        );
    }

    /**
     * Converts our TextCitation to Anthropic's TextCitationParam for storing in conversation history.
     * This is the reverse operation of fromVendorCitation().
     *
     * @param citation our TextCitation
     * @return Anthropic's TextCitationParam
     */
    protected TextCitationParam toVendorCitation(TextCitation citation) {
        return switch (citation) {
            case CharLocationCitation charCitation -> {
                CitationCharLocationParam param = CitationCharLocationParam.builder()
                        .citedText(charCitation.citedText())
                        .documentIndex(charCitation.documentIndex().orElseThrow())
                        .startCharIndex(charCitation.startCharIndex().orElseThrow())
                        .endCharIndex(charCitation.endCharIndex().orElseThrow())
                        .documentTitle(charCitation.documentTitle().orElse(null))
                        .build();
                yield TextCitationParam.ofCharLocation(param);
            }
            case PageLocationCitation pageCitation -> {
                CitationPageLocationParam param = CitationPageLocationParam.builder()
                        .citedText(pageCitation.citedText())
                        .documentIndex(pageCitation.documentIndex().orElseThrow())
                        .startPageNumber(pageCitation.startPageNumber().orElseThrow())
                        .endPageNumber(pageCitation.endPageNumber().orElseThrow())
                        .documentTitle(pageCitation.documentTitle().orElse(null))
                        .build();
                yield TextCitationParam.ofPageLocation(param);
            }
            case ContentBlockLocationCitation blockCitation -> {
                CitationContentBlockLocationParam param = CitationContentBlockLocationParam.builder()
                        .citedText(blockCitation.citedText())
                        .documentIndex(blockCitation.documentIndex().orElseThrow())
                        .startBlockIndex(blockCitation.startBlockIndex().orElseThrow())
                        .endBlockIndex(blockCitation.endBlockIndex().orElseThrow())
                        .documentTitle(blockCitation.documentTitle().orElse(null))
                        .build();
                yield TextCitationParam.ofContentBlockLocation(param);
            }
            case WebSearchResultCitation webCitation -> {
                CitationWebSearchResultLocationParam param = CitationWebSearchResultLocationParam.builder()
                        .citedText(webCitation.citedText())
                        .url(webCitation.url().orElseThrow())
                        .encryptedIndex(webCitation.encryptedIndex().orElseThrow())
                        .title(webCitation.title())
                        .build();
                yield TextCitationParam.ofWebSearchResultLocation(param);
            }
            case SearchResultCitation searchCitation -> {
                CitationSearchResultLocationParam param = CitationSearchResultLocationParam.builder()
                        .citedText(searchCitation.citedText())
                        .source(searchCitation.source().orElseThrow())
                        .startBlockIndex(searchCitation.startBlockIndex().orElseThrow())
                        .endBlockIndex(searchCitation.endBlockIndex().orElseThrow())
                        .searchResultIndex(searchCitation.searchResultIndex().orElseThrow())
                        .title(searchCitation.title())
                        .build();
                yield TextCitationParam.ofSearchResultLocation(param);
            }
            case UnknownCitation unknownCitation ->
                // Cannot convert unknown citations back to params - skip them
                // This should be rare and only happens with new citation types we don't support yet
                throw new UnsupportedOperationException(
                    "Cannot convert UnknownCitation to TextCitationParam: " + unknownCitation.rawData());
            default ->
                throw new UnsupportedOperationException(
                    "Unsupported citation type: " + citation.getClass().getName());
        };
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

    @Override
    protected boolean isCacheable() {
        return CACHING_AVAILABLE;
    }

    @Override
    protected void configureCaching() {
        // Find the last cacheable block in the conversation and add cache control to it
        // Walk backwards through vendorMessages to find the most recent cacheable block

        // TODO: Add support for caching on tool use blocks when tools are implemented
        // TODO: Add support for caching on system prompts when system prompts are supported
        // TODO: Anthropic only caches up to 20 blocks - need to handle this limitation
        //       (currently we only cache one block, but when we expand to multiple cache points,
        //       we'll need to ensure we don't exceed the 20-block limit)

        for (int i = vendorMessages.size() - 1; i >= 0; i--) {
            MessageParam message = vendorMessages.get(i);

            // Get the content from the message
            if (message.content() == null) {
                continue;
            }

            // Try to get block params from the content
            Optional<List<ContentBlockParam>> blockParamsOpt = message.content().blockParams();
            if (blockParamsOpt.isEmpty()) {
                continue;
            }

            List<ContentBlockParam> blockParams = blockParamsOpt.get();

            // Walk backwards through the blocks to find the last cacheable one
            for (int j = blockParams.size() - 1; j >= 0; j--) {
                ContentBlockParam block = blockParams.get(j);

                // Try to rebuild the block with cache control
                ContentBlockParam rebuiltBlock = rebuildBlockWithCacheControl(block);
                if (rebuiltBlock != null) {
                    // Replace the block in the list
                    List<ContentBlockParam> newBlockParams = new ArrayList<>(blockParams);
                    newBlockParams.set(j, rebuiltBlock);

                    // Rebuild the message with the updated blocks
                    MessageParam newMessage = MessageParam.builder()
                            .role(message.role())
                            .content(MessageParam.Content.ofBlockParams(newBlockParams))
                            .build();

                    // Replace the message in vendorMessages
                    vendorMessages.set(i, newMessage);

                    // We found and updated the last cacheable block, so we're done
                    return;
                }
            }
        }
    }

    /**
     * Attempts to rebuild a ContentBlockParam with cache control.
     * Returns the rebuilt block if successful, or null if the block type doesn't support caching.
     *
     * @param block the original block
     * @return the rebuilt block with cache control, or null if not cacheable
     */
    private ContentBlockParam rebuildBlockWithCacheControl(ContentBlockParam block) {
        // Create the cache control object once and reuse it
        var cacheControl = com.anthropic.models.messages.CacheControlEphemeral.builder()
                .ttl(com.anthropic.models.messages.CacheControlEphemeral.Ttl.TTL_5M)
                .build();

        // Check if this is a TextBlockParam
        if (block.text().isPresent()) {
            TextBlockParam textBlock = block.text().get();
            TextBlockParam rebuiltTextBlock = textBlock.toBuilder()
                    .cacheControl(cacheControl)
                    .build();
            return ContentBlockParam.ofText(rebuiltTextBlock);
        }

        // Check if this is an ImageBlockParam
        if (block.image().isPresent()) {
            ImageBlockParam imageBlock = block.image().get();
            ImageBlockParam rebuiltImageBlock = imageBlock.toBuilder()
                    .cacheControl(cacheControl)
                    .build();
            return ContentBlockParam.ofImage(rebuiltImageBlock);
        }

        // Check if this is a DocumentBlockParam
        if (block.document().isPresent()) {
            DocumentBlockParam docBlock = block.document().get();
            DocumentBlockParam rebuiltDocBlock = docBlock.toBuilder()
                    .cacheControl(cacheControl)
                    .build();
            return ContentBlockParam.ofDocument(rebuiltDocBlock);
        }

        // This block type doesn't support caching (or we don't support it yet)
        return null;
    }

    /**
     * Returns the cumulative cache creation input tokens across all messages.
     * These represent tokens used when creating new cache entries.
     *
     * @return the total cache creation input tokens
     */
    public long getTotalCacheCreationInputTokens() {
        return totalCacheCreationInputTokens;
    }

    /**
     * Returns the cumulative cache read input tokens across all messages.
     * These represent tokens read from existing cache entries (at reduced cost).
     *
     * @return the total cache read input tokens
     */
    public long getTotalCacheReadInputTokens() {
        return totalCacheReadInputTokens;
    }
}
