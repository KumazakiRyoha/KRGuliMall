package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * spu信息
 * 
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2022-09-02 17:32:08
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {
    void updateSpuStatus(@Param("spuId") Long spuId, @Param("code") int code);
}
