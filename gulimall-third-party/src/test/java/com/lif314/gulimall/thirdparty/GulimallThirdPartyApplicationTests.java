package com.lif314.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;
import com.lif314.gulimall.thirdparty.component.SmsComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@SpringBootTest
class GulimallThirdPartyApplicationTests {

    @Autowired
    OSSClient ossClient;

    @Autowired
    SmsComponent smsComponent;

    @Test
    public void testSmsSend(){
      smsComponent.sendSmsCode("18387400236", "123456");
    }

    @Test
    void contextLoads() throws FileNotFoundException {
        ossClient.putObject("gulimall-lif314", "gulimall-3th.png", new FileInputStream("C:\\Users\\lilinfei\\Pictures\\github.png"));
        System.out.println("上传成功 ------");
    }

    @Test
    public void uploadFileOssClient() throws FileNotFoundException {
        ossClient.putObject("gulimall-lif314", "gulimall-3th.png", new FileInputStream("C:\\Users\\lilinfei\\Pictures\\github.png"));
        System.out.println("上传成功 ------");
    }

}
