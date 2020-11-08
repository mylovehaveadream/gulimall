package com.guigu.gulimall.cart.controller;

import com.guigu.common.constant.AuthServerConstant;
import com.guigu.gulimall.cart.interceptor.CartInterceptor;
import com.guigu.gulimall.cart.service.CartService;
import com.guigu.gulimall.cart.vo.Cart;
import com.guigu.gulimall.cart.vo.CartItem;
import com.guigu.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    CartService cartService;

    //获取当前用户的所有购物项
    @GetMapping("/currentUserCartItems")
    @ResponseBody
    public List<CartItem> getCurrentUserCartItems(){
        return cartService.getUserCartItem();
    }


    /**
     *  去购物车页面的
     *
     *  浏览器有一个cookie：user-key(标识用户身份)，一个月后过期；
     *  如果第一次使用jd的购物车功能，都会给一个临时的用户的身份
     *  浏览器以后保存，每次访问都会带上这个cookie
     *
     *  登录：session有
     *  没有登录：按照cookie里面带来user-key来做
     *  第一次，如果没有临时用户，帮忙创建一个临时用户
     */
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {

        //1.快速得到用户信息，id,user-key
        //执行请求之前先过拦截器，拦截器过了就到controller了
        //在当前的线程有了
//        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();  //可以拿到拦截器共享的userInfoTo，别人也可以用

        Cart cart = cartService.getCart();
        model.addAttribute("cart",cart);
        return "cartList";
    }


    //添加商品到购物车
    /*
    RedirectAttributes ra:
        ra.addFlashAttribute():将数据放在session里面，可以在页面取出，但是只能取一次
        ra.addAttribute():将数据放在URL后面
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes ra) throws ExecutionException, InterruptedException {
        cartService.addToCart(skuId,num);
//        model.addAttribute("skuId",skuId);    //也可以把参数放在model里面，如果是转发就放在请求域中，如果是重定向就会拼到地址的后面
        //使用重定向携带数据
        ra.addAttribute("skuId",skuId); //重定向就会将数据自动放在URL后面
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }

    //跳转到成功页
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId,Model model){
        //重定向到成功页面，再次查询购物车数据即可,这样可以解决重复提交的问题
        CartItem item = cartService.geyCartItem(skuId);
        model.addAttribute("item",item);
        return "success";
    }

    //勾选某一个购物项
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Integer check){
        cartService.checkItem(skuId,check);
        return "redirect:http://cart.gulimall.com/cart.html";
    }


    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num) {
        cartService.changeItemCount(skuId,num);

        return "redirect:http://cart.gulimall.com/cart.html";
    }


    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimall.com/cart.html";
    }
}















