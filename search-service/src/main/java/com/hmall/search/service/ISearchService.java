package com.hmall.search.service;

import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.PageVO;

import java.util.List;
import java.util.Map;

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

    /**
     * 条件分页查询
     * @param query
     * @return
     */
    PageVO<ItemDoc> search(ItemPageQuery query);

    /**
     * 查询商品分类、品牌列表
     * @param query
     * @return
     */
    Map<String, List<String>> filters(ItemPageQuery query);
}
