package com.guigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guigu.common.exception.NoStockException;
import com.guigu.common.to.mq.OrderTo;
import com.guigu.common.utils.R;
import com.guigu.common.vo.MemberRespVo;
import com.guigu.gulimall.order.constant.OrderConstant;
import com.guigu.gulimall.order.dao.OrderItemDao;
import com.guigu.gulimall.order.entity.OrderItemEntity;
import com.guigu.gulimall.order.entity.PaymentInfoEntity;
import com.guigu.gulimall.order.enume.OrderStatusEnum;
import com.guigu.gulimall.order.feign.CartFeignService;
import com.guigu.gulimall.order.feign.MemberFeignService;
import com.guigu.gulimall.order.feign.ProductFeignService;
import com.guigu.gulimall.order.feign.WmsFeignService;
import com.guigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.guigu.gulimall.order.service.OrderItemService;
import com.guigu.gulimall.order.service.PaymentInfoService;
import com.guigu.gulimall.order.to.OrderCreateTo;
import com.guigu.gulimall.order.vo.*;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guigu.common.utils.PageUtils;
import com.guigu.common.utils.Query;

import com.guigu.gulimall.order.dao.OrderDao;
import com.guigu.gulimall.order.entity.OrderEntity;
import com.guigu.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> submitVoThreadLocal = new ThreadLocal<>();

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    WmsFeignService wmsFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    PaymentInfoService paymentInfoService;

    /**
     * 订单确认页返回需要用的数据
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        //获取之前的请求
        //线程不一样，ThreadLocal里面的数据就不一样
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //1.远程查询所有的收货地址列表
            //每一个线程都来共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);   //在自己的线程来共享requestAttributes
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            confirmVo.setAddress(address);
        }, executor);


        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            //2.远程查询购物车所有选中的购物项
            //每一个线程都来共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);  //这样来解决Feign异步调用丢失请求头问题
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
            //feign在远程调用之前要构造请求，调用很多的拦截器RequestInterceptor interceptor:requestInterceptors
        }, executor).thenRunAsync(() -> {
            //批量查询商品的所有库存信息
            List<OrderItemVo> items = confirmVo.getItems(); //批量查询每一个商品的库存信息
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());

            //TODO 一定要启动库存服务，否则库存查不出
            R hasStock = wmsFeignService.getSkuHasStock(collect);
            List<SkuStockVo> data = hasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if (data != null) {
                Map<Long, Boolean> map =
                        data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        }, executor);


        //3.查询用户的积分
        Integer integration = memberRespVo.getIntegration();
        confirmVo.setIntegration(integration);

        //4.其他数据自动计算

        //TODO 5.防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        //给服务器放一个令牌
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(), token, 30, TimeUnit.MINUTES);

        confirmVo.setOrderToken(token); //给页面一个令牌

        CompletableFuture.allOf(getAddressFuture, cartFuture).get(); //等他们都完成，才给他们一个完整的返回

        return confirmVo;
    }

    //同一个对象内事务互调默认失效，原因：绕过了代理对象
    //事务使用代理对象来控制的
    //  REQUIRED、REQUIRES_NEW，事务的传播行为
    @Transactional(timeout = 30)  //a事务的所有设置就传播到了和它共用一个事务的方法
    public void a() {
        //这种模式下，b,c做任何设置都没用，都是和a共用一个事务
        //传播行为就是b、c这两个小事务，要不要与a共用一个事务
        //b();//a事务，a这个事务设置什么，b的就是什么，b的其他设置就没有效果了;与a共用事务
        //c();//新事务(不回滚)

        OrderServiceImpl orderService = (OrderServiceImpl) AopContext.currentProxy();   //拿到当前代理对象
        //使用代理对象调的，下面的设置才有用
        orderService.b();
        orderService.c();

        int i = 10 / 0;   //异常，a和b回滚，c不回滚
    }

    @Transactional(propagation = Propagation.REQUIRED, timeout = 2)  //REQUIRED需要事务,这个b一定要一个事务
    public void b() {
        //这样b与a共用一个事务
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)  //不与a共用一个事务
    public void c() {
    }


    /**
     * 下单
     *
     * @param vo
     * @return
     */
    //本地事务，在分布式系统，只能控制住自己的回滚，控制不了其他服务的回滚
    //分布式事务，最大原因：网络问题 + 分布式机器
    //mysql默认的级别(isolation = Isolation.REPEATABLE_READ)

    //高并发，不适合用它；
//    @GlobalTransactional    //全局的事务，就能用到seata的事务了，分支的事务不用这个全局的事务，用@Transactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        submitVoThreadLocal.set(vo);

        SubmitOrderResponseVo response = new SubmitOrderResponseVo();

        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        response.setCode(0);

        //1.验证令牌【令牌的对比和删除必须保证原子性】
        //这个脚本返回的是0（令牌失败）和1（删除成功）
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken(); //页面令牌

        //redisTemplate.execute(new DefaultRedisScript<返回值类型>(script,Long.class),KEYS[1](List), ARGV[1])
        //原子验证令牌和删除令牌
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()), orderToken);
        if (result == 0L) {
            //令牌验证失败
            response.setCode(1);
            return response;
        } else {
            //令牌验证成功
            //下单：去创建订单，验令牌，验价格，锁库存...
            //1.创建订单，订单项等信息
            OrderCreateTo order = createOrder();

            //2.验价
            BigDecimal payAmount = order.getOrder().getPayAmount();//订单自己算出的金额
            BigDecimal payPrice = vo.getPayPrice();

            //他们之间的相差范围小于0.01就算对比成功
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                //金额对比
                //TODO 3.保存订单
                saveOrder(order);
                //4.库存锁定,如果库存锁定失败了，还要撤销订单，整个可以是一个事务；只要有异常回滚订单数据
                //调用远程的库存服务，wms_ware_sku表
                //订单号，所有订单项（skuId,skuName,num）
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());

                List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(item.getSkuId());
                    itemVo.setCount(item.getSkuQuantity());
                    itemVo.setTitle(item.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(locks);
                //TODO 4.远程锁库存
                //库存成功了，但是网络原因超时了，订单回滚，库存不滚1

                //为了保证高并发，库存服务自己回滚，可以发消息给库存服务；
                //库存服务本身也可以使用自动解锁模式 消息队列来完成，使用消息队列来保证最终的一致性
                R r = wmsFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0) {
                    //锁成功了
                    response.setOrder(order.getOrder());

                    //TODO 5.远程扣减积分 出现异常
//                    int i = 10/0; //订单回滚，库存不滚
                    //TODO 订单创建成功发送消息给MQ
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                    return response;
                } else {
                    //锁定失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                }
            } else {
                response.setCode(2);
                return response;
            }
        }

//        String redisToken =
//                redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
//        if(orderToken != null && orderToken.equals(redisToken)) {
//            //令牌验证通过
//            redisTemplate.delete(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());//通过，删令牌
//        } else {
//            //不通过
//        }
    }

    //按照订单号获取订单
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity order_sn = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return order_sn;
    }

    @Override
    public void closeOrder(OrderEntity entity) {
        //查询当前这个订单的最新状态
        OrderEntity orderEntity = this.getById(entity.getId());
        //关单
        if (orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            OrderEntity update = new OrderEntity(); //创建一个新的对象，因为之前的对象属性可能发生了改变
            update.setId(entity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
            //发给MQ一个信息
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity, orderTo);
            try {
                //TODO 保证消息一定会发送出去，每一个消息都可以做好日志记录(给数据库保存每一个消息的详细信息)
                //TODO 定期扫描数据库将失败的消息再发送一遍
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.order", orderTo);
            } catch (Exception e) {
                //TODO 将没发送成功的消息进行重新发送
            }
        }
    }

    //获取当前订单的支付信息
    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity order = this.getOrderByOrderSn(orderSn);//查订单

        //支付宝的金额要精确两位数，数据库是四位的，要转
        BigDecimal bigDecimal = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);//小数点的位数,有小数就ROUND_UP向上取值
        payVo.setTotal_amount(bigDecimal.toString());
        payVo.setOut_trade_no(order.getOrderSn());

        List<OrderItemEntity> order_sn =
                orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity entity = order_sn.get(0);//把一个的名字当成标题
        payVo.setSubject(entity.getSkuName());
        payVo.setBody(entity.getSkuAttrsVals());
        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();//拦截器里面有会员id

        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberRespVo.getId()).orderByDesc("id")
        );

        List<OrderEntity> order_sn = page.getRecords().stream().map(order -> {
            //查出当前订单的所有订单项
            List<OrderItemEntity> itemEntities =
                    orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(itemEntities);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(order_sn);

        return new PageUtils(page);
    }

    //处理支付宝的支付结果
    @Override
    public String handlePayResult(PayAsyncVo vo) {
        //交易流水表oms_payment_info，这个表就是为了后来的对账
        //1.保存交易流水,一个订单对应一个流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());  //支付宝交易号
        infoEntity.setOrderSn(vo.getOut_trade_no());    //订单号
        infoEntity.setPaymentStatus(vo.getTrade_status());  //支付状态
        infoEntity.setCallbackTime(vo.getNotify_time());   //回调时间

        paymentInfoService.save(infoEntity);

        //2.修改订单的状态信息
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            //支付成功状态
            String outTradeNo = vo.getOut_trade_no(); //订单号
            this.baseMapper.updateOrderStatus(outTradeNo,OrderStatusEnum.PAYED.getCode());
        }
        return "success";
    }


    //保存订单数据
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());    //订单的修改时间
        this.save(orderEntity);   //oms_order,保存订单

        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems); //批量保存订单项
    }


    //创建订单
    private OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();
        //1.生成订单号
        String orderSn = IdWorker.getTimeId();
        //创建订单号
        OrderEntity orderEntity = buildOrder(orderSn);

        //2.获取所有的订单项
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);

        //3.计算价格、积分等相关
        computePrice(orderEntity, itemEntities);

        createTo.setOrder(orderEntity);
        createTo.setOrderItems(itemEntities);

        return createTo;
    }

    //计算价格相关
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        BigDecimal total = new BigDecimal("0.0");   //总价格

        //每一项优惠的东西进行汇总
        BigDecimal coupon = new BigDecimal("0.0");      //总优惠卷金额
        BigDecimal integration = new BigDecimal("0.0"); //总积分金额
        BigDecimal promotion = new BigDecimal("0.0");   //总打折金额

        BigDecimal gift = new BigDecimal("0.0"); //总积分信息
        BigDecimal growth = new BigDecimal("0.0"); //总成长值信息
        //订单的总额，叠加每一个订单项的总额信息
        for (OrderItemEntity entity : itemEntities) {
            coupon = coupon.add(entity.getCouponAmount());
            integration = integration.add(entity.getIntegrationAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            //当前订单项的真实金额
            total = total.add(entity.getRealAmount());

            gift = gift.add(new BigDecimal(entity.getGiftIntegration().toString()));
            growth = growth.add(new BigDecimal(entity.getGiftGrowth().toString()));//成长值信息
        }
        //1.订单价格相关
        orderEntity.setTotalAmount(total);  //订单总额
        //应付总额:订单总额+运费
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        //打折的总共优惠金额
        orderEntity.setPromotionAmount(promotion);
        //积分的总共优惠金额
        orderEntity.setIntegrationAmount(integration);
        //优惠卷的总共优惠金额
        orderEntity.setCouponAmount(coupon);

        //设置积分等信息
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());

        //删除状态
        orderEntity.setDeleteStatus(0); //未删除
    }


    //构建订单
    private OrderEntity buildOrder(String orderSn) {
        MemberRespVo respVo = LoginUserInterceptor.loginUser.get();
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        //获取会员的id
        entity.setMemberId(respVo.getId());

        OrderSubmitVo orderSubmitVo = submitVoThreadLocal.get();
        //远程获取收货地址信息
        R fare = wmsFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        //设置运费信息
        entity.setFreightAmount(fareResp.getFare());  //运费金额
        //设置收货人信息
        entity.setReceiverCity(fareResp.getAddress().getCity());
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        entity.setReceiverName(fareResp.getAddress().getName());
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        entity.setReceiverRegion(fareResp.getAddress().getRegion());

        //设置订单的相关状态信息
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        //自动确认的时间
        entity.setAutoConfirmDay(7);

        return entity;
    }

    //构建所有订单项数据
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //最后确定每个购物项的价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();

        if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);   //构建出每个的订单项
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    //构建某一个订单项数据
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();

        //1.订单信息，订单号,上面已有
        //2.商品的SPU信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setCategoryId(data.getCatalogId());

        //3.商品的SKU信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        // 把集合按照指定的分割符，变成一个字符串
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());

        //4.优惠信息【不做】
        //5.积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice()
                .multiply(new BigDecimal(cartItem.getCount().toString())).intValue());   //它的价格就决定送多少的积分,成长积分
        itemEntity.setGiftIntegration(cartItem.getPrice()
                .multiply(new BigDecimal(cartItem.getCount().toString())).intValue());    //购物积分

        //6.订单项的价格信息
        itemEntity.setPromotionAmount(new BigDecimal("0"));      //促销的优惠价
        itemEntity.setCouponAmount(new BigDecimal("0"));        //优惠卷，给一个默认的0
        itemEntity.setIntegrationAmount(new BigDecimal("0"));  //积分优惠
        //当前订单项的实际金额。总额减去各种优惠
        BigDecimal orign =
                itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));  //原来的价格
        //减去各种的优惠价格
        BigDecimal subtract = orign.subtract(itemEntity.getCouponAmount()).subtract(itemEntity.getPromotionAmount())
                .subtract(itemEntity.getIntegrationAmount());   //订单的实际金额
        itemEntity.setRealAmount(subtract);

        return itemEntity;
    }


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

}