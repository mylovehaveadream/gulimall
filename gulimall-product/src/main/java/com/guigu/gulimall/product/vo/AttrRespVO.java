package com.guigu.gulimall.product.vo;

import lombok.Data;

@Data
public class AttrRespVO extends AttrVo{
    //所属分类的名字：“手机/数码/手机”
    //所属分组的名字：主体

    //分类的名字
    private String catelogName;
    //分组的名字
    private String groupName;


    private Long[] catelogPath;

}
