package com.atguigu.gmall.sms.mapper;

import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 商品满减信息
 * 
 * @author Frz
 * @email Frz@atguigu.com
 * @date 2021-06-23 10:51:43
 */
@Mapper
@Repository
public interface SkuFullReductionMapper extends BaseMapper<SkuFullReductionEntity> {
	
}
