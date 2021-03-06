package com.guigu.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guigu.common.to.mq.OrderTo;
import com.guigu.common.to.mq.StockLockedTo;
import com.guigu.common.utils.PageUtils;
import com.guigu.gulimall.ware.entity.WareSkuEntity;
import com.guigu.gulimall.ware.vo.LockStockResult;
import com.guigu.gulimall.ware.vo.SkuHasStockVo;
import com.guigu.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author mylovehaveadream
 * @email mylovehaveadream@gmail.com
 * @date 2020-08-26 19:19:52
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVo vo);

    void unlockStock(StockLockedTo to);

    void unlockStock(OrderTo orderTo);

}

