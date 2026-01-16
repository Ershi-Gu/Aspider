package com.ershi.aspider.data.datasource.provider;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ershi.aspider.data.datasource.domain.SectorQuote;
import com.ershi.aspider.data.datasource.domain.SectorTypeEnum;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 东方财富板块行情数据源
 * <p>
 * API字段映射：
 * f12: 板块代码, f14: 板块名称
 * f2: 最新价, f3: 涨跌幅(%), f4: 涨跌额
 * f5: 成交量(手), f6: 成交额(元), f7: 振幅(%)
 * f8: 换手率(%), f15: 最高价, f16: 最低价, f17: 开盘价
 * f104: 上涨家数, f105: 下跌家数, f124: 时间戳
 *
 * @author Ershi-Gu.
 * @since 2025-12-25
 */
@Component
public class EastMoneySectorQuoteDS implements SectorQuoteDataSource {

    private static final Logger log = LoggerFactory.getLogger(EastMoneySectorQuoteDS.class);

    private final CloseableHttpClient httpClient;

    /** 东财板块行情API */
    private static final String API_URL = "https://push2.eastmoney.com/api/qt/clist/get?" +
        "pn=1&pz=500&po=1&np=1&fltt=2&invt=2&fid=f3&fs={fs}" +
        "&fields=f12,f14,f2,f3,f4,f5,f6,f7,f8,f15,f16,f17,f104,f105,f124";

    public EastMoneySectorQuoteDS() {
        this.httpClient = HttpClients.createDefault();
    }

    @Override
    public List<SectorQuote> getSectorQuote(SectorTypeEnum sectorType) {
        log.info("开始获取{}行情数据", sectorType.getDesc());

        String url = API_URL.replace("{fs}", sectorType.getEastMoneyFsParam());

        try {
            String jsonData = fetchData(url);
            if (jsonData == null || jsonData.isEmpty()) {
                log.error("获取{}行情数据失败", sectorType.getDesc());
                return new ArrayList<>();
            }

            List<SectorQuote> result = parseQuoteData(jsonData, sectorType);
            log.info("成功获取 {} 条{}行情数据", result.size(), sectorType.getDesc());
            return result;

        } catch (IOException e) {
            log.error("获取{}行情数据异常", sectorType.getDesc(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<SectorQuote> getAllSectorQuote() {
        List<SectorQuote> allData = new ArrayList<>();

        for (SectorTypeEnum sectorType : SectorTypeEnum.values()) {
            List<SectorQuote> data = getSectorQuote(sectorType);
            allData.addAll(data);
        }

        log.info("共获取 {} 条板块行情数据", allData.size());
        return allData;
    }

    /**
     * 发送HTTP请求获取数据
     */
    private String fetchData(String url) throws IOException {
        HttpGet httpGet = new HttpGet(url);

        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        httpGet.setHeader("Referer", "https://quote.eastmoney.com/");
        httpGet.setHeader("Accept", "*/*");

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
     * 解析行情数据
     */
    private List<SectorQuote> parseQuoteData(String jsonData, SectorTypeEnum sectorType) {
        List<SectorQuote> result = new ArrayList<>();

        try {
            JSONObject root = JSON.parseObject(jsonData);

            // 检查响应状态
            Integer rc = root.getInteger("rc");
            if (rc == null || rc != 0) {
                log.error("API返回错误，rc: {}", rc);
                return result;
            }

            JSONObject data = root.getJSONObject("data");
            if (data == null) {
                log.error("响应中没有data字段");
                return result;
            }

            JSONArray diff = data.getJSONArray("diff");
            if (diff == null || diff.isEmpty()) {
                log.warn("板块数据列表为空");
                return result;
            }

            // 解析每条数据
            for (int i = 0; i < diff.size(); i++) {
                JSONObject item = diff.getJSONObject(i);
                SectorQuote quote = parseSingleItem(item, sectorType);
                if (quote != null) {
                    result.add(quote);
                }
            }

        } catch (Exception e) {
            log.error("解析行情数据失败", e);
        }

        return result;
    }

    /**
     * 解析单条数据
     */
    private SectorQuote parseSingleItem(JSONObject item, SectorTypeEnum sectorType) {
        try {
            SectorQuote quote = new SectorQuote();

            // 板块代码和名称
            quote.setSectorCode(item.getString("f12"));
            quote.setSectorName(item.getString("f14"));
            quote.setSectorType(sectorType.getType());

            // 交易日期（从时间戳转换）
            Long timestamp = item.getLong("f124");
            if (timestamp != null) {
                LocalDate tradeDate = Instant.ofEpochSecond(timestamp)
                    .atZone(ZoneId.of("Asia/Shanghai"))
                    .toLocalDate();
                quote.setTradeDate(tradeDate);
            } else {
                quote.setTradeDate(LocalDate.now());
            }

            // 价格数据
            quote.setClosePrice(getBigDecimal(item, "f2"));
            quote.setOpenPrice(getBigDecimal(item, "f17"));
            quote.setHighPrice(getBigDecimal(item, "f15"));
            quote.setLowPrice(getBigDecimal(item, "f16"));

            // 涨跌数据
            quote.setChangePercent(getBigDecimal(item, "f3"));
            quote.setChangeAmount(getBigDecimal(item, "f4"));

            // 成交数据
            quote.setVolume(getBigDecimal(item, "f5"));
            quote.setAmount(getBigDecimal(item, "f6"));

            // 其他指标
            quote.setAmplitude(getBigDecimal(item, "f7"));
            quote.setTurnoverRate(getBigDecimal(item, "f8"));

            // 涨跌家数
            quote.setRiseCount(item.getInteger("f104"));
            quote.setFallCount(item.getInteger("f105"));

            // 计算成分股数量（上涨+下跌+平盘）
            Integer riseCount = quote.getRiseCount();
            Integer fallCount = quote.getFallCount();
            if (riseCount != null && fallCount != null) {
                // 简单估算，实际可能需要另外获取
                quote.setCompanyCount(riseCount + fallCount);
            }

            // 生成唯一ID
            quote.generateUniqueId();

            return quote;

        } catch (Exception e) {
            log.error("解析单条行情数据失败", e);
            return null;
        }
    }

    /**
     * 安全获取BigDecimal
     */
    private BigDecimal getBigDecimal(JSONObject item, String key) {
        Object value = item.get(key);
        if (value == null || "-".equals(value.toString())) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        EastMoneySectorQuoteDS ds = new EastMoneySectorQuoteDS();

        // 测试获取行业板块行情
        List<SectorQuote> industryData = ds.getSectorQuote(SectorTypeEnum.INDUSTRY);
        log.info("行业板块数量: {}", industryData.size());

        if (!industryData.isEmpty()) {
            SectorQuote first = industryData.get(0);
            log.info("示例数据 - 板块: {}, 涨跌幅: {}%, 成交额: {}",
                     first.getSectorName(),
                     first.getChangePercent(),
                     first.getAmount());
        }

        // 测试获取概念板块行情
        List<SectorQuote> conceptData = ds.getSectorQuote(SectorTypeEnum.CONCEPT);
        log.info("概念板块数量: {}", conceptData.size());
    }
}