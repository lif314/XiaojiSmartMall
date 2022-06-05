package com.lif314.gulimall.search.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lif314.common.utils.R;
import com.lif314.gulimall.search.service.SearchService;
import com.lif314.gulimall.search.vo.SearchParamVo;
import com.lif314.gulimall.search.vo.SearchResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@Controller  // 页面跳转
@RequestMapping("/search")
public class SearchController {

    @Autowired
    SearchService searchService;

    /**
     * SpringMVC 自动将页面提交过来的所有请求的查询参数封装成指定的对象
     */
    // http://search.feihong.com/search.html?catalog3Id=165
    @ResponseBody
    @PostMapping("/list")  // 点击分类跳转到搜索页面
    public R listPage(@RequestBody SearchParamVo searchParam) {
        // 获取query参数
//        System.out.println("参数：" + searchParam);
//        String s = JSON.toJSONString(searchParam);
//        SearchParamVo param = JSON.parseObject(s, new TypeReference<SearchParamVo>(){});
        SearchResultVo result = searchService.search(searchParam);
        return R.ok().put("data", result);
    }
}
