package com.guigu.gulimall.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.*;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 1.导入依赖
 * 2.编写配置,给容器注入一个RestHighLevelClient，它来操作9200端口
 * 3.参照api https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-getting-started-initialization.html
 */
@Configuration
public class ElasticSearchConfig {

    public static final RequestOptions COMMON_OPTIONS;

    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
//        builder.addHeader("Authorization", "Bearer " + TOKEN);
//        builder.setHttpAsyncResponseConsumerFactory(
//                new HttpAsyncResponseConsumerFactory
//                        .HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }

    @Bean
    public RestHighLevelClient esRestClient(){

        RestClientBuilder builder = null;
        builder = RestClient.builder(new HttpHost("47.113.95.188", 9200, "http"));
        RestHighLevelClient client = new RestHighLevelClient(builder);

//        RestHighLevelClient client = new RestHighLevelClient(
//                RestClient.builder(new HttpHost("47.113.95.188", 9200, "http")));

        return client;
    }
}
