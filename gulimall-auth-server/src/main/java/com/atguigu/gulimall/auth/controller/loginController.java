package com.atguigu.gulimall.auth.controller;

import com.atguigu.common.to.SocialUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Controller
public class loginController {

    @RequestMapping(value = {"/login.html","/"})
    public String loginPage(HttpSession session) {
        return "login";
    }

    @RequestMapping(value = {"/reg.html"})
    public String regPage(HttpSession session) {
        return "reg";
    }
}
