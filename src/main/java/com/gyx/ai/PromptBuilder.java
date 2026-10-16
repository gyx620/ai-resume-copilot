package com.gyx.ai;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildAnalyzePrompt(String resumeContent) {
        return """
                你是一名专业Java后端技术面试官，请分析以下简历内容。

                简历内容：
                %s

                请从以下维度进行评分和分析，严格返回JSON格式（不要包含markdown代码块标记）：

                {
                  "score": 数字(0-100),
                  "advantage": "候选人优势",
                  "problem": "存在的问题",
                  "suggestion": "优化建议"
                }
                """.formatted(resumeContent);
    }

    public String buildOptimizePrompt(String resumeContent, String targetPosition) {
        return """
                你是一名资深简历优化专家，请针对以下简历和目标岗位进行优化。

                目标岗位：%s

                简历内容：
                %s

                请给出具体的简历修改建议，包括关键词优化、项目描述改进、技能亮点突出等方面。
                """.formatted(targetPosition, resumeContent);
    }
}