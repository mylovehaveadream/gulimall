package com.guigu.gulimall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@ToString
@Data
public class SpuItemAttrGroupVo {
    private String groupName;   //分组的名字
    private List<Attr> attrs;   //基本属性
}
