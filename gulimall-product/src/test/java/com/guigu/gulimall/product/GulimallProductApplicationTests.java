package com.guigu.gulimall.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guigu.gulimall.product.entity.BrandEntity;
import com.guigu.gulimall.product.service.BrandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.UUID;

@SpringBootTest
public class GulimallProductApplicationTests {
    @Autowired
    BrandService brandService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    public void teststringRedisTemplate() {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        //保存
        ops.set("hello","world_"+ UUID.randomUUID().toString());

        //查询
        String s = ops.get("hello");

        System.out.println("之前保存的数据是："+s);
    }


    @Test
     public void contextLoads() {
//        BrandEntity brandEntity = new BrandEntity();
//        brandEntity.setName("华为");
//        brandService.save(brandEntity);
//
//        System.out.println("成功");

        //继承IService，有增删改查方法
        //查询,传的QueryWrapper是查询的条件
        List<BrandEntity> brand_id = brandService.list(
                new QueryWrapper<BrandEntity>().eq("brand_id", 2L));
        brand_id.forEach((item)->{
            System.out.println(item);
        });
    }

}
