package com.guigu.gulimall.search.vo;

import lombok.Data;

@Data
public class AttrResponseVo {
    /**
     * $column.comments
     */
    private Long attrId;
    /**
     * $column.comments
     */
    private String attrName;
    /**
     * $column.comments
     */
    private Integer searchType;
    /**
     * $column.comments
     */
    private String icon;
    /**
     * $column.comments
     */
    private String valueSelect;
    /**
     * $column.comments
     */
    private Integer attrType;
    /**
     * $column.comments
     */
    private Long enable;
    /**
     * $column.comments
     */
    private Long catelogId;
    /**
     * $column.comments
     */
    private Integer showDesc;

    private Long attrGroupId;

    //分类的名字
    private String catelogName;
    //分组的名字
    private String groupName;

    private Long[] catelogPath;

}
