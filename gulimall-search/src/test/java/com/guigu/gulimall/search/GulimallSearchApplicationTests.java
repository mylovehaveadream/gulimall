package com.guigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.guigu.gulimall.search.config.ElasticSearchConfig;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Map;

@SpringBootTest
public class GulimallSearchApplicationTests {

    /**
     * 给ES存这些数据
     * (1).方便检索{
     *     skuId:1
     *     spuId:11
     *     skuTitle:华为xx
     *     price:998
     *     saleCount:99
     *     attrs:[
     *      {尺寸：5寸}，
     *      {CPU：高通845}，
     *      {分辨率：全高清}
     *     ]
     * }
     *
     * 这样会冗余
     *
     * (2).
     * sku索引{
     *     skuId:1
     *     spuId:11
     *     xxxx
     * }
     *
     * attr索引{
     *     spuId:11
     *     attrs:[
     *        {尺寸：5寸}，
     *        {CPU：高通845}，
     *        {分辨率：全高清}
     *     ]
     * }
     *
     *  这样满足了不冗余，但还有问题
     *
     *  搜索  小米： 粮食、手机、电器
     *  10000个、4000个spu
     *  找到这些spu所有的属性来进行聚合，会做一个分步查询
     *  分步，4000个spu对应的所有可能属性；
     *  esClient:spuId:[4000个spuid] 4000*8个字节=32000byte=32kb
     *  一个请求下来要发32kb的数据
     *
     *
     *  所以最后还是以空间来换取时间，
     *  选择第一种数据
     */

    @Autowired
    private RestHighLevelClient client;

    @Data
    @ToString
    static class Account { //通过工具来生成bean
        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;
    }


    @Test
    public void searchData() throws IOException {
        //1.创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        //指定索引，来检索数据
        searchRequest.indices("bank");
        //指定DSL，检索条件
        //SearchSourceBuilder sourceBuilder 封装的条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //1.1.构造检索条件
        //query这些根操作我们能写的，在searchSourceBuilder都有
//        searchSourceBuilder.query();
//        searchSourceBuilder.from();
//        searchSourceBuilder.size();
//        searchSourceBuilder.aggregation();

        //QueryBuilders工具类
        searchSourceBuilder.query(QueryBuilders.matchQuery("address","mill"));

        //1.2.按照年龄的值分布进行聚合
        //name为这次聚合起的名字
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        searchSourceBuilder.aggregation(ageAgg);

        //1.3.计算平均薪资
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        searchSourceBuilder.aggregation(balanceAvg);

        //打印
        System.out.println("检索条件"+searchSourceBuilder.toString());

        searchRequest.source(searchSourceBuilder);

        //2.执行检索；
        SearchResponse search = client.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);

        //3.分析结果 SearchResponse封装着结果
        System.out.println(search.toString());
        //把结果封装成map
//        Map map = JSON.parseObject(search.toString(), Map.class);

        //3.1.获取所有查到的数据
        SearchHits hits = search.getHits(); //得到最大的hits
        //获取里面的hits,真正命中的所有的记录
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            /**
             * "_index": "bank",
             * 			"_type": "account",
             * 			"_id": "970",
             * 			"_score": 5.4032025,
             * 			"_source": {
             */
//            hit.getIndex();hit.getType();hit.getId();
            String s = hit.getSourceAsString();
            //将JSON字符串转成指定Account对象
            //将检索到的结果封装成对象
            Account account = JSON.parseObject(s, Account.class);
        }

        //3.2.获取这次检索到的分析信息
        //获取聚合信息
        Aggregations aggregations = search.getAggregations();
//        for (Aggregation aggregation : aggregations.asList()) {
//            System.out.println("当前聚合的名字："+aggregation.getName());
//        }

        Terms ageAgg1 = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("年龄"+keyAsString+"-->"+bucket.getDocCount());
        }


        Avg balanceAvg1 = aggregations.get("balanceAvg");
        System.out.println("平均薪资"+balanceAvg1.getValue());
    }




    //index给ES来存储数据
    //测试存储数据到ES
    //保存更新二合一
    @Test
    public void indexData() throws IOException {
        //参数是放索引的
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");   //数据id

        //方式一：
//        indexRequest.source("userName","zhangsan","age",18,"gender","男");

        //方式二：
        User user = new User();
        user.setUserName("zhangsan");
        user.setAge(19);
        user.setGender("男");
        String jsonString = JSON.toJSONString(user);//把对象转成JSON，才能放进来
        indexRequest.source(jsonString, XContentType.JSON);    //要保存的内容,需要传内容类型XContentType.JSON

        //将保存请求传过去,索引保存
        //执行保存操作
        IndexResponse index = client.index(indexRequest, ElasticSearchConfig.COMMON_OPTIONS);

        //提取有用的响应的数据
        System.out.println(index);
    }

    @Data
    class User{
        private String userName;
        private String gender;
        private Integer age;
    }


    @Test
    public void contextLoads() {
        System.out.println(client);
    }


}



