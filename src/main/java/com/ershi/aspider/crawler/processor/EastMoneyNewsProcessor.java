package com.ershi.aspider.crawler.processor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import com.ershi.aspider.crawler.entity.NewsItem;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 东方财富板块聚焦API爬虫
 * 通过直接调用API接口获取新闻数据
 *
 * @author ershi
 */
public class EastMoneyNewsProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EastMoneyNewsProcessor.class);

    private final CloseableHttpClient httpClient;

    // 东方财富板块聚焦数据，API接口URL（去掉callback参数，直接获取JSON）
    private static final String API_URL = "https://np-listapi.eastmoney.com/comm/web/getNewsByColumns?" +
        "client=web&biz=web_news_col&column=408&order=1&needInteractData=0" +
        "&page_index={pageIndex}&page_size={pageSize}" +
        "&fields=code,showTime,title,mediaName,summary,image,url,uniqueUrl,Np_dst" +
        "&types=1,20";

    public EastMoneyNewsProcessor() {
        this.httpClient = HttpClients.createDefault();
    }

    /**
     * 获取新闻列表
     *
     * @param pageIndex 页码（从1开始）
     * @param pageSize  每页数量
     * @return 新闻列表
     */
    public List<NewsItem> getNewsList(int pageIndex, int pageSize) {
        List<NewsItem> newsItems = new ArrayList<>();

        try {
            String url = buildUrl(pageIndex, pageSize);
            logger.info("开始请求API: {}", url);

            String jsonResponse = fetchData(url);
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                logger.error("获取数据失败，响应为空");
                return newsItems;
            }

            // 如果是JSONP格式，需要提取JSON部分
            String jsonData = extractJsonFromJsonp(jsonResponse);

            // 解析JSON数据
            newsItems = parseNewsData(jsonData);
            logger.info("成功获取 {} 条新闻数据", newsItems.size());

        } catch (Exception e) {
            logger.error("获取新闻数据失败", e);
        }

        return newsItems;
    }

    /**
     * 构建请求URL
     */
    private String buildUrl(int pageIndex, int pageSize) {
        long timestamp = System.currentTimeMillis();
        return API_URL
                .replace("{pageIndex}", String.valueOf(pageIndex))
                .replace("{pageSize}", String.valueOf(pageSize))
                + "&req_trace=" + timestamp
                + "&_=" + timestamp;
    }

    /**
     * 发送HTTP请求获取数据
     */
    private String fetchData(String url) throws IOException {
        HttpGet httpGet = new HttpGet(url);

        // 设置请求头，模拟浏览器
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        httpGet.setHeader("Referer", "https://finance.eastmoney.com/");
        httpGet.setHeader("Accept", "*/*");

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                String content = EntityUtils.toString(response.getEntity(), "UTF-8");
                logger.debug("响应数据: {}", content.substring(0, Math.min(200, content.length())));
                return content;
            } else {
                logger.error("HTTP请求失败，状态码: {}", statusCode);
                return null;
            }
        }
    }

    /**
     * 从JSONP响应中提取JSON数据
     * 格式: callback({"code":"1","data":{...}})
     */
    private String extractJsonFromJsonp(String jsonpResponse) {
        if (jsonpResponse == null) {
            return null;
        }

        // 判断是否是JSONP格式
        if (jsonpResponse.contains("(") && jsonpResponse.endsWith(")")) {
            int startIndex = jsonpResponse.indexOf("(");
            int endIndex = jsonpResponse.lastIndexOf(")");
            if (startIndex > 0 && endIndex > startIndex) {
                return jsonpResponse.substring(startIndex + 1, endIndex);
            }
        }

        // 如果不是JSONP格式，直接返回原数据
        return jsonpResponse;
    }

    /**
     * 解析JSON数据为NewsItem列表
     */
    private List<NewsItem> parseNewsData(String jsonData) {
        List<NewsItem> newsItems = new ArrayList<>();

        try {
            JSONObject root = JSON.parseObject(jsonData);

            // 检查响应状态
            String code = root.getString("code");
            if (!"1".equals(code)) {
                logger.error("API返回错误，code: {}, message: {}",
                        code, root.getString("message"));
                return newsItems;
            }

            // 获取data对象
            JSONObject data = root.getJSONObject("data");
            if (data == null) {
                logger.error("响应中没有data字段");
                return newsItems;
            }

            // 获取新闻列表
            JSONArray list = data.getJSONArray("list");
            if (list == null || list.isEmpty()) {
                logger.warn("新闻列表为空");
                return newsItems;
            }

            // 解析每条新闻
            for (int i = 0; i < list.size(); i++) {
                JSONObject newsJson = list.getJSONObject(i);
                NewsItem newsItem = parseNewsItem(newsJson);
                if (newsItem != null) {
                    newsItems.add(newsItem);
                }
            }

            // 记录分页信息
            int pageIndex = data.getIntValue("page_index");
            int pageSize = data.getIntValue("page_size");
            int totalHits = data.getIntValue("totle_hits");
            logger.info("分页信息 - 当前页: {}, 每页数量: {}, 总数: {}", pageIndex, pageSize, totalHits);

        } catch (Exception e) {
            logger.error("解析JSON数据失败", e);
        }

        return newsItems;
    }

    /**
     * 解析单条新闻数据
     */
    private NewsItem parseNewsItem(JSONObject newsJson) {
        try {
            NewsItem newsItem = new NewsItem();

            // 标题
            if (newsJson.containsKey("title") && newsJson.get("title") != null) {
                newsItem.setTitle(newsJson.getString("title"));
            }

            // URL
            if (newsJson.containsKey("url") && newsJson.get("url") != null) {
                newsItem.setUrl(newsJson.getString("url"));
            }

            // 发布时间
            if (newsJson.containsKey("showTime") && newsJson.get("showTime") != null) {
                newsItem.setPublishTime(newsJson.getString("showTime"));
            }

            // 摘要
            if (newsJson.containsKey("summary") && newsJson.get("summary") != null) {
                newsItem.setSummary(newsJson.getString("summary"));
            }

            return newsItem;

        } catch (Exception e) {
            logger.error("解析单条新闻失败", e);
            return null;
        }
    }

    /**
     * 关闭资源
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            logger.error("关闭HttpClient失败", e);
        }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        EastMoneyNewsProcessor crawler = new EastMoneyNewsProcessor();

        try {
            // 获取第1页，每页20条数据
            List<NewsItem> newsList = crawler.getNewsList(1, 20);

            // 输出结果
            System.out.println("==================== 东方财富新闻列表 ====================");
            System.out.println("共获取 " + newsList.size() + " 条新闻\n");

            for (int i = 0; i < newsList.size(); i++) {
                NewsItem item = newsList.get(i);
                System.out.println("【" + (i + 1) + "】 " + item.getTitle());
                System.out.println("时间: " + item.getPublishTime());
                System.out.println("摘要: " + item.getSummary());
                System.out.println("链接: " + item.getUrl());
                System.out.println("------------------------------------------------------------");
            }

        } finally {
            crawler.close();
        }
    }
}