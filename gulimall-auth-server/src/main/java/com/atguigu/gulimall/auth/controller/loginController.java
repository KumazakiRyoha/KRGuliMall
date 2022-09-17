package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Controller
public class loginController {

    @Resource
    private MemberFeignService memberFeignService;

    @RequestMapping(value = {"/login.html","/","/login"})
    public String loginPage(HttpSession session) {
        return "login";
    }

    @RequestMapping(value = {"/reg.html"})
    public String regPage(HttpSession session) {
        return "reg";
    }


    @PostMapping("/register")
    public String register(@Valid UserRegisterVo registerVo, BindingResult result, RedirectAttributes attributes) {
        //1.判断校验是否通过
        Map<String, String> errors = new HashMap<>();
        if (result.hasErrors()){
            //1.1 如果校验不通过，则封装校验结果
            result.getFieldErrors().forEach(item->{
                errors.put(item.getField(), item.getDefaultMessage());
                //1.2 将错误信息封装到session中
                attributes.addFlashAttribute("errors", errors);
            });
            //1.2 重定向到注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        }else {
            //2.若JSR303校验通过
            //2.1.1 使得验证后的验证码失效
            //2.1.2 远程调用会员服务注册
            R r = memberFeignService.register(registerVo);
            if (r.getCode() == 0) {
                //调用成功，重定向登录页
                return "redirect:http://auth.gulimall.com/login.html";
            }else {
                //调用失败，返回注册页并显示错误信息
                String msg = (String) r.get("msg");
                errors.put("msg", msg);
                attributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        }
    }

    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes attributes, HttpSession session){
        R r = memberFeignService.login(vo);
        if (r.getCode() == 0) {
            String jsonString = JSON.toJSONString(r.get("memberEntity"));
            MemberResponseVo memberResponseVo = JSON.parseObject(jsonString, new TypeReference<MemberResponseVo>() {
            });
            session.setAttribute(AuthServerConstant.LOGIN_USER, memberResponseVo);
            return "redirect:http://gulimall.com/";
        }else {
            String msg = (String) r.get("msg");
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", msg);
            attributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }
}
