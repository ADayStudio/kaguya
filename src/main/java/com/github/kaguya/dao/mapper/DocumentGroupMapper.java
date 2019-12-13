package com.github.kaguya.dao.mapper;

import com.github.kaguya.model.DocumentGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentGroupMapper extends BaseMapper<DocumentGroup> {

    /**
     * 获得分类id下所有一级文档，按order_id升序
     */
    List<DocumentGroup> getDocsParent(@Param("categoryId") Long categoryId, @Param("ROOT")Integer ROOT);

    /**
     * 获得文档id下所有文档，按order_id升序
     */
    List<DocumentGroup> getDocsGroup(@Param("documentId") Long documentId);
}