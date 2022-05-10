package com.lif314.gulimall.search.service;

import com.lif314.gulimall.search.vo.SearchParam;
import com.lif314.gulimall.search.vo.SearchResult;

public interface SearchService {
    SearchResult search(SearchParam searchParam);
}
