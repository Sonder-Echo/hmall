package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.utils.CollUtils;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.apache.lucene.index.IndexReader;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

//如果有多个环境可以切换查询的话，可以通过properties设置spring.profile.active进行切换
@SpringBootTest(properties = "spring.profiles.active=dev")
public class DocumentTest {

    @Autowired
    private IItemService itemService;

    //索引库名称
    private static final String INDEX_NAME = "items";

    //es的操作客户端对象，执行任何与es有关的操作都需要
    //说明接下来测试的每个方法都要先初始化该对象
    private RestHighLevelClient client;

    //执行每个方法之前都要先执行的方法
    @BeforeEach
    public void init(){
        client = new RestHighLevelClient(RestClient.builder(HttpHost.create("http://192.168.100.128:9200")));
    }

    //每次执行完之后要关闭客户端的练级
    @AfterEach
    public void close() throws IOException {
        client.close();
    }

    /*
     * 根据商品id擦汗寻mysql数据库中的商品，并将该商品保存到es
     */
    @Test
    public void testCreateIndex() throws IOException {
        //1.根据商品id查询mysql数据库的商品
        Item item = itemService.getById(577967L);
        //2.将Item对象转换为es可以接受的ItemDoc
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        //3.创建 创建文档 的请求对象
        IndexRequest request = new IndexRequest(INDEX_NAME).id(itemDoc.getId().toString());
        //4.设置请求参数
        String jsonStr = JSONUtil.toJsonStr(itemDoc);
        request.source(jsonStr, XContentType.JSON);
        //5.发送请求
        client.index(request, RequestOptions.DEFAULT);
    }

    //查询文档
    @Test
    public void testGetDocument() throws IOException {
        //1.创建查询索引的请求对象
        GetRequest request = new GetRequest(INDEX_NAME, "577967");
        //2.发送请求
        GetResponse response = client.get(request, RequestOptions.DEFAULT);

        //3.获取原插入的数据
        String jsonStr = response.getSourceAsString();
        System.out.println(jsonStr);

        ItemDoc itemDoc = JSONUtil.toBean(jsonStr, ItemDoc.class);
        System.out.println(itemDoc);
    }

    //更新文档
    @Test
    public void testUpdateDocument() throws IOException {
        //创建请求对象
        UpdateRequest request = new UpdateRequest(INDEX_NAME, "577967");
        //设置请求参数
        request.doc("name", "aaabbbccc", "price", 9999);
        //发送请求
        client.update(request, RequestOptions.DEFAULT);
    }

    //删除文档
    @Test
    public void testDeleteDocument() throws IOException {
        //创建请求对象
        DeleteRequest request = new DeleteRequest(INDEX_NAME, "577967");
        //发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }

    //批量导入商品到es
    @Test
    public void testBatchImport() throws IOException {
        //页号
        int pageNo = 1;
        //页大小
        int pageSize = 1000;

        while(true){
            System.out.println("~~~~~~~~~~~~正在导入第" + pageNo + "页数据~~~~~~~~~~~~");

            //1.根据页号和页大小查询mysql数据库中的商品
            Page<Item> page = itemService.lambdaQuery().eq(Item::getStatus, 1).page(new Page<>(pageNo, pageSize));
            List<Item> itemList = page.getRecords();
            if(CollUtils.isEmpty(itemList)){
                break;
            }

            //2.将Item对象转换为es可以接受的ItemDoc
            List<ItemDoc> itemDocList = BeanUtil.copyToList(itemList, ItemDoc.class);

            //3.将ItemDoc对象转换为json字符串并加入bulkRequest中
            BulkRequest bulkRequest = new BulkRequest();
            for (ItemDoc itemDoc : itemDocList) {
                IndexRequest indexRequest = new IndexRequest(INDEX_NAME).id(itemDoc.getId().toString());
                String jsonStr = JSONUtil.toJsonStr(itemDoc);
                indexRequest.source(jsonStr, XContentType.JSON);
                bulkRequest.add(indexRequest);
            }

            //4.提交BulkRequest
            client.bulk(bulkRequest, RequestOptions.DEFAULT);


            System.out.println("~~~~~~~~~~~~第" + pageNo + "页数据导入完成~~~~~~~~~~~~");
            //5.继续分页查询,知道没有数据
            pageNo++;
        }
    }


}
