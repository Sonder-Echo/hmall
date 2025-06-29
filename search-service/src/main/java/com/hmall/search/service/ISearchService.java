package com.hmall.search.service;

public interface ISearchService {

    /**
     * 保存商品文档到es
     * @param itemId
     */
    void saveItemById(Long itemId);

    /**
     * 根据商品id删除es中商品文档
     * @param itemId
     */
    void deleteItemById(Long itemId);
}
