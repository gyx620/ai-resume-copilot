package com.gyx.controller;

import com.gyx.common.Result;
import com.gyx.dto.AIAnalyzeDTO;
import com.gyx.service.AiService;
import com.gyx.vo.AiResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/ai")//路径前缀
public class AIController {

    @Autowired
    private AiService aiService;

    @PostMapping("/analyze")
    public Result<AiResultVO> analyze(@RequestBody AIAnalyzeDTO dto) {
        return Result.success(aiService.analyze(dto.getResumeId()));
    }

    @PostMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeStream(@RequestBody AIAnalyzeDTO dto) {
        SseEmitter emitter = new SseEmitter(300_000L);

        aiService.analyzeStream(
                dto.getResumeId(),
                token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                () -> emitter.complete(),
                error -> emitter.completeWithError(error)
         );

        return emitter;
    }
}
