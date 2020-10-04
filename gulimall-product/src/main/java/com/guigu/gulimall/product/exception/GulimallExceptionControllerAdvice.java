package com.guigu.gulimall.product.exception;

import com.guigu.common.exception.BizCodeEnume;
import com.guigu.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 集中处理所有异常
 */
@Slf4j
//@ResponseBody
//basePackages处理哪个包下的异常
//@ControllerAdvice(basePackages = "com.guigu.gulimall.product.app")
//上面两个的结合
@RestControllerAdvice(basePackages = "com.guigu.gulimall.product.app")
public class GulimallExceptionControllerAdvice {


    //所有数据校验的异常，在这里处理
    @ExceptionHandler(value = MethodArgumentNotValidException.class)//告诉springmvc这个方法能处理什么样的异常
    public R handleValidException(MethodArgumentNotValidException e){
        //感知controller抛出来的异常
        log.error("数据校验出现问题,异常类型",e.getMessage(),e.getClass());
        //获取数据校验的错误结果
        BindingResult result = e.getBindingResult();
        Map<String,String> map = new HashMap<>();
            //获取校验的错误结果
        result.getFieldErrors().forEach((item)->{
            //FieldError 获取到错误提示
            String message = item.getDefaultMessage();
            //获取错误的属性的名字
            String field = item.getField();
            map.put(field,message);
        });
        return R.error(BizCodeEnume.VAILD_EXCEPTION.getCode(),BizCodeEnume.VAILD_EXCEPTION.getMsg()).put("data",map);
    }


    //处理任意类型的异常,来感知所有异常
    @ExceptionHandler(value = Throwable.class)
    public R handleException(Throwable throwable){
        return R.error(BizCodeEnume.UNKNOW_EXCEPTION.getCode(),BizCodeEnume.UNKNOW_EXCEPTION.getMsg());
    }
}
