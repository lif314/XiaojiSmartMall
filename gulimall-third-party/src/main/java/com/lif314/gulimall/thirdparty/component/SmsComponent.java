package com.lif314.gulimall.thirdparty.component;


import com.lif314.common.utils.HttpUtils;
import lombok.Data;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Data
@ConfigurationProperties(prefix = "spring.cloud.alicloud.sms")
public class SmsComponent {
    private String host; // 请求URL
    private String path; // 请求URL路径
    private String templateId; // 模板Id
    private String appcode;
    public void sendSmsCode(String phone, String code) {
        String method = "POST";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("mobile", phone);  // 短信接收人号码
        querys.put("templateId", templateId); // 信模板ID，模板需要联系服务商开通，测试时可使用服务商提供的测试模板
        querys.put("value", code); // 短信发送的变量值，即替换模板中@1@的变量，多个变量以竖线分隔
        Map<String, String> bodys = new HashMap<String, String>();

        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
