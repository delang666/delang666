package com.dl.langchain4jragdemo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学校搜索结果（掌上高考接口返回）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class University {

    /** 学校id（掌上高考内部id） */
    private Long schoolId;

    /** 学校名称 */
    private String name;

    /** 所在省份 */
    private String provinceName;

    /** 所在城市 */
    private String cityName;

    /** 办学层次（本科/专科） */
    private String levelName;

    /** 办学性质（公办/民办） */
    private String natureName;

    /** 院校类型（理工类/综合类等） */
    private String typeName;
}
