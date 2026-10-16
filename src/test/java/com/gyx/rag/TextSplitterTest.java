package com.gyx.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextSplitterTest {

    private TextSplitter textSplitter;

    @BeforeEach
    void setUp() {
        textSplitter = new TextSplitter();
    }

    @Test
    void split_shouldReturnEmptyList_whenTextIsNull() {
        assertTrue(textSplitter.split(null).isEmpty());
    }

    @Test
    void split_shouldReturnEmptyList_whenTextIsBlank() {
        assertTrue(textSplitter.split("   ").isEmpty());
    }

    @Test
    void split_shouldReturnSingleChunk_whenTextIsShort() {
        List<String> chunks = textSplitter.split("这是一段简短的文本。", 500, 50);
        assertEquals(1, chunks.size());
        assertEquals("这是一段简短的文本。", chunks.get(0));
    }

    @Test
    void split_shouldSplitLongText_bySentence() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("这是第").append(i).append("句话。");
        }
        List<String> chunks = textSplitter.split(sb.toString(), 200, 30);
        assertTrue(chunks.size() > 1);
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 250);
        }
    }

    @Test
    void split_shouldHandleTextWithoutSentenceMarkers() {
        String text = "a".repeat(1000);
        List<String> chunks = textSplitter.split(text, 300, 30);
        assertFalse(chunks.isEmpty());
    }
}
