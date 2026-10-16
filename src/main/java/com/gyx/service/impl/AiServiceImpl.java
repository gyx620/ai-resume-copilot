package com.gyx.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyx.ai.PromptBuilder;
import com.gyx.ai.ResumeAssistant;
import com.gyx.entity.AiAnalysisRecord;
import com.gyx.entity.ResumeRecord;
import com.gyx.exception.BusinessException;
import com.gyx.mapper.AiAnalysisRecordMapper;
import com.gyx.mapper.ResumeRecordMapper;
import com.gyx.service.AiService;
import com.gyx.service.ResumeRecordService;
import com.gyx.vo.AiResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    @Autowired
    private ResumeAssistant resumeAssistant;

    @Autowired
    private PromptBuilder promptBuilder;

    @Autowired
    private ResumeRecordService resumeRecordService;

    @Autowired
    private AiAnalysisRecordMapper analysisRecordMapper;

    @Autowired
    private ResumeRecordMapper resumeRecordMapper;

    @Autowired(required = false)
    @Qualifier("stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private boolean redisAvailable() {
        if (redisTemplate == null) return false;
        try {
            redisTemplate.opsForValue().get("__health_check__");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public AiResultVO analyze(Long resumeId) {
        // try cache
        if (redisTemplate != null) {
            String cacheKey = "resume:ai:" + resumeId;
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return objectMapper.readValue(cached, AiResultVO.class);
                }
            } catch (Exception e) {
                log.debug("Redis缓存读取失败，跳过缓存: {}", e.getMessage());
            }
        }

        ResumeRecord resume = resumeRecordService.getById(resumeId);
        if (resume.getContent() == null || resume.getContent().isBlank()) {
            throw new BusinessException(400, "简历内容为空，无法分析");
        }

        String prompt = promptBuilder.buildAnalyzePrompt(resume.getContent());
        log.info("开始AI分析: resumeId={}", resumeId);
        String aiJson = resumeAssistant.analyze(prompt);
        log.info("AI分析完成: resumeId={}", resumeId);

        AiResultVO vo;
        try {
            vo = objectMapper.readValue(cleanJson(aiJson), AiResultVO.class);
        } catch (Exception e) {
            throw new BusinessException(500, "AI 结果解析失败");
        }

        AiAnalysisRecord record = new AiAnalysisRecord();
        record.setResumeId(resumeId);
        record.setScore(vo.getScore());
        record.setAdvantage(vo.getAdvantage());
        record.setProblem(vo.getProblem());
        record.setSuggestion(vo.getSuggestion());
        record.setCreateTime(LocalDateTime.now());
        analysisRecordMapper.insert(record);

        resume.setAiScore(vo.getScore());
        try {
            resume.setAiResult(objectMapper.writeValueAsString(vo));
        } catch (JsonProcessingException e) {
            resume.setAiResult(aiJson);
        }
        resumeRecordMapper.updateById(resume);
        log.info("分析结果已入库: resumeId={}, score={}", resumeId, vo.getScore());

        // try cache write
        if (redisTemplate != null) {
            try {
                String cacheKey = "resume:ai:" + resumeId;
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(vo), 30, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.debug("Redis缓存写入失败: {}", e.getMessage());
            }
        }

        return vo;
    }

    @Override
    public void analyzeStream(Long resumeId, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        ResumeRecord resume = resumeRecordService.getById(resumeId);
        if (resume.getContent() == null || resume.getContent().isBlank()) {
            onError.accept(new BusinessException(400, "简历内容为空，无法分析"));
            return;
        }

        String prompt = promptBuilder.buildAnalyzePrompt(resume.getContent());
        log.info("开始AI流式分析: resumeId={}", resumeId);
        StringBuilder fullText = new StringBuilder();

        resumeAssistant.analyzeStream(prompt)
                .onNext(token -> {
                    fullText.append(token);
                    onToken.accept(token);
                })
                .onComplete(response -> {
                    saveStreamResult(resumeId, resume, fullText.toString());
                    onComplete.run();
                })
                .onError(onError)
                .start();
    }

    private void saveStreamResult(Long resumeId, ResumeRecord resume, String fullText) {
        try {
            AiResultVO vo = objectMapper.readValue(cleanJson(fullText), AiResultVO.class);

            AiAnalysisRecord record = new AiAnalysisRecord();
            record.setResumeId(resumeId);
            record.setScore(vo.getScore());
            record.setAdvantage(vo.getAdvantage());
            record.setProblem(vo.getProblem());
            record.setSuggestion(vo.getSuggestion());
            record.setCreateTime(LocalDateTime.now());
            analysisRecordMapper.insert(record);

            resume.setAiScore(vo.getScore());
            try {
                resume.setAiResult(objectMapper.writeValueAsString(vo));
            } catch (JsonProcessingException e) {
                resume.setAiResult(fullText);
            }
            resumeRecordMapper.updateById(resume);

            if (redisTemplate != null) {
                try {
                    String cacheKey = "resume:ai:" + resumeId;
                    redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(vo), 30, TimeUnit.MINUTES);
                } catch (Exception e) {
                    log.debug("Redis缓存写入失败: {}", e.getMessage());
                }
            }

            log.info("流式分析结果已保存: resumeId={}, score={}", resumeId, vo.getScore());
        } catch (Exception e) {
            log.warn("流式分析结果保存失败: resumeId={}", resumeId, e);
        }
    }

    private String cleanJson(String raw) {
        String json = raw.trim();
        if (json.startsWith("```json")) {
            json = json.substring(7);
        } else if (json.startsWith("```")) {
            json = json.substring(3);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }
        return json.trim();
    }
}
