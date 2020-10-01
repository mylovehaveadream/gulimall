package com.guigu.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.guigu.gulimall.product.entity.ProductAttrValueEntity;
import com.guigu.gulimall.product.service.ProductAttrValueService;
import com.guigu.gulimall.product.vo.AttrGroupRelationVo;
import com.guigu.gulimall.product.vo.AttrRespVO;
import com.guigu.gulimall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.guigu.gulimall.product.entity.AttrEntity;
import com.guigu.gulimall.product.service.AttrService;
import com.guigu.common.utils.PageUtils;
import com.guigu.common.utils.R;



/**
 * 商品属性
 *
 * @author mylovehaveadream
 * @email mylovehaveadream@gmail.com
 * @date 2020-08-26 17:06:18
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

    @Autowired
    private ProductAttrValueService productAttrValueService;

    //修改商品规格
    @PostMapping("/update/{spuId}")
    public R updateSpuAttr(@PathVariable("spuId") Long spuId,
                           @RequestBody List<ProductAttrValueEntity> entities) {
        productAttrValueService.updateSpuAttr(spuId, entities);
        return R.ok();
    }


    //获取spu规格属性
    @RequestMapping("/base/listforspu/{spuId}")
    public R baseAttrlistforspu(@PathVariable("spuId") Long spuId) {

        List<ProductAttrValueEntity> entities =
                productAttrValueService.baseAttrlistforspu(spuId);

        return R.ok().put("data",entities);
    }





    // /sale/list/{catelogId}
    // /base/list/{catelogId}
    @RequestMapping("/{attrType}/list/{catelogId}")
    public R baseAttrList(@RequestParam Map<String, Object> params,
                          @PathVariable("catelogId") Long catelogId,
                          @PathVariable("attrType") String type){
        //1就是基本属性，否则是销售属性
        PageUtils page = attrService.queryBaseAttr(params,catelogId,type);
        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrId}")
    public R info(@PathVariable("attrId") Long attrId){
//		AttrEntity attr = attrService.getById(attrId);

        AttrRespVO respVO = attrService.getAttrInfo(attrId);

        return R.ok().put("attr", respVO);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrVo attr){
		attrService.saveAttr(attr);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrVo attr){
		attrService.updateAttr(attr);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrIds){
		attrService.removeByIds(Arrays.asList(attrIds));

        return R.ok();
    }


}











