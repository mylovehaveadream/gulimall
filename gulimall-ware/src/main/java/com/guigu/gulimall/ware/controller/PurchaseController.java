package com.guigu.gulimall.ware.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.guigu.gulimall.ware.vo.MergeVo;
import com.guigu.gulimall.ware.vo.PurchaseDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.guigu.gulimall.ware.entity.PurchaseEntity;
import com.guigu.gulimall.ware.service.PurchaseService;
import com.guigu.common.utils.PageUtils;
import com.guigu.common.utils.R;



/**
 * 采购信息
 *
 * @author mylovehaveadream
 * @email mylovehaveadream@gmail.com
 * @date 2020-08-26 19:19:52
 */
@RestController
@RequestMapping("ware/purchase")
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;


    //完成采购单
    @PostMapping("/done")
    public R finish(@RequestBody PurchaseDoneVo purchaseDoneVo){

        purchaseService.done(purchaseDoneVo);

        return R.ok();
    }


    //领取采购单
    @PostMapping("/received")
    public R received(@RequestBody List<Long> ids){

        purchaseService.received(ids);

        return R.ok();
    }

    //合并采购单
    @PostMapping("/merge")
    public R merge(@RequestBody MergeVo mergeVo){
        purchaseService.mergePurchase(mergeVo);
        return R.ok();
    }


    //查询未领取的采购单
    @RequestMapping("/unreceive/list")
    public R unreceivelist(@RequestParam Map<String, Object> params){
        PageUtils page = purchaseService.queryPageUnreceivePurchase(params);

        return R.ok().put("page", page);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = purchaseService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		PurchaseEntity purchase = purchaseService.getById(id);

        return R.ok().put("purchase", purchase);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody PurchaseEntity purchase){
        purchase.setCreateTime(new Date());
        purchase.setUpdateTime(new Date());
		purchaseService.save(purchase);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody PurchaseEntity purchase){
		purchaseService.updateById(purchase);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		purchaseService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
