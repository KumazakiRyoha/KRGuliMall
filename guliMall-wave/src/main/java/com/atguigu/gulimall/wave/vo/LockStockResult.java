package com.atguigu.gulimall.wave.vo;

import lombok.Data;

@Data
public class LockStockResult {

    private Long skuId;
    private Integer num;
    private boolean lock;
}
