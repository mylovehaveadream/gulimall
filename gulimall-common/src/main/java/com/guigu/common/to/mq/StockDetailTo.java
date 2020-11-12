package com.guigu.common.to.mq;

import lombok.Data;

@Data
public class StockDetailTo {

    private Long id;

    private Long skuId;

    private String skuName;

    private Integer skuNum;

    private Long taskId;

    /**
     * 仓库id
     */
    private Long wareId;

    /**
     *	锁定状态
     */
    private Integer lockStatus;
}
