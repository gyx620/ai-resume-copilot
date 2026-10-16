package com.gyx.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    private EmbeddingModel embeddingModel;

    @PostConstruct
    public void init() {
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl("https://api.deepseek.com")
                .modelName("text-embedding-3-small")
                .build();
    }

    public float[] embed(String text) {
        Embedding embedding = embeddingModel.embed(text).content();
        float[] vector = new float[embedding.vector().length];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = embedding.vector()[i];
        }
        return vector;
    }

    public List<float[]> embedAll(List<String> texts) {
        List<TextSegment> segments = texts.stream()
                .map(TextSegment::from)
                .toList();
        return embeddingModel.embedAll(segments).content().stream()
                .map(e -> {
                    float[] v = new float[e.vector().length];
                    for (int i = 0; i < v.length; i++) {
                        v[i] = e.vector()[i];
                    }
                    return v;
                })
                .toList();
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public List<String> findSimilar(String query, List<String> documents, int topK) {
        float[] queryVector = embed(query);
        return documents.stream()
                .map(doc -> {
                    float[] docVector = embed(doc);
                    return new DocSimilarity(doc, cosineSimilarity(queryVector, docVector));
                })
                .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                .limit(topK)
                .map(ds -> ds.document)
                .toList();
    }

    private record DocSimilarity(String document, double similarity) {}
}
