package com.ershi.aspider.dbmanager.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.ershi.aspider.dbmanager.ElasticsearchUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch工具类配置 将工具类注入到Spring容器中
 *
 * @author ershi
 */
@Configuration
public class ElasticsearchUtilsConfig {

    @Bean
    public ElasticsearchUtils elasticsearchUtils(ElasticsearchClient elasticsearchClient) {
        return new ElasticsearchUtils(elasticsearchClient);
    }
}