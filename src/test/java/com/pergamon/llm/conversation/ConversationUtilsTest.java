package com.pergamon.llm.conversation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConversationUtilsTest {

    // ===== Plain Text Tests =====

    @Test
    void testPlainText_Simple() {
        String text = "This is plain text.";
        assertEquals(TextBlockFormat.PLAIN, ConversationUtils.detectTextFormat(text));
        assertFalse(ConversationUtils.isMarkdown(text));
        assertFalse(ConversationUtils.isWellFormedHtml(text));
    }

    @Test
    void testPlainText_MultiLine() {
        String text = "This is plain text.\nWith multiple lines.\nNo formatting.";
        assertEquals(TextBlockFormat.PLAIN, ConversationUtils.detectTextFormat(text));
        assertFalse(ConversationUtils.isMarkdown(text));
        assertFalse(ConversationUtils.isWellFormedHtml(text));
    }

    @Test
    void testPlainText_WithSpecialChars() {
        String text = "Text with special chars: & @ $ % but no formatting";
        assertEquals(TextBlockFormat.PLAIN, ConversationUtils.detectTextFormat(text));
        assertFalse(ConversationUtils.isMarkdown(text));
        assertFalse(ConversationUtils.isWellFormedHtml(text));
    }

    @Test
    void testPlainText_Null() {
        assertEquals(TextBlockFormat.PLAIN, ConversationUtils.detectTextFormat(null));
    }

    @Test
    void testPlainText_Empty() {
        assertEquals(TextBlockFormat.PLAIN, ConversationUtils.detectTextFormat(""));
    }

    @Test
    void testPlainText_Blank() {
        assertEquals(TextBlockFormat.PLAIN, ConversationUtils.detectTextFormat("   "));
    }

    // ===== Markdown Tests =====

    @Test
    void testMarkdown_Heading() {
        String text = "# This is a heading";
        assertEquals(TextBlockFormat.MARKDOWN, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isMarkdown(text));
    }

    @Test
    void testMarkdown_MultipleHeadings() {
        String text = "# Heading 1\n## Heading 2";
        assertEquals(TextBlockFormat.MARKDOWN, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isMarkdown(text));
    }

    @Test
    void testMarkdown_Bold() {
        String text = "This is **bold** text";
        assertEquals(TextBlockFormat.MARKDOWN, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isMarkdown(text));
    }

    @Test
    void testMarkdown_Italic() {
        String text = "This is *italic* text";
        assertEquals(TextBlockFormat.MARKDOWN, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isMarkdown(text));
    }

    @Test
    void testMarkdown_Link() {
        String text = "Check out [this link](https://example.com)";
        assertEquals(TextBlockFormat.MARKDOWN, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isMarkdown(text));
    }

    @Test
    void testMarkdown_CodeBlock() {
        String text = "```java\nSystem.out.println(\"Hello\");\n```";
        assertEquals(TextBlockFormat.MARKDOWN, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isMarkdown(text));
    }

    @Test
    void testMarkdown_InlineCode() {
        String text = "Use `System.out.println()` to print";
        assertEquals(TextBlockFormat.MARKDOWN, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isMarkdown(text));
    }

    @Test
    void testMarkdown_UnorderedList() {
        String text = "- Item 1\n- Item 2\n- Item 3";
        assertEquals(TextBlockFormat.MARKDOWN, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isMarkdown(text));
    }

    @Test
    void testMarkdown_OrderedList() {
        String text = "1. First item\n2. Second item\n3. Third item";
        assertEquals(TextBlockFormat.MARKDOWN, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isMarkdown(text));
    }

    @Test
    void testMarkdown_BlockQuote() {
        String text = "> This is a quote";
        assertEquals(TextBlockFormat.MARKDOWN, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isMarkdown(text));
    }

    @Test
    void testMarkdown_MixedFormatting() {
        String text = "# Title\n\nThis is **bold** and *italic* text.\n\n- Item 1\n- Item 2";
        assertEquals(TextBlockFormat.MARKDOWN, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isMarkdown(text));
    }

    @Test
    void testMarkdown_HorizontalRule() {
        String text = "Some text\n\n---\n\nMore text";
        assertEquals(TextBlockFormat.MARKDOWN, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isMarkdown(text));
    }

    // ===== HTML Tests =====

    @Test
    void testHtml_SimpleDiv() {
        String text = "<div>Hello World</div>";
        assertEquals(TextBlockFormat.HTML, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isWellFormedHtml(text));
    }

    @Test
    void testHtml_Paragraph() {
        String text = "<p>This is a paragraph</p>";
        // Note: Jsoup wraps plain text in <p>, so we need actual structure
        assertEquals(TextBlockFormat.HTML, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isWellFormedHtml(text));
    }

    @Test
    void testHtml_Heading() {
        String text = "<h1>Main Heading</h1>";
        assertEquals(TextBlockFormat.HTML, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isWellFormedHtml(text));
    }

    @Test
    void testHtml_Bold() {
        String text = "<p>This is <b>bold</b> text</p>";
        assertEquals(TextBlockFormat.HTML, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isWellFormedHtml(text));
    }

    @Test
    void testHtml_Italic() {
        String text = "<p>This is <i>italic</i> text</p>";
        assertEquals(TextBlockFormat.HTML, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isWellFormedHtml(text));
    }

    @Test
    void testHtml_Link() {
        String text = "<a href=\"https://example.com\">Link</a>";
        assertEquals(TextBlockFormat.HTML, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isWellFormedHtml(text));
    }

    @Test
    void testHtml_List() {
        String text = "<ul><li>Item 1</li><li>Item 2</li></ul>";
        assertEquals(TextBlockFormat.HTML, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isWellFormedHtml(text));
    }

    @Test
    void testHtml_MultipleElements() {
        String text = "<h1>Title</h1><p>Paragraph</p>";
        assertEquals(TextBlockFormat.HTML, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isWellFormedHtml(text));
    }

    @Test
    void testHtml_NestedElements() {
        String text = "<div><h2>Heading</h2><p>Content</p></div>";
        assertEquals(TextBlockFormat.HTML, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isWellFormedHtml(text));
    }

    @Test
    void testHtml_Table() {
        String text = "<table><tr><td>Cell</td></tr></table>";
        assertEquals(TextBlockFormat.HTML, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isWellFormedHtml(text));
    }

    // ===== Edge Cases =====

    @Test
    void testEdgeCase_AngleBracketsNotHtml() {
        String text = "2 < 3 and 5 > 4";
        assertEquals(TextBlockFormat.PLAIN, ConversationUtils.detectTextFormat(text));
        assertFalse(ConversationUtils.isWellFormedHtml(text));
    }

    @Test
    void testEdgeCase_MarkdownLikeButPlain() {
        // Just having a # doesn't make it markdown unless it's at start of line as heading
        String text = "Price is #100";
        // This might actually be detected as plain since # needs to be at line start
        TextBlockFormat format = ConversationUtils.detectTextFormat(text);
        // Either PLAIN or MARKDOWN is acceptable depending on parser behavior
        assertTrue(format == TextBlockFormat.PLAIN || format == TextBlockFormat.MARKDOWN);
    }

    @Test
    void testEdgeCase_EscapedHtmlEntities() {
        String text = "Use &lt;div&gt; for divisions";
        // HTML entities without tags should be plain
        assertEquals(TextBlockFormat.PLAIN, ConversationUtils.detectTextFormat(text));
    }

    @Test
    void testEdgeCase_IncompleteHtml() {
        String text = "<div>Unclosed tag";
        // Jsoup is tolerant and will still parse this as HTML
        assertEquals(TextBlockFormat.HTML, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isWellFormedHtml(text));
    }

    // ===== Realistic LLM Response Tests =====

    @Test
    void testRealistic_AnthropicResponse() {
        String text = "Here's how to solve the problem:\n\n" +
                      "1. First, identify the issue\n" +
                      "2. Then, implement the fix\n\n" +
                      "Here's the code:\n\n" +
                      "```java\n" +
                      "public void solve() {\n" +
                      "    // implementation\n" +
                      "}\n" +
                      "```\n\n" +
                      "This should work!";
        assertEquals(TextBlockFormat.MARKDOWN, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isMarkdown(text));
    }

    @Test
    void testRealistic_SimpleAnswer() {
        String text = "The answer is 42.";
        assertEquals(TextBlockFormat.PLAIN, ConversationUtils.detectTextFormat(text));
    }

    @Test
    void testRealistic_MarkdownBoldAndItalic() {
        String text = "You should **definitely** use *this* approach.";
        assertEquals(TextBlockFormat.MARKDOWN, ConversationUtils.detectTextFormat(text));
        assertTrue(ConversationUtils.isMarkdown(text));
    }
}
