package com.gyx.service;

import com.gyx.vo.AiResultVO;

import java.util.function.Consumer;

public interface AiService {

    AiResultVO analyze(Long resumeId);

    void analyzeStream(Long resumeId, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
}
