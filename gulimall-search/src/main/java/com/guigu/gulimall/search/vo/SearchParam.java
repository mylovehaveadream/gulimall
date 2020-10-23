package com.guigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可能传递过来的查询条件
 *
 * 模拟一个查询条件：
 * catalog3Id=225&keyword=小米&sort=saleCount_asc&hasStock=0/1&brandId=1&brandId=2
 * 排序条件，只能选择某一个
 */
@Data
public class SearchParam {

    private String keyword;   //页面传递过来的全文匹配关键字

    private Long catalog3Id; //三级分类id

    /**
     * 只能选择一个
     * sort=saleCount_asc/desc
     * sort=skuPrice_asc/desc
     * sort=hotScore_asc/desc
     */
    private String sort;    //排序条件


    /**
     * 好多的过滤条件
     * hasStock(是否有货)、skuPrice价格区间、brandId、catalog3Id、attrs
     * hasStock=0/1
     * skuPrice=1_500（1到500以内）/_500（500以内）/500_（500以上）
     * brandId=1
     * attrs=1(系统)_其他:安卓&attrs=2(屏幕尺寸)_5寸:6寸
     */
    private Integer hasStock;//是否只显示有货 0（没有库存） 1（有库存）
    private String skuPrice;//价格区间查询
    private List<Long> brandId;//传多个品牌的id,按照品牌进行查询，可以多选
    private List<String> attrs;//按照属性进行筛选
    private Integer pageNum = 1;//页码

    private String _queryString;//原生的所有查询条件

}













