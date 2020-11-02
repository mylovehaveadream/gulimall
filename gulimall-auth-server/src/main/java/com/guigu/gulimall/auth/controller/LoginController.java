package com.guigu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.guigu.common.constant.AuthServerConstant;
import com.guigu.common.exception.BizCodeEnume;
import com.guigu.common.utils.R;
import com.guigu.common.vo.MemberRespVo;
import com.guigu.gulimall.auth.feign.MemberFeignService;
import com.guigu.gulimall.auth.feign.ThirdPartFeignService;
import com.guigu.gulimall.auth.vo.UserLoginVo;
import com.guigu.gulimall.auth.vo.UserRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    @Autowired
    ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone){
        //TODO 1.接口防刷

        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(!StringUtils.isEmpty(redisCode)) {
            long l = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - l < 60000) {
                //60秒内不能再发
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(), BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        //2.验证码的再次校验，存在redis里面;存的key-phone,value-code  sms:code:166XXXXX -> 12345
        String code = UUID.randomUUID().toString().substring(0, 5);
        //随机验证码,加上系统的时间，给redis里面存
        String substring = code + "_" +System.currentTimeMillis();

        //设置它的有效时间,redis缓存验证码，防止同一个phone在60s内再次发送验证码,加上系统的时间，给redis里面存,前端做的校验不安全
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone,substring,10, TimeUnit.MINUTES);

        thirdPartFeignService.sendCode(phone,code);

        return R.ok();
    }

    /*
        RedirectAttributes:模拟重定向携带数据
        TODO 重定向携带数据，利用session原理。将数据放在session中，只要跳到下一个页面取出这个数据以后，session里面的数据就会删除

        TODO 1.分布式下的session问题
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result,
                         RedirectAttributes redirectAttributes){ //BindingResult封装校验的结果
        if(result.hasErrors()){
            //获取所有属性的错误信息
            /**
             * .map(fieldError -> {
             *      String field = fieldError.getField();   //哪个字段出现了错误
             *      String defaultMessage = fieldError.getDefaultMessage(); //它的错误信息是什么，错误提示
             *      errors.put(field,defaultMessage);
             *      return
             * });
             */
            Map<String, String> errors = result.getFieldErrors().stream().collect(
                    Collectors.toMap(FieldError::getField , FieldError::getDefaultMessage));//这样可以直接变成一个map,指定了key和value

//            model.addAttribute("errors",errors); 重定向获取不到请求域中的数据，可以使用下面的方式来获取
            redirectAttributes.addFlashAttribute("errors",errors);  //这个属性只需要取一次就行了，这个允许重定向进行访问

            //报错信息：Request method 'POST' not supported
            //用户注册-->/regist[post]-->转发/reg.html（路径映射默认都是get方式访问的）（类似于用post的方式给get的方式发了一个请求，所以post不支持了）
            //转发：原请求原封不动的转给下一个人

            //校验出错，转发到注册页
//            return "forward:/reg.html"; 不用转发了,会有上面的问题
            return "redirect:http://auth.gulimall.com/reg.html";    //所以重定向携带数据就是模拟session,跨页面共享可以给session里放
            //使用重定向来解决表单重复提交的问题，但是重定向获取不到请求域中的数据，重定的共享数据RedirectAttributes
        }


        //1.校验验证码
        String code = vo.getCode();

        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if(!StringUtils.isEmpty(s)) {
            if(code.equals(s.split("_")[0])) {
                //删除验证码,令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());

                //验证码通过  //真正注册，调用远程服务进行注册
                R r = memberFeignService.regist(vo);
                if(r.getCode() == 0) {
                    //成功
                    return "redirect:http://auth.gulimall.com/login.html";
                } else {
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors",errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code","验证码错误");
                redirectAttributes.addFlashAttribute("errors",errors);  //这个属性只需要取一次就行了，这个允许重定向进行访问

                return "redirect:http://auth.gulimall.com/reg.html";
            }
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code","验证码错误");
            redirectAttributes.addFlashAttribute("errors",errors);  //这个属性只需要取一次就行了，这个允许重定向进行访问

            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }


    @PostMapping("/login")
    public String login(UserLoginVo vo,RedirectAttributes redirectAttributes,
            HttpSession session){//这里不能写@RequestBody，因为表单提交过来的是k、v，不是JSON数据

        //远程登录,调用远程的要把UserLoginVo转为json
        R login = memberFeignService.login(vo);
        if(login.getCode() == 0){
            //成功
            MemberRespVo data = login.getData("data", new TypeReference<MemberRespVo>() {});
            //登录成功，放在session中
            session.setAttribute(AuthServerConstant.LOGIN_USER,data);
            return "redirect:http://gulimall.com";
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg",login.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(attribute == null){
            //没有登录
            return "login";
        } else {
            //登录了跳回首页
            return "redirect:http://gulimall.com";
        }
    }


    /**
     * 发送一个请求直接跳转到一个页面
     *  springMVC viewcontroller:将请求和页面映射过来，就不用写下面的这些空方法了
     */

//    @GetMapping("/login.html")
//    public String loginPage(){
//        return "login";
//    }
//
//    @GetMapping("/reg.html")
//    public String regPage(){
//        return "reg";
//    }

}
