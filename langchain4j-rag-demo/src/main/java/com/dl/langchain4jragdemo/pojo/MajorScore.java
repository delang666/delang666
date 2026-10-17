package com.dl.langchain4jragdemo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 专业录取分数（掌上高考接口返回）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MajorScore {

    /** 学校名称 */
    private String schoolName;

    /** 专业名称 */
    private String majorName;

    /** 录取批次（如本科批） */
    private String batchName;

    /** 科类（物理类/历史类） */
    private String typeName;

    /** 最低分 */
    private String minScore;

    /** 最低位次 */
    private String minRank;

    /** 省控线 */
    private String proscore;

    /** 年份 */
    private Integer year;
}
