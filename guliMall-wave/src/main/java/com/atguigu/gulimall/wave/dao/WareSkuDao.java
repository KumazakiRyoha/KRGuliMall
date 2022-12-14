package com.atguigu.gulimall.wave.dao;

import com.atguigu.gulimall.wave.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * εεεΊε­
 * 
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2022-09-02 20:49:41
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    void addStock(Long skuId, Long wareId, Integer skuNum);

    Long getSkuStock(Long skuId);
}
