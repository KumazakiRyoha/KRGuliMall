package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedissonClient redisson;


    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model) {
        List<CategoryEntity> categoryList = categoryService.getLevel1();
        model.addAttribute("categorys", categoryList);
        return "index";
    }

    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        Map<String, List<Catelog2Vo>> catelogJson = categoryService.getCatelogJson();
        return catelogJson;
    }

    @ResponseBody
    @GetMapping("hello")
    public String hello() {
        RLock myLock = redisson.getLock("myLock");
        myLock.lock();

        try {
            System.out.println("加锁成功，执行业务..." + Thread.currentThread().getName());
            Thread.sleep(20000);
        } catch (Exception e) {

        } finally {
            System.out.println("释放锁..." + Thread.currentThread().getName());
            myLock.unlock();
        }
        return "hello";
    }

}
