package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.SpuService;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {
    @Autowired
    private SpuDescMapper spuDescMapper;

    @Autowired
    private SpuAttrValueService spuAttrValueService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private GmallSmsClient smsClient;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuByCidAndPage(PageParamVo pageParamVo, Long categoryId) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        //如果categoryId不为0，需要查询本类
        if (categoryId != 0){
            wrapper.eq("category_id",categoryId);
        }
        //关键字查询
        String key = pageParamVo.getKey();
        if (StringUtils.isNotBlank(key)){
            //如果在wrapper后面直接写条件，条件之间默认是and关系，并且都没有()
            //WHERE (category_id = ? AND (id = ? OR name LIKE ?))
            wrapper.and(t->t.eq("id",key).or().like("name",key));
        }
        IPage<SpuEntity> page = this.page(pageParamVo.getPage(),wrapper);
        return new PageResultVo(page);
    }

    @GlobalTransactional
    @Override
    public void bigSave(SpuVo spu) {

        //1、保存spu相关的3张表
        //1.1、保存pms_spu
        spu.setCreateTime(new Date());
        spu.setUpdateTime(spu.getCreateTime());
        this.save(spu);
        Long spuId = spu.getId();
        //1.2、保存pms_spu_desc
        List<String> spuImages = spu.getSpuImages();
        if (!CollectionUtils.isEmpty(spuImages)) {
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            spuDescEntity.setSpuId(spuId);
            spuDescEntity.setDecript(StringUtils.join(spuImages,","));
            this.spuDescMapper.insert(spuDescEntity);
        }
        //1.3、保存pms_spu_attr_value
        List<SpuAttrValueVo> spuBaseAttrs = spu.getBaseAttrs();
        if (!CollectionUtils.isEmpty(spuBaseAttrs)) {
            //把SpuAttrValueVo集合转化为SpuAttrValueEntity集合
            List<SpuAttrValueEntity> spuAttrValueEntities = spuBaseAttrs.stream().filter(
                    spuAttrValueVo -> spuAttrValueVo.getAttrValue()!=null
            ).map(spuAttrValueVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                spuAttrValueEntity.setSpuId(spuId);
                BeanUtils.copyProperties(spuAttrValueVo, spuAttrValueEntity);
                return spuAttrValueEntity;
            }).collect(Collectors.toList());
            this.spuAttrValueService.saveBatch(spuAttrValueEntities);
        }

        //2、保存sku相关的3张表
        List<SkuVo> skus = spu.getSkus();
        if (CollectionUtils.isEmpty(skus)){
            return ;
        }
        //遍历sku并保存
        skus.forEach(skuVo -> {
            //2.1、保存pms_sku
            skuVo.setSpuId(spuId);
            skuVo.setCategoryId(spu.getCategoryId());
            skuVo.setBrandId(spu.getBrandId());
            //获取图片列表
            List<String> skuVoImages = skuVo.getImages();
            //判断第一张图片是否为空
            if (!CollectionUtils.isEmpty(skuVoImages)){
                //如果用户不设置图片设置第一张图片为默认图片
                skuVo.setDefaultImage(StringUtils.isBlank(skuVo.getDefaultImage())?skuVoImages.get(0):skuVo.getDefaultImage());
            }
            this.skuMapper.insert(skuVo);
            Long skuVoId = skuVo.getId();

            //2.2、保存pms_sku_images
            if (!CollectionUtils.isEmpty(skuVoImages)){
                this.skuImagesService.saveBatch(skuVoImages.stream().map(image->{
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuVoId);
                    skuImagesEntity.setUrl(image);
                    //如果当前图片的地址和sku默认的图片地址相同即为1否则为0
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(skuVo.getDefaultImage() , image) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }
            //2.3、保存pms_sku_attr_value
            List<SkuAttrValueEntity> skuVoSaleAttrs = skuVo.getSaleAttrs();
            if (!CollectionUtils.isEmpty(skuVoSaleAttrs)){
                skuVoSaleAttrs.forEach(skuAttrValueEntity -> {
                            skuAttrValueEntity.setSkuId(skuVoId);
                });
                this.skuAttrValueService.saveBatch(skuVoSaleAttrs);
            }

            //3、保存销售信息相关的3张表
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo,skuSaleVo);
            skuSaleVo.setSkuId(skuVoId);
            this.smsClient.saleSales(skuSaleVo);
        });


    }

}