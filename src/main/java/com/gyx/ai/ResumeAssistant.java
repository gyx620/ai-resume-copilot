package com.gyx.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface ResumeAssistant {

    @SystemMessage("""
            你是一名专业Java后端技术面试官。
            你的任务是分析用户简历，
            给出评分、优势、不足和优化建议。

            返回JSON格式：
            {
              "score":数字,
              "advantage":"优势",
              "problem":"问题",
              "suggestion":"建议"
            }
            """)
    String analyze(@UserMessage String resume);

    @SystemMessage("""
            你是一名专业Java后端技术面试官。
            你的任务是分析用户简历，
            给出评分、优势、不足和优化建议。

            返回JSON格式：
            {
              "score":数字,
              "advantage":"优势",
              "problem":"问题",
              "suggestion":"建议"
            }
            """)
    TokenStream analyzeStream(@UserMessage String resume);
}
