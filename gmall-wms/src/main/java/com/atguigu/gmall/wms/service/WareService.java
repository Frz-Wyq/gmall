package com.atguigu.gmall.wms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.wms.entity.WareEntity;

import java.util.Map;

/**
 * 仓库信息
 *
 * @author Frz
 * @email Frz@atguigu.com
 * @date 2021-06-24 15:25:10
 */
public interface WareService extends IService<WareEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

