package io.github.kabi.llama.llamaspringbootstarter.rag;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 文档类
 * 表示知识库中的文档对象
 */
@Data
@Builder
public class Document {
    
    /**
     * 文档唯一ID
     */
    private String id;
    
    /**
     * 文档内容
     */
    private String content;
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 文档元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 文档向量表示
     */
    private float[] embedding;
    
    /**
     * 文档来源
     */
    private String source;
}