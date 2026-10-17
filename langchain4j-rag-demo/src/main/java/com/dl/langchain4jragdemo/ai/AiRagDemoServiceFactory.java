package com.dl.langchain4jragdemo.ai;

import com.dl.langchain4jragdemo.ai.guardrail.PromptSafetyInputGuardrail;
import com.dl.langchain4jragdemo.ai.tool.ReservationTool;
import com.dl.langchain4jragdemo.ai.tool.UniversityAdmissionTool;
import com.dl.langchain4jragdemo.service.ChatHistoryService;
import com.dl.langchain4jragdemo.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Slf4j
@Configuration
@DependsOn("springContextUtil")
public class AiRagDemoServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ContentRetriever contentRetriever;

    @Resource
    private ReservationTool reservationTool;

    @Resource
    private UniversityAdmissionTool universityAdmissionTool;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 创建新的 AI 服务实例
     */
    public AiRagDemoService createAiRagDemoService(long memoryId) {
        // 从数据库加载历史对话到记忆中（服务重启或 Redis 记忆过期时）
        chatHistoryService.restoreMemory(memoryId);

        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(memoryId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();

                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                return AiServices.builder(AiRagDemoService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .inputGuardrails(new PromptSafetyInputGuardrail()) // 添加输入护轨
                        //    .outputGuardrails(new RetryOutputGuardrail()) // 添加输出护轨，为了流式输出，不采用输出护轨
                        //    .outputGuardrailsConfig(outputGuardrailsConfig) // 最大重试次数
                        .contentRetriever(contentRetriever) // 配置向量数据库检索对象
                        .tools(reservationTool, universityAdmissionTool) // 配置工具
                        .build();
    }
}
