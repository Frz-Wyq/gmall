package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.SkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * sku信息
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-06-22 21:49:42
 */
@Mapper
@Repository
public interface SkuMapper extends BaseMapper<SkuEntity> {
	
}
