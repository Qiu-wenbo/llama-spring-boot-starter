package io.github.kabi.llama.llamaspringbootstarter.feature;

import io.github.kabi.llama.llamaspringbootstarter.aiservice.AiServices;
import io.github.kabi.llama.llamaspringbootstarter.rag.KnowledgeBase;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG功能注册器
 * 用于为智能体添加RAG功能和知识库
 */
public class RagFeatureRegistry extends AbstractFeatureRegistry {
    
    private final List<KnowledgeBase> knowledgeBases = new ArrayList<>();
    
    public RagFeatureRegistry() {
        super("rag");
    }
    
    /**
     * 添加知识库
     */
    public RagFeatureRegistry addKnowledgeBase(KnowledgeBase knowledgeBase) {
        if (knowledgeBase != null) {
            this.knowledgeBases.add(knowledgeBase);
        }
        return this;
    }
    
    /**
     * 获取所有知识库
     */
    public List<KnowledgeBase> getKnowledgeBases() {
        return new ArrayList<>(knowledgeBases);
    }
    
    @Override
    public void registerTo(AiServices aiServices) {
        if (!isEnabled()) {
            return;
        }
        
        aiServices.enableRAG(true);
        
        // 注册所有知识库
        for (KnowledgeBase knowledgeBase : knowledgeBases) {
            aiServices.addKnowledgeBase(knowledgeBase);
        }
    }
}