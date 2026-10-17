package com.dl.langchain4jragdemo.mapper;

import com.dl.langchain4jragdemo.pojo.ChatHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatHistoryMapper {

    // 保存一条聊天历史
    @Insert("insert into chat_history(`message`, `messageType`, `sessionId`, `userId`) values(#{message}, #{messageType}, #{sessionId}, #{userId})")
    int insert(ChatHistory chatHistory);

    // 根据会话id查询聊天历史（升序）
    @Select("select * from chat_history where `sessionId` = #{sessionId} and `isDelete` = 0 order by `createTime` asc, `id` asc")
    List<ChatHistory> listBySessionId(Long sessionId);
}
