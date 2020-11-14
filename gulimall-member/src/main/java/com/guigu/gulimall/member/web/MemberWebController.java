package com.guigu.gulimall.member.web;

import com.guigu.common.utils.R;
import com.guigu.gulimall.member.feign.OrderFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 每个新的项目考虑的统一的配置
 * 1.spring-session依赖
 * 2.spring-session配置
 * 3.引入LoginInterceptor、WebMvcConfigurer
 */
@Controller
public class MemberWebController {

    @Autowired
    OrderFeignService orderFeignService;

    //订单分页查询
    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum,
                                  Model model, HttpServletRequest request){
        //获取到支付宝给我们传来的所有请求数据
        //验证签名，如果正确可以去修改


        //查出当前登录的用户的所有订单列表数据
        Map<String,Object> page = new HashMap<>();
        page.put("page",pageNum.toString());   //去第几页，查的时候用的是page
        R r = orderFeignService.listWithItem(page);
        model.addAttribute("orders",r);
        return "orderList";
    }
}
