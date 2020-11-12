package com.guigu.common.to.mq;

import lombok.Data;

import java.util.List;

/**
 * 库存锁定成功的To
 */
@Data
public class StockLockedTo {
    private Long id;    //库存工作单的id
    private StockDetailTo detail;      //每一个仓库的商品锁定了几件,工作单详情
}
