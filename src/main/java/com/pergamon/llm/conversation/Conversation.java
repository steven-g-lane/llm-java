package com.pergamon.llm.conversation;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A vendor-agnostic conversation that holds a sequence of Messages and
 * metadata for both local and vendor-specific tracking.
 */
public final class Conversation {

    // Nullable: assigned by the database (UUID string) when persisted
    private String id;

    private final ModelId modelId;

    private String name;
    private boolean starred = false;

    // Optional vendor metadata (nullable)
    private String vendorConversationId;
    private String vendorProjectId;

    private final List<Message> messages = new ArrayList<>();

    /**
     * Creates a new Conversation with a generated default name.
     * id is null until the database assigns it.
     */
    public Conversation(ModelId modelId) {
        this(modelId, null);
    }

    /**
     * Creates a Conversation with an explicit name (or default if null/blank).
     */
    public Conversation(ModelId modelId, String name) {
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
