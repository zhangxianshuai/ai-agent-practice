package com.zhang.aiagentpractice;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.collection.DropCollectionParam;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class RagServiceTest {

    // 注入 Spring AI 自动配置的 Embedding 模型（默认使用 DashScope）
    @Autowired
    private EmbeddingModel embeddingModel;

    // 注入 Spring AI 自动配置的 Milvus 向量库
    @Autowired
    private MilvusVectorStore vectorStore;

    // 注入 Milvus 客户端，用于删除旧 collection
    @Autowired
    private MilvusServiceClient milvusClient;

    @Test
    public void testEmbeddingAndStore() throws Exception {
        // 0. 删除旧的 collection，避免维度不一致（1536 vs 1024）导致写入失败
        try {
            milvusClient.dropCollection(
                DropCollectionParam.newBuilder()
                    .withCollectionName("vector_store")
                    .build()
            );
            System.out.println("🗑️ 已删除旧 collection: vector_store");
        } catch (Exception e) {
            System.out.println("⚠️ 删除 collection 失败（可能不存在）: " + e.getMessage());
        }

        // 0.1 重新触发 schema 初始化（initialize-schema 只在启动时执行一次，drop 后需手动重建）
        vectorStore.afterPropertiesSet();
        System.out.println("🔄 已重新初始化 collection: vector_store（维度: 1024）");

        // 1. 手动读取文件内容，彻底绕开 Tika/TextReader 的依赖问题
        String rawText = Files.readString(Path.of("test.txt"));
        // 转义 % 为 %%，防止 Document.getText() 内部 String.format 解析 % 字符时 NPE
        String safeText = rawText.replace("%", "%%");
        Document doc = new Document(safeText, Map.of("source", "test.txt"));
        List<Document> documents = List.of(doc);

        // 2. 切分文档
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(documents);

        // 3. 向量化并写入 Milvus
        vectorStore.add(chunks);

        System.out.println("✅ 成功将 " + chunks.size() + " 个文本块向量化并存入 Milvus！");
    }
}
