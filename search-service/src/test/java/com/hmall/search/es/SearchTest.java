package com.hmall.search.es;

import cn.hutool.json.JSONUtil;
import com.hmall.api.dto.ItemDTO;
import org.apache.http.HttpHost;
import org.checkerframework.checker.units.qual.A;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SearchTest {

    private RestHighLevelClient client;

    private static final String INDEX_NAME = "items";

    @BeforeEach
    public void init(){
        client = new RestHighLevelClient(
                RestClient.builder(
                        HttpHost.create("http://192.168.100.128:9200")
                )
        );
    }

    @AfterEach
    public void close() throws IOException {
        client.close();
    }

    //测试match_all查询
    @Test
    public void testMatchAll() throws IOException {
        //创建请求对象
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        //设置请求参数
        searchRequest.source().query(QueryBuilders.matchAllQuery());
        //发送请求并获取结果
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        //解析响应结果
        handelResponse(searchResponse);
    }

    //测试match查询
    @Test
    public void testMatch() throws IOException {
        //创建请求对象
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        //设置请求参数
        searchRequest.source().query(QueryBuilders.matchQuery("name", "小米手机"));
        //发送请求并获取结果
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        //解析响应结果
        handelResponse(searchResponse);
    }

    //测试multi_match查询
    @Test
    public void testMultiMatch() throws IOException {
        //创建请求对象
        SearchRequest request = new SearchRequest(INDEX_NAME);
        //设置请求参数
        request.source().query(QueryBuilders.multiMatchQuery("脱脂牛奶", "name", "category"));
        //发送请求并获取结果
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //解析响应结果
        handelResponse(response);
    }

    //测试term查询
    @Test
    public void testTerm() throws IOException {
        //创建请求对象
        SearchRequest request = new SearchRequest(INDEX_NAME);
        //设置请求参数
//        request.source().query(QueryBuilders.termQuery("category", "牛奶"));
        request.source().query(QueryBuilders.termQuery("category.keyword", "牛奶"));
        //发送请求并获取结果
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //解析响应结果
        handelResponse(response);
    }

    //测试range查询
    @Test
    public void testRange() throws IOException {
        //创建请求对象
        SearchRequest request = new SearchRequest(INDEX_NAME);
        //设置请求参数
        request.source().query(QueryBuilders.rangeQuery("price").gte(100).lte(150));
        //发送请求并获取结果
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //解析响应结果
        handelResponse(response);
    }

    //测试bool
    @Test
    public void testBool() throws IOException {
        //创建请求对象
        SearchRequest request = new SearchRequest(INDEX_NAME);
        //设置请求参数
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //搜索name，关键字为手机
        boolQueryBuilder.must(QueryBuilders.matchQuery("name","手机"));
        //品牌过滤为查询华为
        boolQueryBuilder.filter(QueryBuilders.termQuery("brand.keyword","华为"));
        //价格过滤为低于30000
        boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(30000));

        request.source().query(boolQueryBuilder);
        //发送请求并获取结果
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //解析响应结果
        handelResponse(response);
    }

    //测试排序和分页
    @Test
    public void testSortAndPage() throws IOException {
        //创建请求对象
        SearchRequest request = new SearchRequest(INDEX_NAME);
        //设置请求参数
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //搜索name，关键字为手机
        boolQueryBuilder.must(QueryBuilders.matchQuery("name","手机"));
        //品牌过滤为查询华为
        boolQueryBuilder.filter(QueryBuilders.termQuery("brand.keyword","华为"));
        //价格过滤为低于30000
        boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(30000));

        request.source().query(boolQueryBuilder);


        //排序
        request.source().sort("price", SortOrder.DESC);
        //分页
        int pageNo = 1;
        int pageSize = 5;
        //from 设置的是起始索引号
        request.source().from((pageNo-1)*pageSize).size(pageSize);

        //发送请求并获取结果
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //解析响应结果
        handelResponse(response);
    }


    private static void handelResponse(SearchResponse searchResponse) {
        //解析响应结果
        SearchHits searchHits = searchResponse.getHits();
        //符合本次搜索的总记录数（最多10000条）
        long total = searchHits.getTotalHits().value;
        System.out.println("本次搜索命中的总记录数：" + total);
        SearchHit[] hits = searchHits.getHits();
        if (hits != null && hits.length > 0){
            for (SearchHit hit : hits) {
                //获取源文档json字符串
                String jsonStr = hit.getSourceAsString();
                ItemDTO itemDTO = JSONUtil.toBean(jsonStr, ItemDTO.class);
                System.out.println(itemDTO);
            }
        }
    }

}
