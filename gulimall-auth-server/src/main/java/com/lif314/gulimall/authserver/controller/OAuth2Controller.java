package com.lif314.gulimall.authserver.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lif314.common.constant.AuthServerConstant;
import com.lif314.common.utils.HttpUtils;
import com.lif314.common.utils.R;
import com.lif314.gulimall.authserver.feign.MemberFeignService;
import com.lif314.gulimall.authserver.utils.GiteeHttpClient;
import com.lif314.common.to.MemberRespTo;
import com.lif314.gulimall.authserver.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理社交登录请求
 */
@Controller
public class OAuth2Controller {


    @Autowired
    MemberFeignService memberFeignService;

    @Value("${oauth.gitee.clientid}")
    private String client_id;
    @Value("${oauth.gitee.clientsecret}")
    private String client_secret;
    @Value("${oauth.gitee.redirecturi}")
    private String redirect_uri;


    @RequestMapping("/oauth2.0/gitee/success")
    public String giteeAuth(@RequestParam("code") String code, HttpSession session) {
        String host = "https://gitee.com";
        String method = "POST";
        String path = "/oauth/token";
        Map<String, String> headers = new HashMap<>();
//        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");

        // 根据code换取access token
        Map<String, String> querys = new HashMap<>();
        querys.put("grant_type", "authorization_code");
        querys.put("client_id", client_id);
        querys.put("code", code);
        querys.put("redirect_uri", redirect_uri);

        // bodys
        Map<String, String> bodys = new HashMap<>();
        bodys.put("client_secret", client_secret);

        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            if (response.getStatusLine().getStatusCode() == 200) {
                // 获取access_tocken成功
                String s = EntityUtils.toString(response.getEntity());
                JSONObject accessTokenJson = JSON.parseObject(s);
                String access_token = (String) accessTokenJson.get("access_token");
                System.out.println("access_tocken:" + access_token);
                // 建立社交账号与系统的关联
                // 获取用户信息
                String url = "https://gitee.com/api/v5/user?access_token=" + access_token;
                JSONObject userInfoJson = GiteeHttpClient.getUserInfo(url);
                /**
                 * 返回用户数据，大都是没有用的
                 * {
                 *     "id": 7812289,
                 *     "login": "lilinfei314",
                 *     "name": "lilinfei314",
                 *     "email": null
                 *     ...
                 * }
                 */
                // 用户唯一id标识
                Integer id = (Integer) userInfoJson.get("id"); // id类型是Integer
                SocialUser socialUser = new SocialUser();
                socialUser.setSocialUid(Long.parseLong(String.valueOf(id)));
                socialUser.setSocialType("gitee");

                // 远程社交登录
                R r = memberFeignService.oauthlogin(socialUser);
                if(r.getCode()== 0){
                    // 登录成功，回到首页
                    Object data =  r.get("data");
                    String toString = JSON.toJSON(data).toString();
                    MemberRespTo memberRespTo = JSON.parseObject(toString, MemberRespTo.class);
                    // System.out.println("用户信息:"+ data.toString());
                    // 子域的session也能让父域名使用：发卡的时候指定域名为父域
                    // 会将数据存在Redis中
                    // TODO 1. 默认发的令牌 session=asa, 作用域是当前域--> 解决子域共享的问题
                    // TODO 2. Redis中存储数据是JDK默认序列化，希望使用JSON序列化
                    session.setAttribute(AuthServerConstant.LOGIN_USER, memberRespTo);
                    return "redirect:http://feihong.com";
                }else{
                    // 登录失败，回到登录页
                    return "redirect:http://auth.feihong.com/login.html";
                }
            } else {
                // 获取失败，重新回到登录页
                return "redirect:http://auth.feihong.com/login.html";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:http://auth.feihong.com/login.html";
    }

}
