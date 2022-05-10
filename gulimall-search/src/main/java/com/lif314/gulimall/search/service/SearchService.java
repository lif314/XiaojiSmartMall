package com.lif314.gulimall.search.service;

import com.lif314.gulimall.search.vo.SearchParamVo;
import com.lif314.gulimall.search.vo.SearchResultVo;

public interface SearchService {
    SearchResultVo search(SearchParamVo searchParam);
}
