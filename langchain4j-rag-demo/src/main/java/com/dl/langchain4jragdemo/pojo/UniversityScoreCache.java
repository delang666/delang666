package com.dl.langchain4jragdemo.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 高校专业录取分数缓存实体
 */
@Data
public class UniversityScoreCache {

    private Long id;

    /** 学校id */
    private Long schoolId;

    /** 省份id */
    private String provinceId;

    /** 年份 */
    private Integer year;

    /** 专业录取分数JSON（List<MajorScore> 序列化结果） */
    private String scoreData;

    private LocalDateTime createTime;
}
