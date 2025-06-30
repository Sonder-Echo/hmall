package com.hmall.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.utils.CollUtils;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.PageVO;
import com.hmall.search.service.ISearchService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public PageVO<ItemDoc> search(ItemPageQuery query) {
        PageVO<ItemDoc> pageVO = PageVO.empty(0L, 0L);

        try {

            //1.创建查询请求
            SearchRequest request = new SearchRequest(INDEX_NAME);
            //2.设置查询及各类参数
            //创建bool查询
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            //设置搜索关键字
            boolean isHighLight = false;
            if(StrUtil.isNotBlank(query.getKey())){
                boolQueryBuilder.must(QueryBuilders.matchQuery("name", query.getKey()));
                //只有搜索了关键字才设置高亮
                isHighLight = true;
            }
            //设置分类过滤查询
            if(StrUtil.isNotBlank(query.getCategory())){
                //TODO：查找为什么term要.keyword原因
                boolQueryBuilder.filter(QueryBuilders.termQuery("category.keyword", query.getCategory()));
            }
            //设置品牌过滤查询
            if (StrUtil.isNotBlank(query.getBrand())){
                //TODO：查找为什么term要.keyword原因
                boolQueryBuilder.filter(QueryBuilders.termQuery("brand.keyword", query.getBrand()));
            }
            //设置价格过滤查询
            if(query.getMinPrice() != null){
                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()));
            }
            if(query.getMaxPrice() != null){
                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()));
            }
            //设置高亮
            if (isHighLight){
                request.source().highlighter(
                        SearchSourceBuilder.highlight()
                                .field("name")
                                .preTags("<em>")
                                .postTags("</em>")
                );
            }
            //设置分页
            int pageNo = query.getPageNo();
            int pageSize = query.getPageSize();
            request.source().from((pageNo - 1)*pageSize).size(pageSize);
            //设置排序
            if(StrUtil.isNotBlank(query.getSortBy())){
                request.source().sort(query.getSortBy(), query.getIsAsc() ? SortOrder.ASC:SortOrder.DESC);
            }else {
                //默认按照更新时间降序排序
                request.source().sort("updateTime", SortOrder.DESC);
            }
            //设置查找对象
            request.source().query(boolQueryBuilder);

            //3.发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //4.解析响应结果
            SearchHits hits = response.getHits();
            //总记录数
            long total = hits.getTotalHits().value;
            pageVO.setTotal(total);
            //通过页大小和总记录数计算总页数
            long pages = (total % pageSize == 0) ? total/pageSize : (total/pageSize + 1);
            pageVO.setPages(pages);

            List<ItemDoc> itemDocList = new ArrayList<>(pageSize);
            for (SearchHit hit : hits.getHits()) {
                ItemDoc itemDoc = JSONUtil.toBean(hit.getSourceAsString(), ItemDoc.class);

                //处理高亮部分
                if(isHighLight){
                    HighlightField highlightField = hit.getHighlightFields().get("name");
                    if(highlightField != null){
                        String name = highlightField.getFragments()[0].toString();
                        itemDoc.setName(name);
                    }
                }
                itemDocList.add(itemDoc);

                return pageVO;
            }

        } catch (IOException e) {
            throw new RuntimeException("查询es中商品失败！", e);
        }
        return pageVO;
    }

}