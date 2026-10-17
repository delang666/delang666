package com.dl.langchain4jragdemo;

import com.dl.langchain4jragdemo.ai.AiRagDemoService;
import com.dl.langchain4jragdemo.ai.AiRagDemoServiceFactory;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;

@SpringBootTest
public class test1 {

    @Resource
    private AiRagDemoServiceFactory aiRagDemoServiceFactory;

    @Test
    public void aiTest() {
        AiRagDemoService aiRagDemoService = aiRagDemoServiceFactory.createAiRagDemoService(1L);
        Flux<String> codeStream = aiRagDemoService.chat("你是谁");
        // 阻塞等待所有数据收集完成
        List<String> result = codeStream.collectList().block();
        // 验证结果
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }
}
