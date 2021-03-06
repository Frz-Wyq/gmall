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
        //??????categoryId??????0?????????????????????
        if (categoryId != 0){
            wrapper.eq("category_id",categoryId);
        }
        //???????????????
        String key = pageParamVo.getKey();
        if (StringUtils.isNotBlank(key)){
            //?????????wrapper?????????????????????????????????????????????and????????????????????????()
            //WHERE (category_id = ? AND (id = ? OR name LIKE ?))
            wrapper.and(t->t.eq("id",key).or().like("name",key));
        }
        IPage<SpuEntity> page = this.page(pageParamVo.getPage(),wrapper);
        return new PageResultVo(page);
    }

    @GlobalTransactional
    @Override
    public void bigSave(SpuVo spu) {

        //1?????????spu?????????3??????
        //1.1?????????pms_spu
        spu.setCreateTime(new Date());
        spu.setUpdateTime(spu.getCreateTime());
        this.save(spu);
        Long spuId = spu.getId();
        //1.2?????????pms_spu_desc
        List<String> spuImages = spu.getSpuImages();
        if (!CollectionUtils.isEmpty(spuImages)) {
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            spuDescEntity.setSpuId(spuId);
            spuDescEntity.setDecript(StringUtils.join(spuImages,","));
            this.spuDescMapper.insert(spuDescEntity);
        }
        //1.3?????????pms_spu_attr_value
        List<SpuAttrValueVo> spuBaseAttrs = spu.getBaseAttrs();
        if (!CollectionUtils.isEmpty(spuBaseAttrs)) {
            //???SpuAttrValueVo???????????????SpuAttrValueEntity??????
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

        //2?????????sku?????????3??????
        List<SkuVo> skus = spu.getSkus();
        if (CollectionUtils.isEmpty(skus)){
            return ;
        }
        //??????sku?????????
        skus.forEach(skuVo -> {
            //2.1?????????pms_sku
            skuVo.setSpuId(spuId);
            skuVo.setCategoryId(spu.getCategoryId());
            skuVo.setBrandId(spu.getBrandId());
            //??????????????????
            List<String> skuVoImages = skuVo.getImages();
            //?????????????????????????????????
            if (!CollectionUtils.isEmpty(skuVoImages)){
                //???????????????????????????????????????????????????????????????
                skuVo.setDefaultImage(StringUtils.isBlank(skuVo.getDefaultImage())?skuVoImages.get(0):skuVo.getDefaultImage());
            }
            this.skuMapper.insert(skuVo);
            Long skuVoId = skuVo.getId();

            //2.2?????????pms_sku_images
            if (!CollectionUtils.isEmpty(skuVoImages)){
                this.skuImagesService.saveBatch(skuVoImages.stream().map(image->{
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuVoId);
                    skuImagesEntity.setUrl(image);
                    //??????????????????????????????sku?????????????????????????????????1?????????0
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(skuVo.getDefaultImage() , image) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }
            //2.3?????????pms_sku_attr_value
            List<SkuAttrValueEntity> skuVoSaleAttrs = skuVo.getSaleAttrs();
            if (!CollectionUtils.isEmpty(skuVoSaleAttrs)){
                skuVoSaleAttrs.forEach(skuAttrValueEntity -> {
                            skuAttrValueEntity.setSkuId(skuVoId);
                });
                this.skuAttrValueService.saveBatch(skuVoSaleAttrs);
            }

            //3??????????????????????????????3??????
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo,skuSaleVo);
            skuSaleVo.setSkuId(skuVoId);
            this.smsClient.saleSales(skuSaleVo);
        });


    }

}