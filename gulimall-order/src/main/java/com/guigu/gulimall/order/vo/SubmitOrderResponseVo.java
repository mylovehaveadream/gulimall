package com.guigu.gulimall.order.vo;

import com.guigu.gulimall.order.entity.OrderEntity;
import lombok.Data;

/**
 * 下单操作的返回数据
 */
@Data
public class SubmitOrderResponseVo {
    private OrderEntity order;
    private Integer code;   //0成功 错误状态码
}
