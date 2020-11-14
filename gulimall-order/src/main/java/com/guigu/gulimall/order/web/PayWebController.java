package com.guigu.gulimall.order.web;

import com.alipay.api.AlipayApiException;
import com.guigu.gulimall.order.config.AlipayTemplate;
import com.guigu.gulimall.order.service.OrderService;
import com.guigu.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PayWebController {
    @Autowired
    AlipayTemplate alipayTemplate;

    @Autowired
    OrderService orderService;

    /*
        1.将支付页让浏览器展示
        2.支付成功以后，我们要跳到用户的订单列表页
     */
    @ResponseBody
    @GetMapping(value = "/payOrder",produces = "text/html") //produces：产生那种类型的数据，产生的是html
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
//        PayVo payVo = new PayVo();
//        payVo.setOut_trade_no();    //订单号
//        payVo.setBody();    //订单备注
//        payVo.setSubject(); //订单的主题（标题）
//        payVo.setTotal_amount();    //订单金额

        PayVo payVo = orderService.getOrderPay(orderSn);  //订单的支付信息
        //返回的是一个页面，将此页面直接交给浏览器就行
        String pay = alipayTemplate.pay(payVo);
        return pay; //支付页面
    }
}
