package com.xuecheng.search;

import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestIndex {

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Autowired
    RestClient restClient;


    @Test
    public void testDeleteIndex() throws IOException {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("xc_course");
        IndicesClient indices = restHighLevelClient.indices();
        DeleteIndexResponse delete = indices.delete(deleteIndexRequest);
        boolean acknowledged = delete.isAcknowledged();
        System.out.println(acknowledged);
    }

    @Test
    public void testCreateIndex() throws IOException{
        CreateIndexRequest createIndexRequest = new CreateIndexRequest("xc_course");
        createIndexRequest.settings(Settings.builder().put("number_of_shards","1").put("number_of_replicas",0));
        createIndexRequest.mapping("_doc","{\n" +
                "    \"properties\": {\n" +
                "        \"name\": {\n" +
                "            \"type\": \"text\",\n" +
                "            \"analyzer\": \"ik_max_word\",\n" +
                "            \"search_analyzer\": \"ik_smart\"\n" +
                "        },\n" +
                "        \"description\": {\n" +
                "            \"type\": \"text\",\n" +
                "            \"analyzer\": \"ik_max_word\",\n" +
                "            \"search_analyzer\": \"ik_smart\"\n" +
                "        },\n" +
                "        \"studymodel\": {\n" +
                "            \"type\": \"keyword\"\n" +
                "        },\n" +
                "        \"price\": {\n" +
                "            \"type\": \"float\"\n" +
                "        },\n" +
                "        \"timestamp\": {\n" +
                "            \"type\": \"date\",\n" +
                "            \"format\": \"yyyy‐MM‐ddHH:mm:ss||yyyy‐MM‐dd||epoch_millis\"\n" +
                "        }\n" +
                "    }\n" +
                "}", XContentType.JSON);
        IndicesClient indices = restHighLevelClient.indices();
        AcknowledgedResponse createIndexResponse = indices.create(createIndexRequest);
        boolean acknowledged = createIndexResponse.isAcknowledged();
        System.out.println(acknowledged);


    }

    @Test
    public void testAddDoc() throws IOException {
        //准备json数据
        Map<String,Object> jsonMap=new HashMap<>();
        jsonMap.put("name","springcloud实战");
        jsonMap.put("description","本课程主要从四个章节进行讲解：1.微服务架构入门2.springcloud基础入门3.实战SpringBoot4.注册中心eureka。");
        jsonMap.put("studymodel","201001");
        SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy‐MM‐ddHH:mm:ss");
        jsonMap.put("timestamp",dateFormat.format(new Date()));
        jsonMap.put("price",5.6f);//索引请求对象
        //创建索引创建对象
        IndexRequest indexRequest = new IndexRequest("xc_course","_doc");
        //文档内容
        indexRequest.source(jsonMap);
        //通过client进行http请求
        IndexResponse index = restHighLevelClient.index(indexRequest);
        DocWriteResponse.Result result = index.getResult();
        System.out.println(result);
    }

    @Test
    public void testGetDoc() throws IOException {
        GetRequest getRequest = new GetRequest("xc_course", "_doc", "lVgwCHQBkitNpOrNRfs8");
        GetResponse documentFields = restHighLevelClient.get(getRequest);
        Map<String, Object> sourceAsMap = documentFields.getSourceAsMap();
        System.out.println(sourceAsMap);

    }

    @Test
    public void testSearchAll() throws IOException, ParseException {
        //搜索请求对象
        SearchRequest searchRequest = new SearchRequest("xc_course");
        //指定类型
        searchRequest.types("doc");
        //搜索源构建对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //搜索方式
        //matchAllQuery
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        //设置搜索源字段过滤
        searchSourceBuilder.fetchSource(new String[]{"name","studymodel,price","timestamp"},new String[]{});
        //向搜索请求对象中设置搜索源
        searchRequest.source(searchSourceBuilder);
        //执行搜索
        SearchResponse search = restHighLevelClient.search(searchRequest);
        long totalHits = search.getHits().getTotalHits();
        SearchHit[] hits = search.getHits().getHits();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for(SearchHit hit:hits){
            hit.getId();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            Double price = (Double) sourceAsMap.get("price");
            Date timestamp = simpleDateFormat.parse((String) sourceAsMap.get("timestamp"));
            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(studymodel);
        }
    }

}
