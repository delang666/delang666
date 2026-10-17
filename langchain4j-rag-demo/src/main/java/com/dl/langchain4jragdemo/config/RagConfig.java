package com.dl.langchain4jragdemo.config;

import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RagConfig {

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private RedisEmbeddingStore redisEmbeddingStore;

    @Bean
    public EmbeddingStore store() {
        // 加载文档进内存
        List<Document> documents = ClassPathDocumentLoader.loadDocuments("content", new ApachePdfBoxDocumentParser());
        // 构建向量数据库操作对象，（这里先用内存版本的向量数据库）
        // InMemoryEmbeddingStore store = new InMemoryEmbeddingStore();
        // 构建文档分割器对象
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 100);
        // 构建一个EmbeddingStoreIngestor对象，完成文本数据切割，向量化，存储
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                // 为了提高搜索质量，为每个 TextSegment 添加文档名称
                .textSegmentTransformer(textSegment -> TextSegment.from(
                        textSegment.metadata().getString("file_name") + "\n" + textSegment.text(),
                        textSegment.metadata()
                ))
                .embeddingStore(redisEmbeddingStore) // 设置向量数据库操作对象
                .documentSplitter(splitter) // 设置文档分割器对象
                .embeddingModel(embeddingModel) // 设置向量模型对象
                .build();
        ingestor.ingest(documents);

        return redisEmbeddingStore;
    }

    @Bean
    public ContentRetriever contentRetriever(/*EmbeddingStore store*/) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(redisEmbeddingStore) // 设置向量数据库操作对象
                .minScore(0.5) // 设置最小分数
                .maxResults(5) // 设置最大片段数量
                .embeddingModel(embeddingModel) // 设置向量模型对象
                .build();
    }
}
