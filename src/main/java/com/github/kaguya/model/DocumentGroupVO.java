package com.github.kaguya.model;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;

/**
 * 树形结构组
 */
@Data
@Accessors(chain = true)
public class DocumentGroupVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    private Long id;
    /**
     * 文档id
     */
    private Long documentId;
    /**
     * 父类id
     */
    private Long parentId;
    /**
     * 分类id
     */
    private Long categoryId;
    /**
     * 展示顺序id
     */
    private Integer orderId;
    /**
     * 标题
     */
    private String title;
    /**
     * 标题
     */
    private String label;
    /**
     * 子类
     */
    @Transient
    List<DocumentGroupVO> children = null;
}
