package com.guigu.gulimall.order.dao;

import com.guigu.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author mylovehaveadream
 * @email mylovehaveadream@gmail.com
 * @date 2020-08-26 19:11:15
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
