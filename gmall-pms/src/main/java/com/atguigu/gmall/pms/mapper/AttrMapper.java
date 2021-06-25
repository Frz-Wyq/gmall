package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 商品属性
 * 
 * @author Frz
 * @email Frz@atguigu.com
 * @date 2021-06-23 13:52:48
 */
@Mapper
@Repository
public interface AttrMapper extends BaseMapper<AttrEntity> {
	
}
