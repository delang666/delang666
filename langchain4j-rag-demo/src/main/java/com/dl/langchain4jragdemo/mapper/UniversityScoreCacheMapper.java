package com.dl.langchain4jragdemo.mapper;

import com.dl.langchain4jragdemo.pojo.UniversityScoreCache;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UniversityScoreCacheMapper {

    // 根据 学校id + 省份id + 年份 查询缓存
    @Select("select * from university_score_cache where school_id = #{schoolId} and province_id = #{provinceId} and `year` = #{year}")
    UniversityScoreCache get(@Param("schoolId") Long schoolId, @Param("provinceId") String provinceId, @Param("year") Integer year);

    // 写入缓存
    @Insert("insert into university_score_cache(school_id, province_id, `year`, score_data) values(#{schoolId}, #{provinceId}, #{year}, #{scoreData})")
    int insert(UniversityScoreCache cache);
}
