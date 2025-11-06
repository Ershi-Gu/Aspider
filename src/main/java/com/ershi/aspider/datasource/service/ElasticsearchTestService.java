package com.ershi.aspider.datasource.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Elasticsearch连接测试服务
 */
@Service
public class ElasticsearchTestService {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    /**
     * 测试ES连接
     * 获取ES集群信息
     */
    public void testConnection() {
        try {
            System.out.println("========== 开始测试Elasticsearch连接 ==========");

            // 获取ES服务器信息
            InfoResponse info = elasticsearchClient.info();

            System.out.println("✅ ES连接成功！");
            System.out.println("集群名称: " + info.clusterName());
            System.out.println("集群UUID: " + info.clusterUuid());
            System.out.println("ES版本: " + info.version().number());
            System.out.println("Lucene版本: " + info.version().luceneVersion());
            System.out.println("========== ES连接测试完成 ==========");

        } catch (Exception e) {
            System.err.println("❌ ES连接失败！");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
        }
    }
}