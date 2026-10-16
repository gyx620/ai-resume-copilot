package com.gyx.vo;

import lombok.Data;

/**
 * AI 分析结果 VO（返回给前端）
 * <p>
 * 面试要点：VO（View Object）用于展示层，与 Entity 分离，
 * 避免直接暴露数据库表结构给前端。
 *
 * @author gyx
 */
@Data
public class AiResultVO {

    /** 简历评分 */
    private Integer score;

    /** 优点分析 */
    private String advantage;

    /** 存在问题 */
    private String problem;

    /** 优化建议 */
    private String suggestion;

}
