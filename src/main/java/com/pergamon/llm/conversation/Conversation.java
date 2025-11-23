package com.pergamon.llm.conversation;

import com.pergamon.llm.config.ApiConfig;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Abstract base class for conversations with different LLM vendors.
 *
 * This class merges conversation state and conversation management, implementing
 * the Template Method pattern to handle the common flow of:
 * 1. Converting our Message to vendor-specific format
 * 2. Sending the entire conversation history to the vendor API
 * 3. Converting the vendor response back to our Message format
 * 4. Automatically appending messages to both generic and vendor-specific histories
 *
 * Maintains two parallel conversation histories:
 * - messages: Generic Message objects (vendor-agnostic)
 * - vendorMessages: Vendor-specific message objects (e.g., Anthropic's MessageParam)
 *
 * Each API call sends the full conversation context, enabling multi-turn conversational
 * interactions where the model can reference previous messages in the conversation.
 *
 * @param <V> The vendor-specific message type (e.g., MessageParam for Anthropic)
 * @param <R> The vendor-specific response type (e.g., com.anthropic.models.messages.Message)
 */
public abstract class Conversation<V, R> {

    // Nullable: assigned by the database (UUID string) when persisted
    private String id;

    private final ModelId modelId;

    private String name;
    private boolean starred = false;

    // Optional vendor metadata (nullable)
    private String vendorConversationId;
    private String vendorProjectId;

    /**
     * Generic vendor-agnostic conversation history.
     */
    private final List<Message> messages = new ArrayList<>();

    /**
     * Vendor-specific conversation history.
     * Maintained in parallel with messages to avoid repeated serialization/deserialization.
     */
    protected final List<V> vendorMessages = new ArrayList<>();

    // --------- Static Factory Methods ---------

    /**
     * Factory method to create a Conversation for a specific model.
     *
     * @param modelId the model to create a conversation for
     * @param config the API configuration containing vendor API keys
     * @return a Conversation instance for the model's vendor
     * @throws IllegalArgumentException if the vendor is not supported
     */
    public static Conversation<?, ?> forModel(ModelId modelId, ApiConfig config) {
        return switch (modelId.vendor()) {
            case ANTHROPIC -> createAnthropicConversation(modelId, config);
            case OPENAI -> createOpenAIConversation(modelId, config);
            case GOOGLE -> createGoogleConversation(modelId, config);
        };
    }

    private static Conversation<?, ?> createAnthropicConversation(ModelId modelId, ApiConfig config) {
        String apiKey = config.getApiKeyOrThrow(Vendor.ANTHROPIC);
        return new AnthropicConversation(modelId, apiKey);
    }

    private static Conversation<?, ?> createOpenAIConversation(ModelId modelId, ApiConfig config) {
        // TODO: Implement OpenAI conversation
        throw new UnsupportedOperationException("OpenAI conversation not yet implemented");
    }

    private static Conversation<?, ?> createGoogleConversation(ModelId modelId, ApiConfig config) {
        // TODO: Implement Google conversation
        throw new UnsupportedOperationException("Google conversation not yet implemented");
    }

    // --------- Constructors ---------

    /**
     * Creates a new Conversation with a generated default name.
     * id is null until the database assigns it.
     */
    protected Conversation(ModelId modelId) {
        this(modelId, null);
    }

    /**
     * Creates a Conversation with an explicit name (or default if null/blank).
     */
    protected Conversation(ModelId modelId, String name) {
        if (modelId == null) {
            throw new IllegalArgumentException("modelId cannot be null");
        }

        this.modelId = modelId;
        this.name = (name == null || name.isBlank())
                ? generateDefaultName()
                : name;
    }

    // --------- Identity & model ---------

    /** Returns the UUID string assigned by the DB, if any. */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /** To be called by the persistence layer once the DB has generated a UUID. */
    public void setId(String id) {
        this.id = id;
    }

    public ModelId modelId() {
        return modelId;
    }

    // --------- Name & starred ---------

    public String name() {
        return name;
    }

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Conversation name cannot be null or blank");
        }
        this.name = newName;
    }

    public boolean isStarred() {
        return starred;
    }

    /** Marks the conversation as starred. */
    public void star() {
        this.starred = true;
    }

    /** Removes the starred flag. */
    public void unstar() {
        this.starred = false;
    }

    /** Allows general-purpose manual setting if needed. */
    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    /** Flips the starred state. */
    public void toggleStar() {
        this.starred = !this.starred;
    }

    // --------- Vendor metadata ---------

    public Optional<String> vendorConversationId() {
        return Optional.ofNullable(vendorConversationId);
    }

    public void setVendorConversationId(String vendorConversationId) {
        this.vendorConversationId = vendorConversationId;
    }

    public Optional<String> vendorProjectId() {
        return Optional.ofNullable(vendorProjectId);
    }

    public void setVendorProjectId(String vendorProjectId) {
        this.vendorProjectId = vendorProjectId;
    }

    // --------- Core Conversation Logic ---------

    /**
     * Sends a message in the conversation and returns the response.
     *
     * This method implements the template pattern:
     * 1. Appends the user message to messages
     * 2. Converts to vendor-specific format
     * 3. Appends the vendor message to vendorMessages
     * 4. Calls the vendor API with the entire conversation history
     * 5. Appends the vendor response to vendorMessages
     * 6. Converts response back to our Message format
     * 7. Appends the response message to messages
     *
     * @param message the message to send
     * @return the response message from the LLM
     */
    public Message sendMessage(Message message) {
        // 1. Append the user message to generic history
        messages.add(message);

        // 2. Convert our Message to vendor-specific format
        V vendorMessage = toVendorMessage(message);

        // 3. Append the vendor message to vendor-specific history
        vendorMessages.add(vendorMessage);

        // 4. Send the entire conversation to the vendor API
        R vendorResponse = sendConversationToVendor();

        // 5. Convert vendor response to vendor message format and append
        V vendorResponseMessage = vendorResponseToVendorMessage(vendorResponse);
        vendorMessages.add(vendorResponseMessage);

        // 6. Convert vendor response back to our Message format
        Message responseMessage = fromVendorResponse(vendorResponse);

        // 7. Append the response to generic history
        messages.add(responseMessage);

        return responseMessage;
    }

    // --------- Abstract Methods for Subclasses ---------

    /**
     * Converts our generic Message to the vendor-specific message format.
     * Subclasses implement this to handle vendor-specific message structure.
     *
     * @param message our generic message
     * @return the vendor-specific message object
     */
    protected abstract V toVendorMessage(Message message);

    /**
     * Sends the entire conversation history to the vendor API and returns the response.
     * This is where the actual API call happens.
     *
     * Subclasses should use the vendorMessages list to include the full conversation context
     * in the API request, enabling multi-turn conversational interactions.
     *
     * @return the vendor-specific response
     */
    protected abstract R sendConversationToVendor();

    /**
     * Converts a vendor-specific response to a vendor-specific message format
     * for storage in the vendorMessages history.
     *
     * @param vendorResponse the vendor-specific response
     * @return the vendor-specific message representation of the response
     */
    protected abstract V vendorResponseToVendorMessage(R vendorResponse);

    /**
     * Converts a vendor-specific response back to our generic Message format.
     *
     * @param vendorResponse the vendor-specific response
     * @return our generic message
     */
    protected abstract Message fromVendorResponse(R vendorResponse);

    // --------- Messages ---------

    public List<Message> messages() {
        return Collections.unmodifiableList(messages);
    }

    public void append(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }
        messages.add(message);
    }

    public void clearMessages() {
        messages.clear();
        vendorMessages.clear();
    }

    // --------- Vendor Messages ---------

    /**
     * Returns an unmodifiable view of the vendor-specific message history.
     */
    public List<V> vendorMessages() {
        return Collections.unmodifiableList(vendorMessages);
    }

    // --------- Helpers ---------

    private static String generateDefaultName() {
        final String prefix = "Untitled-";
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        final int suffixLength = 16;

        long ts = Instant.now().toEpochMilli();
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(suffixLength);

        for (int i = 0; i < suffixLength; i++) {
            int idx = rng.nextInt(alphabet.length());
            sb.append(alphabet.charAt(idx));
        }

        return prefix + ts + "-" + sb;
    }
}
