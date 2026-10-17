package com.dl.langchain4jragdemo.service;

import com.dl.langchain4jragdemo.enums.MessageType;
import com.dl.langchain4jragdemo.mapper.ChatHistoryMapper;
import com.dl.langchain4jragdemo.pojo.ChatHistory;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话聊天历史服务：负责历史的落库、查询以及从数据库恢复 AI 记忆
 */
@Service
public class ChatHistoryService {

    @Autowired
    private ChatHistoryMapper chatHistoryMapper;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 保存单条消息（供聊天自动落库）
     */
    public void save(Long sessionId, Long userId, String message, MessageType messageType) {
        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setSessionId(sessionId);
        chatHistory.setUserId(userId);
        chatHistory.setMessage(message);
        chatHistory.setMessageType(messageType);
        chatHistoryMapper.insert(chatHistory);
    }

    /**
     * 保存 ChatHistory 对象（供 controller 保存接口）
     */
    public void save(ChatHistory chatHistory) {
        chatHistoryMapper.insert(chatHistory);
    }

    /**
     * 根据会话id查询聊天历史（升序）
     */
    public List<ChatHistory> listBySessionId(Long sessionId) {
        return chatHistoryMapper.listBySessionId(sessionId);
    }

    /**
     * 恢复记忆：服务重启或 Redis 中的会话记忆过期时，从数据库回填到 Redis
     *
     * @param sessionId 会话id（与 AI 服务的 memoryId 对应）
     */
    public void restoreMemory(Long sessionId) {
        List<ChatMessage> existing = redisChatMemoryStore.getMessages(sessionId);
        if (existing != null && !existing.isEmpty()) {
            return;
        }
        List<ChatHistory> histories = chatHistoryMapper.listBySessionId(sessionId);
        if (histories == null || histories.isEmpty()) {
            return;
        }
        List<ChatMessage> messages = histories.stream()
                .map(this::toChatMessage)
                .collect(Collectors.toList());
        redisChatMemoryStore.updateMessages(sessionId, messages);
    }

    private ChatMessage toChatMessage(ChatHistory history) {
        if (history.getMessageType() == MessageType.USER) {
            return UserMessage.from(history.getMessage());
        }
        return AiMessage.from(history.getMessage());
    }
}
