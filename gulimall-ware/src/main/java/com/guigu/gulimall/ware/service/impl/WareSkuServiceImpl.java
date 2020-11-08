package com.guigu.gulimall.ware.service.impl;

import com.guigu.common.utils.R;
import com.guigu.common.exception.NoStockException;
import com.guigu.gulimall.ware.feign.ProductFeignService;
import com.guigu.gulimall.ware.vo.OrderItemVo;
import com.guigu.gulimall.ware.vo.SkuHasStockVo;
import com.guigu.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guigu.common.utils.PageUtils;
import com.guigu.common.utils.Query;

import com.guigu.gulimall.ware.dao.WareSkuDao;
import com.guigu.gulimall.ware.entity.WareSkuEntity;
import com.guigu.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            wrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            wrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //1.判断如果没有这个库存记录，就新增
        List<WareSkuEntity> wareSkuEntities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>()
                .eq("sku_id", skuId)
                .eq("ware_id", wareId));
        if (wareSkuEntities == null || wareSkuEntities.size() == 0) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStockLocked(0);
            //TODO 远程查询sku名字,如果失败，整个事务无需回滚
            //1.自己catch异常
            //TODO 2. 还可以用什么办法让异常出现以后不回滚？
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    //成功
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {
                //远程调用失败了，不用管，继续往下执行，不回滚
            }

            wareSkuDao.insert(wareSkuEntity);
        } else {
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }

    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {

        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();

            //查询当前sku的总库存量
            //select sum(stock-stock_locked) from `wms_ware_sku` where sku_id=1
            //总的库存要减去占用的库存stock_locked
            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());

        return collect;
    }

    //为某个订单锁定库存
    //(rollbackFor = NoStockException.class),默认只要是运行时异常都会回滚
    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {

        //1.按照下单的收货地址，找到一个就近仓库，锁定库存(不用)

        //1.找到每个商品在哪个仓库都有库存(用它)
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            //商品在那些仓库有库存
            stock.setSkuId(skuId);
            //锁几件
            stock.setNum(item.getCount());
            //查询这个商品在哪里有库存
            List<Long> wareIds = wareSkuDao.listWareIdHasSkuStock(skuId);  //列出所有仓库的id
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        Boolean allLock = true; //是否全部都锁定了
        //2.锁定库存
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked = false; //当前商品是否锁住了
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                //没有任何仓库有这个商品的库存
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                //成功就返回1，否则就是0
                Long count = wareSkuDao.lockSkuStock(skuId,wareId,hasStock.getNum());  //锁定sku的库存
                if(count == 1) {
                    //锁住了,锁住了就没有必要试其他仓库了
                    skuStocked = true;
                    break;
                } else {
                    //当前仓库锁失败，重试下一个仓库
                }
            }

            if(skuStocked == false){
                //当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }

        //3.能走到这，肯定全部都是锁定成功的

        return true;
    }

    //仓库拥有库存的数据
    @Data
    class SkuWareHasStock {
        private Long skuId; //商品的id
        private Integer num; //商品的数量
        private List<Long> wareId;  //仓库的id
    }
}