package com.gyx.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    @Autowired
    private DocumentLoader documentLoader;

    @Autowired
    private TextSplitter textSplitter;

    @Autowired
    private EmbeddingService embeddingService;

    public String retrieveContext(String query, String knowledgeBase, int topK) {
        String content = documentLoader.load(knowledgeBase);
        List<String> chunks = textSplitter.split(content);

        if (chunks.isEmpty()) {
            return "";
        }

        List<String> relevant = embeddingService.findSimilar(query, chunks, topK);
        return relevant.stream().collect(Collectors.joining("\n---\n"));
    }

    public String buildAugmentedPrompt(String query, String context) {
        if (context == null || context.isBlank()) {
            return query;
        }
        return """
                请参考以下知识库内容回答问题：

                【知识库内容】
                %s

                【用户问题】
                %s
                """.formatted(context, query);
    }
}
