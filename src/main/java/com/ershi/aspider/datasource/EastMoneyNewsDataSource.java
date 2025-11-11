package com.ershi.aspider.datasource;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ershi.aspider.datasource.entity.NewsDataItem;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 东方财富板块聚焦新闻数据获取源
 *
 * @author Ershi-Gu.
 * @since 2025-11-10
 */
@Component
public class EastMoneyNewsDataSource implements NewsDataSource {

    private static final Logger log = LoggerFactory.getLogger(EastMoneyNewsDataSource.class);

    private final CloseableHttpClient httpClient;

    private final Executor aspiderVirtualExecutor;

    // 东方财富板块聚焦数据，API接口URL（去掉callback参数，直接获取JSON）
    private static final String LIST_API_URL = "https://np-listapi.eastmoney.com/comm/web/getNewsByColumns?" +
        "client=web&biz=web_news_col&column=408&order=1&needInteractData=0" +
        "&page_index={pageIndex}&page_size={pageSize}" +
        "&fields=code,showTime,title,mediaName,summary,image,url,uniqueUrl,Np_dst" +
        "&types=1,20";

    @Autowired
    public EastMoneyNewsDataSource(Executor aspiderVirtualExecutor) {
        this.httpClient = HttpClients.createDefault();
        this.aspiderVirtualExecutor = aspiderVirtualExecutor;
    }

    @Override
    public List<NewsDataItem> getNewsData() {
        // 获取新闻列表
        List<NewsDataItem> newsDataItems = getNewsList(1, 20);

        // 获取详情内容
        return batchGetFullNewsContent(newsDataItems);
    }

    /**
     * 从新闻列表中并发解析出详情。查看NewsDataItem.content
     *
     * @param newsDataItems
     * @return {@link List }<{@link NewsDataItem }>
     */
    private List<NewsDataItem> batchGetFullNewsContent(List<NewsDataItem> newsDataItems) {
        List<CompletableFuture<NewsDataItem>> futures = newsDataItems.stream().map(
            item -> CompletableFuture.supplyAsync(() -> {
                try {
                    // 访问详情url，获取html
                    Document doc = Jsoup.connect(item.getContentUrl()).userAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36").timeout(10000).get();

                    // 提取正文内容
                    Element contentBody = doc.getElementById("ContentBody");

                    if (contentBody != null) {
                        // 移除隐藏段落，清洗格式
                        cleanContent(contentBody);
                        // 提取纯文本内容
                        String content = contentBody.text();
                        // 移除所有空白字符
                        content = content.replaceAll("[\\s\\u3000\\u00A0]+", "");

                        item.setContent(content);
                    } else {
                        log.info("《{}》 未找到详情内容", item.getTitle());
                    }

                } catch (IOException e) {
                    log.error("获取内容失败:{}，错误:{}", item.getContentUrl(), e.getMessage());
                }

                return item;

            }, aspiderVirtualExecutor)).collect(Collectors.toList());

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 收集结果
        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());

    }

    /**
     * 清理内容，移除不需要的元素
     */
    private void cleanContent(Element contentBody) {
        // 移除隐藏段落
        contentBody.select("p[style*='display:none']").remove();
        // 移除广告链接
        contentBody.select("a[href*='acttg.eastmoney.com']").remove();
        // 移除图片（可选）
        contentBody.select("img").remove();
        // 移除脚本和样式
        contentBody.select("script, style").remove();
    }

    /**
     * 获取新闻列表
     *
     * @param pageIndex
     * @param pageSize
     * @return {@link List }<{@link NewsDataItem }>
     */
    private List<NewsDataItem> getNewsList(int pageIndex, int pageSize) {
        // 结果集
        List<NewsDataItem> newsDataItems;

        // 构建请求url
        String url = buildUrl(pageIndex, pageSize);
        log.debug("开始请求API: {}", url);

        try {
            // 发送http请求获取数据
            String jsonData = fetchData(url);
            if (jsonData == null || jsonData.isEmpty()) {
                log.error("获取东方财富板块聚焦数据失败");
            }

            // 解析新闻列表数据，保存为dto
            newsDataItems = parseNewsData(jsonData);
            log.info("成功获取 {} 条新闻数据", newsDataItems.size());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return newsDataItems;
    }

    /**
     * 构建请求URL
     */
    private String buildUrl(int pageIndex, int pageSize) {
        long timestamp = System.currentTimeMillis();
        return LIST_API_URL.replace("{pageIndex}", String.valueOf(pageIndex)).replace("{pageSize}", String.valueOf(
            pageSize)) + "&req_trace=" + timestamp + "&_=" + timestamp;
    }

    /**
     * 发送HTTP请求获取数据
     */
    private String fetchData(String url) throws IOException {
        // 构建http请求体
        HttpGet httpGet = new HttpGet(url);

        // 设置请求头，模拟浏览器
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        httpGet.setHeader("Referer", "https://finance.eastmoney.com/");
        httpGet.setHeader("Accept", "*/*");

        // 发出请求
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                String content = EntityUtils.toString(response.getEntity(), "UTF-8");
                log.debug("响应数据: {}", content.substring(0, Math.min(200, content.length())));
                return content;
            } else {
                log.error("HTTP请求失败，状态码: {}", statusCode);
                return null;
            }
        }
    }

    /**
     * 解析JSON数据为NewsDataItem列表
     */
    private List<NewsDataItem> parseNewsData(String jsonData) {
        List<NewsDataItem> newsDataItems = new ArrayList<>();

        try {
            // 解析JSON数据
            JSONObject root = JSON.parseObject(jsonData);

            // 检查响应状态
            String code = root.getString("code");
            if (!"1".equals(code)) {
                log.error("API返回错误，code: {}, message: {}", code, root.getString("message"));
                return newsDataItems;
            }

            // 获取data对象
            JSONObject data = root.getJSONObject("data");
            if (data == null) {
                log.error("响应中没有data字段");
                return newsDataItems;
            }

            // 获取新闻列表
            JSONArray list = data.getJSONArray("list");
            if (list == null || list.isEmpty()) {
                log.warn("新闻列表为空");
                return newsDataItems;
            }

            // 解析每条新闻
            for (int i = 0; i < list.size(); i++) {
                JSONObject newsJson = list.getJSONObject(i);
                NewsDataItem newsDataItem = parseNewsDataItem(newsJson);
                if (newsDataItem != null) {
                    newsDataItems.add(newsDataItem);
                }
            }

            // 记录分页信息
            int pageIndex = data.getIntValue("page_index");
            int pageSize = data.getIntValue("page_size");
            int totalHits = data.getIntValue("totle_hits");
            log.info("分页信息 - 当前页: {}, 每页数量: {}, 总数: {}", pageIndex, pageSize, totalHits);

        } catch (Exception e) {
            log.error("解析JSON数据失败", e);
        }

        return newsDataItems;
    }

    /**
     * 解析单条新闻数据
     *
     * @param newsJson api返回json格式数据
     * @return {@link NewsDataItem }
     */
    private NewsDataItem parseNewsDataItem(JSONObject newsJson) {
        try {
            NewsDataItem newsDataItem = new NewsDataItem();

            // 标题
            if (newsJson.containsKey("title") && newsJson.get("title") != null) {
                newsDataItem.setTitle(newsJson.getString("title"));
            }

            // URL
            if (newsJson.containsKey("url") && newsJson.get("url") != null) {
                newsDataItem.setContentUrl(newsJson.getString("url"));
            }

            // 发布时间
            if (newsJson.containsKey("showTime") && newsJson.get("showTime") != null) {
                String showTimeStr = newsJson.getString("showTime");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime showTime = LocalDateTime.parse(showTimeStr, formatter);
                newsDataItem.setPublishTime(showTime);
            }

            // 摘要
            if (newsJson.containsKey("summary") && newsJson.get("summary") != null) {
                newsDataItem.setSummary(newsJson.getString("summary"));
            }

            return newsDataItem;

        } catch (Exception e) {
            log.error("解析单条新闻失败", e);
            return null;
        }
    }

    /**
     * 测试方法
     *
     * @param args
     */
    public static void main(String[] args) {
        // testGetNewsList();

        testGetNewsData();
    }

    public static void testGetNewsList() {
        ThreadFactory virtualFactory = Thread.ofVirtual().name("Aspider-virtual-", 0).factory();
        ExecutorService executorService = Executors.newThreadPerTaskExecutor(virtualFactory);

        // 获取数据
        EastMoneyNewsDataSource eastMoneyNewsDataSource = new EastMoneyNewsDataSource(executorService);
        List<NewsDataItem> newsList = eastMoneyNewsDataSource.getNewsList(1, 5);

        // 解析输出
        for (NewsDataItem newsDataItem : newsList) {
            log.info("====================================");
            log.info("标题: {}", newsDataItem.getTitle());
            log.info("URL: {}", newsDataItem.getContentUrl());
            log.info("摘要: {}", newsDataItem.getSummary());
            log.info("发布时间: {}", newsDataItem.getPublishTime());
            log.info("爬取时间: {}", newsDataItem.getCrawlTime());
        }
    }

    public static void testGetNewsData() {
        ThreadFactory virtualFactory = Thread.ofVirtual().name("Aspider-virtual-", 0).factory();
        ExecutorService executorService = Executors.newThreadPerTaskExecutor(virtualFactory);

        NewsDataSource eastMoneyNewsDataSource = new EastMoneyNewsDataSource(executorService);
        List<NewsDataItem> newsData = eastMoneyNewsDataSource.getNewsData();
        log.info("成功获取详情：{} 条", newsData.size());
    }
}
