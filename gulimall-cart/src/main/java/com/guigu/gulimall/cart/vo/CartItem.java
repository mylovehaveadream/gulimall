package com.guigu.gulimall.cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车里面的每一个购物项
 */
public class CartItem {
    private Long skuId;
    private Boolean check = true; //商品是否被选中
    private String title;
    private String image;
    private List<String> skuAttr;   //套餐信息
    private BigDecimal price;   //商品价格
    private Integer count;
    private BigDecimal totalPrice;  //总价

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Boolean getCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getSkuAttr() {
        return skuAttr;
    }

    public void setSkuAttr(List<String> skuAttr) {
        this.skuAttr = skuAttr;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    /**
     *  计算当前项的总价
     */
    public BigDecimal getTotalPrice() {
        //因为他是BigDecimal，所以相乘使用multiply，BigDecimal(字符串)
        return this.price.multiply(new BigDecimal("" + this.count));
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
