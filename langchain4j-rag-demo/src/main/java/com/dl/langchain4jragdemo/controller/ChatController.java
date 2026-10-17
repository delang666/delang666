package com.dl.langchain4jragdemo.controller;

import com.dl.langchain4jragdemo.ai.AiRagDemoService;
import com.dl.langchain4jragdemo.ai.AiRagDemoServiceFactory;
import com.dl.langchain4jragdemo.enums.MessageType;
import com.dl.langchain4jragdemo.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
public class ChatController {
    @Resource
    private AiRagDemoServiceFactory aiRagDemoServiceFactory;

    @Resource
    private ChatHistoryService chatHistoryService;

    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(String memoryId, String message) {
        long sessionId = Long.parseLong(memoryId);
        log.info("memoryId:{},message:{}", memoryId, message);

        // 用户消息落库
        chatHistoryService.save(sessionId, 1L, message, MessageType.USER);

        AiRagDemoService aiRagDemoService = aiRagDemoServiceFactory.createAiRagDemoService(sessionId);
        StringBuilder aiReply = new StringBuilder();
        return aiRagDemoService.chat(message)
                .doOnNext(aiReply::append)
                .doOnComplete(() -> chatHistoryService.save(sessionId, 1L, aiReply.toString(), MessageType.AI));
    }
}
