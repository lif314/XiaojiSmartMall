package com.lif314.gulimall.authserver.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lif314.common.constant.AuthServerConstant;
import com.lif314.common.exception.BizCodeEnum;
import com.lif314.common.to.MemberRespTo;
import com.lif314.common.to.SmsTo;
import com.lif314.common.utils.R;
import com.lif314.gulimall.authserver.feign.MemberFeignService;
import com.lif314.gulimall.authserver.feign.ThirdPartySerrvice;
import com.lif314.gulimall.authserver.vo.UserLoginVo;
import com.lif314.gulimall.authserver.vo.UserRegisterVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController  // 页面跳转， @RestController会将return结果放在body中，导致无法实现页面提交和跳转
@RequestMapping("/auth")
public class LoginController {

    /**
     * 调用第三方服务发送验证码
     */
    @Autowired
    ThirdPartySerrvice thirdPartySerrvice;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    /**
     *发送验证码
     * @param phone 电话号码
     * @return 返回验证码
     */
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone){
        // TODO 验证码接口防刷
        // TODO 验证码再次校验  Redis  key-phone value-code

        // 先从Redis中查找当前手机的验证码是否已经存在，以及是否超过60s
        String nowRedisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(StringUtils.isNotEmpty(nowRedisCode)){
            long time = Long.parseLong(nowRedisCode.split("_")[1]);
            if(System.currentTimeMillis() - time < 60000){
                // 验证码发送时间在60s以内，不能继续发送
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(),BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        // 生成6位随机数验证码
        String SYMBOLS = "0123456789"; // 数字
        Random RANDOM = new SecureRandom();
        char[] nonceChars = new char[6];
        for (int index = 0; index < nonceChars.length; ++index) {
            nonceChars[index] = SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length()));
        }
        // 6位随机数验证码
        String code =  new String(nonceChars);

        // 防刷：在redis中存入当前时间，下一次发送请求时，查看是否在60s内
        String redisCode = code + "_" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, redisCode, 10, TimeUnit.MINUTES);

        // 调用第三方服务发送验证码
        SmsTo smsTo = new SmsTo();
        smsTo.setPhone(phone);
        smsTo.setCode(code);
        R r =  thirdPartySerrvice.sendCode(smsTo);
        if(r.getCode() == 0){
            // 返回验证码给前端，进行校验
            return R.ok().put("data", code);
        }else{
            return R.error();
        }
    }

    /**
     * TODO  重定向携带数据，利用session原理，将数据放在session中
     */
    /**
     * 注册
     * @param vo 注册请求体
     * @param bindingResult 数据校验
     * @return  注册成功与失败
     */
    @PostMapping("/register")
    public  R register(@Valid @RequestBody UserRegisterVo vo , BindingResult bindingResult) {
        // 1. 进行数据校验
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = bindingResult.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            // 收集校验的错误信息返回
            return R.error().put("data", errors);
        }

        // 2. 后端校验验证码
        String code = vo.getCode();
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (StringUtils.isNotEmpty(redisCode)) {
            String realCode = redisCode.split("_")[0];
            if (realCode.equals(code)) {
                // 验证码通过，删除验证码  -- 令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());

                // TODO 验证成功 -- 调用远程服务注册
                // 3.调用远程服务进行注册
                // 需要判断用户名和手机号不能是已经存在的
                R r = memberFeignService.register(vo);
                if (r.getCode() != 0) {
                    // 调用失败, 返回注册页
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", r.get("msg").toString());
                    return R.error().put("data", errors);
                }else{
                    Object data = r.get("data");
                    String s = JSON.toJSONString(data);
                    MemberRespTo memberRespTo = JSON.parseObject(s, new TypeReference<MemberRespTo>() {
                    });
                    // 注册成功
                    return R.ok();
                }

            } else {
                // 验证码错误
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                return R.error().put("data", errors);
            }
        } else {
            // 验证码过期
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码已过期");
            return R.error().put("data", errors);
        }
    }


    /**
     * 登录
     * @param vo 登录数据
     * @param session session
     * @return 返回登录结果
     * @TODO token登录认证
     */
    @PostMapping("/login")
    public R login(@RequestBody UserLoginVo vo, HttpSession session){
        // 调用远程登录
        R r = memberFeignService.login(vo);
        if(r.getCode() == 0) {
            // 登录成功，回到首页
            Object data =  r.get("data");
            String toString = JSON.toJSON(data).toString();
            MemberRespTo memberRespTo = JSON.parseObject(toString, MemberRespTo.class);
            session.setAttribute(AuthServerConstant.LOGIN_USER, memberRespTo);
            // @TODO 暂时返回用户信息
            return R.ok().put("data", memberRespTo);
        }else{
            // 登录失败
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", r.get("msg").toString());
            return R.error().put("data", errors);
        }
    }

//    @GetMapping("/login.html")
//    public String loginPage(HttpSession session){
//        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
//        if(attribute == null){
//            // 回到登录页
//            return "login";
//        }else{
//            // 回到首页
//            return "redirect:http://feihong.com";
//        }
//    }
}
