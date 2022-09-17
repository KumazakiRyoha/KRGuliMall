package com.atguigu.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.gulimall.member.exception.PhoneNumExistException;
import com.atguigu.gulimall.member.exception.UserExistException;
import com.atguigu.gulimall.member.feign.CouponFeignService;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;



/**
 * 会员
 *
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2022-09-02 20:30:01
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    private CouponFeignService couponFeignService;


    @RequestMapping("/login")
    public R login(@RequestBody MemberLoginVo loginVo) {
        MemberEntity entity=memberService.login(loginVo);
        if (entity!=null){
            return R.ok().put("memberEntity",entity);
        }else {
            return R.error(BizCodeEnum.LOGINACCT_PASSWORD_EXCEPTION.getCode(), BizCodeEnum.LOGINACCT_PASSWORD_EXCEPTION.getMsg());
        }
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
 //   @RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
  //  @RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
 //   @RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
 //   @RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 注册会员
     * @return
     */
    @RequestMapping("/register")
    public R register(@RequestBody MemberRegisterVo registerVo) {
        try {
            memberService.register(registerVo);
        } catch (UserExistException userException) {
            return R.error(BizCodeEnum.USER_EXIST_EXCEPTION.getCode(), BizCodeEnum.USER_EXIST_EXCEPTION.getMsg());
        } catch (PhoneNumExistException phoneException) {
            return R.error(BizCodeEnum.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnum.PHONE_EXIST_EXCEPTION.getMsg());
        }
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
 //   @RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
