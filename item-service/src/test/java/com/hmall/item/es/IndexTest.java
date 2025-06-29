package com.hmall.item.es;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class IndexTest {
    //索引库名称
    private static final String INDEX_NAME = "items";
    //索引库映射结构
    private static final String MAPPING_TEMPLATE = "{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"name\":{\n" +
            "        \"type\": \"text\",\n" +
            "        \"analyzer\": \"ik_max_word\"\n" +
            "      },\n" +
            "      \"price\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"stock\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"image\":{\n" +
            "        \"type\": \"keyword\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"category\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"brand\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"sold\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"commentCount\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"isAD\":{\n" +
            "        \"type\": \"boolean\"\n" +
            "      },\n" +
            "      \"updateTime\":{\n" +
            "        \"type\": \"date\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";



    //es的操作客户端对象，执行任何与es有关的操作都需要
    //说明接下来测试的每个方法都要先初始化该对象
    private RestHighLevelClient client;

    //执行每个方法之前都要先执行的方法
    @BeforeEach
    public void init(){
        client = new RestHighLevelClient(RestClient.builder(HttpHost.create("http://192.168.100.128:9200")));
    }

    //测试client是否有效
    @Test
    public void testClient() throws IOException {
        System.out.println(client);
    }

    //每次执行完之后要关闭客户端的练级
    @AfterEach
    public void close() throws IOException {
        client.close();
    }

    //创建索引库items
    @Test
    public void createIndex() throws IOException {
        //1.创建索引库对象-->创建索引的请求
        CreateIndexRequest request = new CreateIndexRequest(INDEX_NAME);

        //2.设置参数
        request.source(MAPPING_TEMPLATE, XContentType.JSON);

        //3.发送请求
        client.indices().create(request, RequestOptions.DEFAULT);
    }

    //判断是否存在
    @Test
    public void testExistsIndex() throws IOException {
        //1.创建索引库对象-->创建索引的请求
        GetIndexRequest request = new GetIndexRequest(INDEX_NAME);

        //2.发送请求
        Boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);

        //3.处理结果
        System.out.println(exists?"索引不存在":"索引存在");
    }

    //删除索引库
    @Test
    void testDeleteIndex() throws IOException {
        //1.创建索引库对象-->创建索引的请求
        DeleteIndexRequest request = new DeleteIndexRequest(INDEX_NAME);
        //2.发送请求
        client.indices().delete(request, RequestOptions.DEFAULT);
    }


    
}
