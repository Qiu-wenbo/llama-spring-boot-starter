package io.github.kabi.llama.llamaspringbootstarter.rag;

import java.util.List;

/**
 * 知识库接口
 * 定义知识库的基本操作
 */
public interface KnowledgeBase {
    
    /**
     * 获取知识库名称
     */
    String getName();
    
    /**
     * 获取知识库类型
     */
    String getType();
    
    /**
     * 获取知识库路径
     */
    String getPath();
    
    /**
     * 根据查询文本检索相关文档
     * @param query 查询文本
     * @param topK 返回的文档数量
     * @param similarityThreshold 相似度阈值
     * @return 相关文档列表
     */
    List<Document> search(String query, int topK, double similarityThreshold);
    
    /**
     * 向知识库中添加文档
     * @param document 文档对象
     */
    void addDocument(Document document);
    
    /**
     * 向知识库中批量添加文档
     * @param documents 文档列表
     */
    void addDocuments(List<Document> documents);
    
    /**
     * 从知识库中删除文档
     * @param documentId 文档ID
     */
    void removeDocument(String documentId);
    
    /**
     * 清空知识库
     */
    void clear();
}