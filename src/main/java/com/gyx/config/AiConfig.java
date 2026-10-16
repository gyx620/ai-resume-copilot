package com.gyx.config;

import com.gyx.ai.ResumeAssistant;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name:deepseek-chat}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.temperature:0.3}")
    private Double temperature;

    private static final String DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1";

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(DEEPSEEK_BASE_URL)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(120))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(DEEPSEEK_BASE_URL)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(120))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean
    public ResumeAssistant resumeAssistant(
            ChatLanguageModel chatModel,
            StreamingChatLanguageModel streamingModel) {
        return AiServices.builder(ResumeAssistant.class)
                .chatLanguageModel(chatModel)
                .streamingChatLanguageModel(streamingModel)
                .build();
    }
}
