package com.guigu.gulimall.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 商品三级分类
 * 
 * @author mylovehaveadream
 * @email mylovehaveadream@gmail.com
 * @date 2020-08-26 16:24:49
 */
@Data
@TableName("pms_category")
public class CategoryEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * $column.comments
	 */
	@TableId
	private Long catId;
	/**
	 * $column.comments
	 */
	private String name;
	/**
	 * $column.comments
	 */
	private Long parentCid;
	/**
	 * $column.comments
	 */
	private Integer catLevel;
	/**
	 * 0-显示，1-不显示
	 * 表中的逻辑删除字段和全局的配置是反的，那就定义自己的规则，如下
	 * value = "1"(逻辑不删除),delval = "0"(逻辑删除)
	 */
	@TableLogic(value = "1",delval = "0")  //代表这是一个逻辑删除字段
	private Integer showStatus;
	/**
	 * $column.comments
	 */
	private Integer sort;
	/**
	 * $column.comments
	 */
	private String icon;
	/**
	 * $column.comments
	 */
	private String productUnit;
	/**
	 * $column.comments
	 */
	private Integer productCount;

	//字段不为空的时候，才返回，不会带上空集合了
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	//它所有的子分类
	//因为子分类不是数据表里面的属性，所以使用@TableField表里面的属性，
	// exist = false说明属性在数据表里面不存在，我们加的自定义的属性
	@TableField(exist = false)
	private List<CategoryEntity> children;

}
