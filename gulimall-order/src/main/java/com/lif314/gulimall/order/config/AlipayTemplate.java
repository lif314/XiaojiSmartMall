package com.lif314.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.lif314.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2021000119642680";

    // 商户私钥，您的PKCS8格式RSA2私钥 -- 应用私钥
    // 私钥自己加密
    private  String merchant_private_key = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCDC30QxLN8CMeGrp6n2p5GAh3vxHscVgiVdiwvMkbnVZjDdsDLESE/1vgSC+d9aVF0wLkTKamQcWPTCnMhMSquVet+eU51CWgdoJzKHErL9w7IDakmmhy8A4GWvp/8wfbRdpyw2nX7o4l2VaZPA8KSYdX39rsXgZYZYyKfhiVZHQsAMke0UhWTMFJ/ZNwsA6jTt38a/0nTWPHnarMoGcwI+odqEixgKtvWnVu36qtUoSuBojaZPJ1ZQs+rtpV8E1Ij7I6l50WczEiybsBA1QjDCyYkzujp/Cfco0RaC3fkbFGZOIxWc5sgIJr1Pav0tF65DtBzX5DRXcJMX+Z/7OJDAgMBAAECggEADWrBtVPOZKapdxqt19XEWrqgwgnF6RLN8i+VOofVRp4289xWxPPOCbFC2OYrH2YHFwMcb9/WIpnZdKQfQYi9K/hUEjDtXYJHDLGDHBZlehijhMwRQ4iH5KAfRoXh74kUckHIrTrQ1aDa1CjUzEZAR6iItcinQzLbYWILC3Y6oLL48ERujNWawwIg0jMy2imYjSjE/IopU4wNmBiqb3JxNVypr1M8C5NVfJswWzqx2+7JL//cdVZGZHOl0umSa7T0khwzXGKw3KpX+VRrl79SMmsNR1UK299XkwKzQwdJr2CdOQMWHwJytnJG50l87dzjW2C6k6vQyJt/j5OxfZLsYQKBgQDFArgS4y781boyitftOyOud9Ei9fdDREfG4P6D7A8yBA28OKb/J85njq8S4z5H6hLf3H9Hwz6MNhmv6pNdHR1di/N8iLx0wMXz9du/cf1anbG9c4ont2Dshs21L8722eJlo9rvKU4jfjMpEQIXlHRaDe6q4iQHWy5AQ1/8n06gKQKBgQCqSFsLB8PO7kMVfTXUkGjXE+UOpWhABuNXlLg9RSoXFsj3mjmt6gVGRZ2rtG7YhNsqzetKM9GEBgLPFGiHmh7Ee0DRFJ6FfuNIdtE7Zy/QQ4SbzS8AMJ1jr8lDjtxB6ZBieQ38UuUnU5oRw91phJPn987O8m3N0F295I4G+ioMiwKBgQCH4nc6N86J/5S9Fb0XWMokpWnvtj2E/QyQ5VFHhsN4WQlsQJsWtMsN7IFfArRLf+zi0UBNLCLxLMOM7bwU5mcBxez8gYeS8//VZEfchXAdV1Vn+dBZ0occvWIXTdFvvBsGxfzVJuVJX9tDwQJzZybVknSDExWMqVx+YLpNEUh2+QKBgQCNW9WgmYiWjzrHaxM168/sxEpB11Rs2or+GKq9yl3/nt2klrZNRtmsFXynnvjp65iZtyodhljm1aqdR97iuzHRgFQ1fX1/au+5J1HYk3eg13wNbs8WxETUDYjx+XWljgRbiwzuK86p0AA1wThcMUArsI9RQvWehtxbGJJC/ofLswKBgF2W291gv4qkGu8LI0W5ORVNzQglW/Pmb05XJE15O+z2J+Od8L0MEmg7BuqxUgAKCDrGQSxkfTKrfMPQtcSpgwrIcyypCTT3WUL73KzFAJ9yeWfb7w1brhZQxjcGLdXWrs63n4pXzS/hZ5n6sSeulvlO5cKikxyIyeSdbWjhnAbp";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiLxAWn3YfthrljmeY/UxphqGLSFEGaObRt0CzJr9jtH+YhaYqwcNiLnD0g+OI3nL7pKoJRiUNkNmDVHo50lFczjRjIBez3GkiG3eqHZc1OP5QXBSW2WmokLpwCmFLx33bQ1NqwUOY06lreKlEDnFnuJQ6rsYBlpR6XK+jPffzCKHsFEWkv1LRUvIES4CBCZg60XfNYTwC/MEKCaucRESwcbthxAyo1+UcQ+1ZcmA8cxuJpYcIEyovQ3/BJFnTJX9aU8AZV+2r+8k7f+nFxOhBLKTNXl2FCWjykDuyIsa2SO+08s0JJk9hmiLMswpbQ0/UldVLyNeD/SyCxXMPHTOGwIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问

    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    // 异步通知
    private  String notify_url = "http://b734ouo58c.shhttp.cn/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 同步通知，支付成功，一般跳转到成功页
    private  String return_url = "http://order.feihong.com/memberOrder.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 限时收单
    private String timeout = "30m";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+ timeout + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        // 会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
