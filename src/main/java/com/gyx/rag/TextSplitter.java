package com.gyx.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class TextSplitter {

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int OVERLAP = 50;
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("(?<=[。！？\\n])\\s*");

    public List<String> split(String text) {
        return split(text, DEFAULT_CHUNK_SIZE, OVERLAP);
    }

    public List<String> split(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String[] sentences = SENTENCE_PATTERN.split(text);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        List<String> currentSentences = new ArrayList<>();

        int charCount = 0;
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (charCount + trimmed.length() > chunkSize && current.length() > 0) {
                chunks.add(current.toString().trim());

                int overlapChars = 0;
                current.setLength(0);
                for (int i = currentSentences.size() - 1; i >= 0; i--) {
                    String s = currentSentences.get(i);
                    if (overlapChars + s.length() > overlap) {
                        break;
                    }
                    current.insert(0, s);
                    overlapChars += s.length();
                }
                currentSentences.clear();
                charCount = overlapChars;
            }

            current.append(trimmed);
            currentSentences.add(trimmed);
            charCount += trimmed.length();
        }

        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }
}
