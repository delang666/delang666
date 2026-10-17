package com.dl.langchain4jragdemo.ai.tool;

import com.dl.langchain4jragdemo.pojo.MajorScore;
import com.dl.langchain4jragdemo.pojo.University;
import com.dl.langchain4jragdemo.service.UniversityAdmissionService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 高校录取分数查询工具：让 AI 在内部知识库覆盖不到时，联网查询真实录取分数
 */
@Component
public class UniversityAdmissionTool {

    @Autowired
    private UniversityAdmissionService universityAdmissionService;

    // 1.工具方法：根据学校名称搜索学校
    @Tool("根据学校名称搜索学校，返回学校列表（含学校id、所在省市、办学层次等）")
    public List<University> searchUniversity(@P("学校名称，例如广东工业大学") String name) {
        return universityAdmissionService.searchUniversity(name);
    }

    // 2.工具方法：查询某学校某年某省份的专业录取分数
    @Tool("查询某学校某年某省份的专业录取分数线和位次")
    public List<MajorScore> getMajorScores(
            @P("学校名称") String schoolName,
            @P("年份，例如2024") Integer year,
            @P("省份名称，例如广东") String provinceName,
            @P(value = "科类，物理类或历史类，可选", required = false) String typeName,
            @P(value = "专业名称关键词，可选", required = false) String majorKeyword) {
        return universityAdmissionService.getMajorScores(schoolName, year, provinceName, typeName, majorKeyword);
    }
}
