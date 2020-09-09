package com.guigu.gulimall.coupon.controller;

import java.util.Arrays;
import java.util.Map;

import com.guigu.common.to.SkuReductionTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.guigu.gulimall.coupon.entity.SkuFullReductionEntity;
import com.guigu.gulimall.coupon.service.SkuFullReductionService;
import com.guigu.common.utils.PageUtils;
import com.guigu.common.utils.R;



/**
 * 商品满减信息
 *
 * @author mylovehaveadream
 * @email mylovehaveadream@gmail.com
 * @date 2020-08-26 18:25:33
 */
@RestController
@RequestMapping("coupon/skufullreduction")
public class SkuFullReductionController {
    @Autowired
    private SkuFullReductionService skuFullReductionService;


    @PostMapping("/saveinfo")
    public R saveinfo(@RequestParam SkuReductionTo skuReductionTo){
        skuFullReductionService.saveSkuReduction(skuReductionTo);

        return R.ok();
    }



    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = skuFullReductionService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		SkuFullReductionEntity skuFullReduction = skuFullReductionService.getById(id);

        return R.ok().put("skuFullReduction", skuFullReduction);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody SkuFullReductionEntity skuFullReduction){
		skuFullReductionService.save(skuFullReduction);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody SkuFullReductionEntity skuFullReduction){
		skuFullReductionService.updateById(skuFullReduction);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		skuFullReductionService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
