package com.pergamon.llm.conversation;

import java.util.ArrayList;
import java.util.List;

public record Message(MessageRole role, List<MessageBlock> blocks) {

    /**
     * Creates a Message with an immutable copy of the provided blocks list.
     */
    public Message {
        blocks = List.copyOf(blocks);
    }

    /**
     * Returns a new Message with the specified block added to the end of the blocks list.
     * The original Message is unchanged.
     *
     * @param block the block to add
     * @return a new Message with the added block
     */
    public Message withBlock(MessageBlock block) {
        List<MessageBlock> newBlocks = new ArrayList<>(blocks);
        newBlocks.add(block);
        return new Message(role, newBlocks);
    }

    /**
     * Returns a new Message with the specified blocks added to the end of the blocks list.
     * The original Message is unchanged.
     *
     * @param additionalBlocks the blocks to add
     * @return a new Message with the added blocks
     */
    public Message withBlocks(List<MessageBlock> additionalBlocks) {
        List<MessageBlock> newBlocks = new ArrayList<>(blocks);
        newBlocks.addAll(additionalBlocks);
        return new Message(role, newBlocks);
    }
}