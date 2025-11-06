package com.ershi.aspider.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * es客户端配置类
 *
 * @author Ershi-Gu.
 * @since 2025-11-05
 */
@Configuration
public class ElasticsearchConfig {

    /** es节点所在服务器地址 */
    @Value("${elasticsearch.host}")
    private String host;

    /** es节点端口 */
    @Value("${elasticsearch.port}")
    private int port;

    @Bean
    public Rest5Client rest5Client() {
        return Rest5Client.builder(new HttpHost("http", host, port))
            .setRequestConfigCallback(requestConfig -> requestConfig
                // 连接超时10秒
                .setConnectionRequestTimeout(Timeout.of(10, TimeUnit.SECONDS))
                // 响应超时60秒
                .setConnectionRequestTimeout(Timeout.of(60, TimeUnit.SECONDS)).build()).build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(Rest5Client rest5Client) {
        Rest5ClientTransport transport = new Rest5ClientTransport(rest5Client, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}