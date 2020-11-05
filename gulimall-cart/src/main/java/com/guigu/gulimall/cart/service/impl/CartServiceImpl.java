package com.guigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.guigu.common.utils.R;
import com.guigu.gulimall.cart.feign.ProductFeignService;
import com.guigu.gulimall.cart.interceptor.CartInterceptor;
import com.guigu.gulimall.cart.service.CartService;
import com.guigu.gulimall.cart.vo.Cart;
import com.guigu.gulimall.cart.vo.CartItem;
import com.guigu.gulimall.cart.vo.SkuInfoVo;
import com.guigu.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor executor;    //线程池

    private final String CART_PREFIX = "gulimall:cart:";

    //将商品添加到购物车
    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        //调用这个方法，来获取购物车
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String res = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(res)) {
            //购物车没有这个商品

            //2.添加新商品到购物车
            CartItem cartItem = new CartItem();

            //1.远程查询当前要添加的商品的信息
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                R skuInfo = productFeignService.getSkuInfo(skuId);
                SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {});

                cartItem.setCheck(true);
                cartItem.setCount(num);
                cartItem.setImage(data.getSkuDefaultImg());
                cartItem.setTitle(data.getSkuTitle());
                cartItem.setSkuId(skuId);
                cartItem.setPrice(data.getPrice());
            }, executor);


            //3.远程查询sku的组合信息
            CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(values);
            }, executor);

            CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrValues).get(); //阻塞等，等他们都完成，才执行下面的，不然会出异常

            String s = JSON.toJSONString(cartItem); //序列化的问题
            cartOps.put(skuId.toString(), s);    //放进redis中，有序列化问题，用JSON来解决

            return cartItem;
        } else {
            //购物车有这个商品，修改数量即可
            CartItem cartItem = JSON.parseObject(res, CartItem.class);//把JSON字符串逆转成对象
            cartItem.setCount(cartItem.getCount() + num);
            //更新redis
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));

            return cartItem;
        }

    }


    //获取购物车中某个购物项
    @Override
    public CartItem geyCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String str = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(str, CartItem.class);

        return cartItem;
    }

    //获取整个购物车
    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();

        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() != null) {
            //1.登录
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            //2.如果临时购物车的数据还没有进行合并[合并购物车]
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if(tempCartItems != null){
                //临时购物车有数据，需要合并
                for (CartItem item : tempCartItems) {
                    addToCart(item.getSkuId(),item.getCount());
                }

                //清除临时购物车的数据
                clearCart(tempCartKey);
            }
            //3.获取登录后的购物车的数据[包含合并过来的临时购物车的数据，和登录后的购物车的数据]
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        } else {
            //2.没有登录
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            //获取临时购物车的所有购物项
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }

        return cart;
    }

    //获取到我们要操作的购物车
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();  //同一次请求，在任何位置都能得到用户信息
        //决定redis的key
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            //登录了，就用登录购物车
            //gulimall:cart:1
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            //没有登录，就用临时购物车
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }

        //绑定一个Hash操作，以后对redis的增删改查都是针对这个key的增删改查
        //获取购物车信息
        return redisTemplate.boundHashOps(cartKey);
    }

    //获取购物车里面的所有购物项
    private List<CartItem> getCartItems(String cartKey){
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if (values != null && values.size() > 0) {
            List<CartItem> collect = values.stream().map((obj) -> {
                String str = (String) obj;
                CartItem cartItem = JSON.parseObject(str, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());

            return collect;
        }
        return null;
    }


    //清空购物车数据
    @Override
    public void clearCart(String cartKey){
        redisTemplate.delete(cartKey);
    }


    //勾选购物项
    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = geyCartItem(skuId);//获取当前购物车里面指定的购物项
        cartItem.setCheck(check==1?true:false);
        String s = JSON.toJSONString(cartItem);//最新状态的对象序列化到redis
        cartOps.put(skuId.toString(),s);
    }


    //修改购物项的数量
    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = geyCartItem(skuId);
        cartItem.setCount(num);

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(),JSON.toJSONString(cartItem));
    }

    //删除购物项
    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }


}











