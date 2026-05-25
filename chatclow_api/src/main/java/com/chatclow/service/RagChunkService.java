package com.chatclow.service;

import com.chatclow.entity.RagChunk;
import java.util.List;

public interface RagChunkService {

    /**
     * 新增一条切片
     */
    boolean add(RagChunk chunk);

    /**
     * 按知识库ID查所有切片
     */
    List<RagChunk> listByKbId(Long kbId);

    /**
     * 按文档ID查所有切片
     */
    List<RagChunk> listByDocumentId(Long documentId);

    /**
     * 批量保存切片（文档切分后一次插入多条）
     */
    int batchInsert(List<RagChunk> chunks);

    /**
     * 按ID查询单条
     */
    RagChunk getById(Long id);

    /**
     * 删除某文档的所有切片（删除文档时级联用）
     */
    int deleteByDocumentId(Long documentId);
}
