package com.guigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.guigu.common.to.mq.OrderTo;
import com.guigu.common.to.mq.StockDetailTo;
import com.guigu.common.to.mq.StockLockedTo;
import com.guigu.common.utils.R;
import com.guigu.common.exception.NoStockException;
import com.guigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.guigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.guigu.gulimall.ware.feign.OrderFeignService;
import com.guigu.gulimall.ware.feign.ProductFeignService;
import com.guigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.guigu.gulimall.ware.service.WareOrderTaskService;
import com.guigu.gulimall.ware.vo.OrderItemVo;
import com.guigu.gulimall.ware.vo.OrderVo;
import com.guigu.gulimall.ware.vo.SkuHasStockVo;
import com.guigu.gulimall.ware.vo.WareSkuLockVo;
import com.rabbitmq.client.Channel;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    @Autowired
    WareOrderTaskService orderTaskService;

    @Autowired
    WareOrderTaskDetailService orderTaskDetailService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    OrderFeignService orderFeignService;


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

    /**
     * 库存解锁的场景
     * 1.下订单成功，订单过期没有支付被系统自动取消、被用户手动取消。都要解锁库存
     * 2.下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。
     * 之前锁定的库存就要自动解锁
     */
    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        /**
         * 保存库存工作单的详情
         * 为了追溯
         */
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn()); //为哪个订单号锁的库存
        orderTaskService.save(taskEntity);

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
            //1.如果每一个商品都锁定成功，将当前商品锁定了几件的工作单记录发给MQ
            //2.锁定失败。前面保存的工作单信息就回滚了。发送出去的消息，即使要解锁记录，由于去数据库查不到id,所以就不用解锁
            for (Long wareId : wareIds) {
                //成功就返回1，否则就是0
                Long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());  //锁定sku的库存
                if (count == 1) {
                    //锁住了,锁住了就没有必要试其他仓库了
                    skuStocked = true;
                    //TODO 告诉MQ库存锁定成功
                    WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity(null, skuId, "",
                            hasStock.getNum(), taskEntity.getId(), wareId, 1);
                    orderTaskDetailService.save(entity);//锁成功的详情

                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(taskEntity.getId());
                    //属性对拷
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(entity, stockDetailTo);
                    //只发id还不够，应该发完整锁定的详细消息;防止回滚以后找不到数据
                    lockedTo.setDetail(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", lockedTo);
                    break;
                } else {
                    //当前仓库锁失败，重试下一个仓库
                }
            }

            if (skuStocked == false) {
                //当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }

        //3.能走到这，肯定全部都是锁定成功的

        return true;
    }

    //处理库存锁定的释放
    /**
     * 1.库存自动解锁
     *      下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。之前锁定的库存就要自动解锁
     *
     * 2.订单失败
     *      锁库存失败
     *
     *
     * 只要解锁库存的消息失败。一定要告诉服务解锁失败，开启手动ack机制
     */
    @Override
    public void unlockStock(StockLockedTo to) {
        StockDetailTo detail = to.getDetail();
        Long detailId = detail.getId();
        //解锁
        //1.查询数据库关于这个订单的锁定库存信息
        //有：证明库存锁定成功了
        //      解锁：要看订单情况
        //          1.没有这个订单，必须解锁
        //          2.有这个订单，不是解锁库存
        //              订单状态：已取消：解锁库存
        //                      没取消：不能解锁
        //没有：库存锁定失败，库存回滚了。这个情况无需解锁
        WareOrderTaskDetailEntity byId = orderTaskDetailService.getById(detailId);
        if (byId != null) {
            //解锁
            Long id = to.getId();//库存工作单的id
            WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();//根据订单号查询订单的状态
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                //订单数据返回成功
                OrderVo data = r.getData(new TypeReference<OrderVo>() {
                });

                if (data == null || data.getStatus() == 4) {
                    //订单不存在
                    //订单已经被取消了，才能解锁库存
                    if(byId.getLockStatus() == 1) {  //已锁定才能解锁
                        //当前库存工作单详情，状态为1 已锁定，但是未解锁才可以解锁
                        unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            } else {
                //远程服务失败，重新解锁
                throw new RuntimeException("远程服务失败");
            }
        } else {
            //无需解锁
        }
    }

    //防止订单服务卡顿，导致订单状态消息一直改不了，库存消息优先到期，查订单状态为新建状态，什么都不做就走了
    //导致卡顿的订单，永远不能解锁库存
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //查一下最新库存解锁的状态，防止重复解锁库存
        WareOrderTaskEntity task = orderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = task.getId();//库存工作单的id
        //按照工作单找到所有没有解锁的库存，进行解锁
        List<WareOrderTaskDetailEntity> entities = orderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", id)
                .eq("lock_status", 1));
        for (WareOrderTaskDetailEntity entity : entities) {
            //解锁
            unLockStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum(),entity.getId());
        }
    }

    private void unLockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        //库存解锁
        wareSkuDao.unLockStock(skuId, wareId, num);
        //更新库存工作单的状态
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetailId);
        entity.setLockStatus(2);    //变为已解锁
        orderTaskDetailService.updateById(entity);
    }

    //仓库拥有库存的数据
    @Data
    class SkuWareHasStock {
        private Long skuId; //商品的id
        private Integer num; //商品的数量
        private List<Long> wareId;  //仓库的id
    }
}