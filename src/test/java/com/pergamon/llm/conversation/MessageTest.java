package com.pergamon.llm.conversation;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void testCreateMessageWithSingleTextBlock() {
        TextBlock textBlock = new TextBlock(TextBlockFormat.PLAIN, "Hello, World!", List.of());
        Message message = new Message(MessageRole.USER, List.of(textBlock));

        assertEquals(MessageRole.USER, message.role());
        assertEquals(1, message.blocks().size());
        assertEquals(textBlock, message.blocks().get(0));
    }

    @Test
    void testWithBlockAddsBlockToMessage() {
        Message initial = new Message(MessageRole.USER, List.of());
        TextBlock textBlock = new TextBlock(TextBlockFormat.PLAIN, "Hello", List.of());

        Message updated = initial.withBlock(textBlock);

        assertEquals(0, initial.blocks().size(), "Original message should be unchanged");
        assertEquals(1, updated.blocks().size(), "Updated message should have one block");
        assertEquals(textBlock, updated.blocks().get(0));
    }

    @Test
    void testWithBlocksAddsMultipleBlocks() {
        Message initial = new Message(MessageRole.ASSISTANT, List.of());
        TextBlock block1 = new TextBlock(TextBlockFormat.PLAIN, "First", List.of());
        TextBlock block2 = new TextBlock(TextBlockFormat.MARKDOWN, "**Second**", List.of());

        Message updated = initial.withBlocks(List.of(block1, block2));

        assertEquals(0, initial.blocks().size(), "Original message should be unchanged");
        assertEquals(2, updated.blocks().size());
        assertEquals(block1, updated.blocks().get(0));
        assertEquals(block2, updated.blocks().get(1));
    }

    @Test
    void testFluentApiWithMultipleBlockTypes() {
        // Create blocks of different types
        TextBlock plainText = new TextBlock(TextBlockFormat.PLAIN, "Here is an image from a URL:", List.of());
        URLImageBlock urlImage = new URLImageBlock(
                URI.create("https://example.com/photo.jpg"),
                "image/jpeg"
        );
        TextBlock markdown = new TextBlock(TextBlockFormat.MARKDOWN, "And here is a **base64** image:", List.of());
        Base64ImageBlock base64Image = new Base64ImageBlock(
                "iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==",
                "image/png"
        );
        TextBlock htmlText = new TextBlock(TextBlockFormat.HTML, "<p>Finally, some HTML</p>", List.of());

        // Build message fluently
        Message message = new Message(MessageRole.USER, List.of())
                .withBlock(plainText)
                .withBlock(urlImage)
                .withBlock(markdown)
                .withBlock(base64Image)
                .withBlock(htmlText);

        // Verify block count
        assertEquals(5, message.blocks().size(), "Message should contain 5 blocks");
        assertEquals(MessageRole.USER, message.role());

        // Verify block types and order
        assertTrue(message.blocks().get(0) instanceof TextBlock);
        assertTrue(message.blocks().get(1) instanceof URLImageBlock);
        assertTrue(message.blocks().get(2) instanceof TextBlock);
        assertTrue(message.blocks().get(3) instanceof Base64ImageBlock);
        assertTrue(message.blocks().get(4) instanceof TextBlock);

        // Verify specific content
        assertEquals("Here is an image from a URL:", ((TextBlock) message.blocks().get(0)).text());
        assertEquals("image/jpeg", ((URLImageBlock) message.blocks().get(1)).mimeType());
        assertEquals("image/png", ((Base64ImageBlock) message.blocks().get(3)).mimeType());
    }

    @Test
    void testMessageImmutability() {
        TextBlock block1 = new TextBlock(TextBlockFormat.PLAIN, "Block 1", List.of());
        Message message = new Message(MessageRole.SYSTEM, List.of(block1));

        // Attempting to modify the blocks list should throw exception
        assertThrows(UnsupportedOperationException.class, () -> {
            message.blocks().add(new TextBlock(TextBlockFormat.PLAIN, "Block 2", List.of()));
        }, "Blocks list should be immutable");
    }

    @Test
    void testCreateMessageWithEmptyBlockList() {
        Message message = new Message(MessageRole.TOOL, List.of());

        assertEquals(MessageRole.TOOL, message.role());
        assertEquals(0, message.blocks().size());
        assertTrue(message.blocks().isEmpty());
    }

    @Test
    void testMessageEquality() {
        TextBlock block = new TextBlock(TextBlockFormat.PLAIN, "Test", List.of());
        Message msg1 = new Message(MessageRole.USER, List.of(block));
        Message msg2 = new Message(MessageRole.USER, List.of(block));

        assertEquals(msg1, msg2, "Messages with same role and blocks should be equal");
        assertEquals(msg1.hashCode(), msg2.hashCode(), "Equal messages should have same hash code");
    }

    @Test
    void testMixedImageTypes() {
        URLImageBlock urlImage1 = new URLImageBlock(
                URI.create("https://example.com/image1.png"),
                "image/png"
        );
        Base64ImageBlock base64Image = new Base64ImageBlock(
                "base64data==",
                "image/gif"
        );
        URLImageBlock urlImage2 = new URLImageBlock(
                URI.create("https://example.com/image2.webp"),
                "image/webp"
        );

        Message message = new Message(MessageRole.ASSISTANT, List.of())
                .withBlock(urlImage1)
                .withBlock(base64Image)
                .withBlock(urlImage2);

        assertEquals(3, message.blocks().size());
        assertTrue(message.blocks().get(0) instanceof URLImageBlock);
        assertTrue(message.blocks().get(1) instanceof Base64ImageBlock);
        assertTrue(message.blocks().get(2) instanceof URLImageBlock);
    }
}
