package com.dl.langchain4jragdemo.controller;

import com.dl.langchain4jragdemo.common.Result;
import com.dl.langchain4jragdemo.enums.MessageType;
import com.dl.langchain4jragdemo.pojo.ChatHistory;
import com.dl.langchain4jragdemo.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    // 获取会话聊天历史
    @GetMapping("/list")
    public Result<List<ChatHistory>> list(@RequestParam Long sessionId) {
        return Result.success(chatHistoryService.listBySessionId(sessionId));
    }

    // 保存一条聊天历史（messageType 传 "user"/"ai"）
    @PostMapping("/save")
    public Result<Void> save(@RequestParam Long sessionId,
                             @RequestParam(required = false) Long userId,
                             @RequestParam String message,
                             @RequestParam String messageType) {
        chatHistoryService.save(sessionId, userId, message, MessageType.getEnumByValue(messageType));
        return Result.success();
    }
}
