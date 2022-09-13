package com.atguigu.gulimall.product.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimall-wave")
public interface WareFeignService {
    @PostMapping("/wave/waresku/hasstock")
    R getSkuHasStock(@RequestBody List<Long> skuIds);
}
