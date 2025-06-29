package com.hmall.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.service.ISearchService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SearchServiceImpl implements ISearchService {
    //索引库名称
    private static final String INDEX_NAME = "items";

    @Autowired
    private ItemClient itemClient;

    private RestHighLevelClient client;

    public SearchServiceImpl() {
        client = new RestHighLevelClient(RestClient.builder(HttpHost.create("http://192.168.100.128:9200")));
    }

    @Override
    public void saveItemById(Long itemId) {
        try {
            //1.根据商品id查询商品信息
            ItemDTO itemDTO = itemClient.queryItemById(itemId);
            if(itemDTO != null){
                //2.转换为itemDoc json字符串
                ItemDoc itemDoc = BeanUtil.copyProperties(itemDTO, ItemDoc.class);
                String jsonStr = JSONUtil.toJsonStr(itemDoc);
                //3.创建请求
                IndexRequest request = new IndexRequest(INDEX_NAME).id(itemDoc.getId().toString());
                //4.设置source参数
                request.source(jsonStr, XContentType.JSON);
                //5.发送请求
                client.index(request, RequestOptions.DEFAULT);
            }
        }catch (IOException e){
            throw new RuntimeException("更新es中商品失败！参数:" + itemId, e);
        }
    }

    @Override
    public void deleteItemById(Long itemId) {
        try {
            //1.创建删除请求
            DeleteRequest request = new DeleteRequest(INDEX_NAME).id(itemId.toString());
            //2.发送请求
            client.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("删除es中商品失败！参数:" + itemId, e);
        }
    }
}