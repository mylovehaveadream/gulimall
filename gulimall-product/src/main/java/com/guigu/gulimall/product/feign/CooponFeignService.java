package com.guigu.gulimall.product.feign;

import com.guigu.common.to.SkuReductionTo;
import com.guigu.common.to.SpuBoundTo;
import com.guigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-coupon")
public interface CooponFeignService {

    /*
        1.CooponFeignService.saveSpuBounds(spuBoundTo);
            1.@RequestBody将这个对象转为JSON
            2.找到gulimall-coupon服务，给/coupon/spubounds/save发送请求。
              将上一步转的JSON放在请求体位置，发送请求。
            3.对方服务收到请求。请求体里有JSON数据
              (@RequestBody SpuBoundsEntity spuBounds)：将请求体的JSON转为SpuBoundsEntity
              属性名有一一对应就能封装，传出去的数据类型和接收数据的类型可以不一致
        只要JSON数据模型是兼容的，双方服务无需使用同一个To
     */
    //与远程的接口，保证完整的签名
    @PostMapping("/coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundTo spuBoundTo);


    @PostMapping("/coupon/skufullreduction/saveinfo")
    R saveSkuReduction(@RequestBody SkuReductionTo skuReductionTo);
}
