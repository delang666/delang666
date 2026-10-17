package com.dl.langchain4jragdemo.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiRagDemoService {

    /**
     * 生成 HTML 代码 (流式)
     *
     * @param userMessage 用户信息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/system.txt")
    Flux<String> chat(@UserMessage String userMessage);
}
