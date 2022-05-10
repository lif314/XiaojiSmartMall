package com.lif314.gulimall.product.web;

import com.alibaba.nacos.shaded.org.checkerframework.checker.units.qual.A;
import com.lif314.gulimall.product.entity.CategoryEntity;
import com.lif314.gulimall.product.service.CategoryService;
import com.lif314.gulimall.product.vo.Catelog2Vo;
import lombok.experimental.PackagePrivate;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    /**
     * 更改访问路由
     * / /index.html 都跳转到首页
     * model: 页面
     * @return 页面地址
     */
    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model){

        // 查出所有的一级分类
        List<CategoryEntity> categoryEntityList =  categoryService.getLevel1Category();
        // 将数据放在页面中
        model.addAttribute("categories", categoryEntityList);
        /**
         * 返回逻辑视图。视图解析器进行拼串
         * 默认前缀：classpath:/templates
         * 默认后缀：.html
         * 实际路由：classpath:/templates/index.html
         */
        return "index";
    }


    // index/catalog.json
    @ResponseBody // 返回数据，而不是跳转页面
    @GetMapping("index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatelogJson(){
        Map<String, List<Catelog2Vo>> map =  categoryService.getCatelogJson();
        return map;
    }

    @Autowired
    RedissonClient redissonClient;

   @Autowired
    StringRedisTemplate stringRedisTemplate;

    // 保证读到最新的数据，写锁是一个排他锁(互斥锁)，读锁是一个共享锁
    // 写锁没有释放，读锁需要一直等待
    @ResponseBody
    @GetMapping("/write")
    public String writeValue(){
        RReadWriteLock rw_lock = redissonClient.getReadWriteLock("rw_lock");
        String s = "";
        RLock wLock = rw_lock.writeLock();
        try {
            // 1、改数据，加写锁,读数据加读锁
            wLock.lock();
            System.out.println("写锁加锁成功....");
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
            // 写入数据
            stringRedisTemplate.opsForValue().set("writeValue", s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            // 释放锁
            wLock.unlock();
            System.out.println("写锁释放成功........");
        }
        return s;
    }


    @ResponseBody
    @GetMapping("/read")
    public String readValue(){

        RReadWriteLock rw_lock = redissonClient.getReadWriteLock("rw_lock");
        String s = "";
        RLock rLock = rw_lock.readLock();
        try {
            // 读取数据
            rLock.lock();
            System.out.println("读锁加锁成功......");
            stringRedisTemplate.opsForValue().get("writeValue");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            rLock.unlock();
            System.out.println("读锁释放成功......");
        }
        return s;
    }



    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        // 获取一把锁，参数是锁名，只要锁名相同，就是同一把锁
        RLock reentrant_lock = redissonClient.getLock("Reentrant_Lock");
        // 加锁---阻塞式等待：没有拿到锁，一直等待
        reentrant_lock.lock(10, TimeUnit.SECONDS); // 10s解锁
        try{ // 防止业务执行异常
            System.out.println("加锁成功，执行业务......");
            Thread.sleep(30000); // 模拟业务执行时间
        }catch (Exception e){
        }finally {
            // 解锁
            // 假设解锁代码没有运行，会不会出现死锁？
            // 锁的TTL(存活时间，s) -- 锁的自动续机
            System.out.println("释放锁...: " + Thread.currentThread().getId());
            reentrant_lock.unlock();
        }
        return "Hello";
    }


    /**
     * 信号量
     * 车位停车
     * 3车位
     */
    @GetMapping("/park")
    @ResponseBody
    public String park(){
        RSemaphore park = redissonClient.getSemaphore("park");
        try {
            park.acquire();//获取一个信号量,取得一个值，占一个车位
//            park.tryAcquire(); // 不阻塞
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "ok";
    }

    @GetMapping("/go")
    @ResponseBody
    public String go(){
        RSemaphore park = redissonClient.getSemaphore("park");
        park.release();  //释放一个车位
        return "ok";
    }


    /**
     * 闭锁
     * 放假， 1班没人了
     * 5个班走完才锁门
     */
    @GetMapping("/lockdoor")
    @ResponseBody
    public String lockDoor(){
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        try {
            // 等待闭锁完成
            door.trySetCount(5);
            door.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "放假了.....";
    }

    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id){
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.countDown();  // 计数减1
        return id.toString() + "班的人已经走了";
    }



}
