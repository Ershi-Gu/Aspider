package com.ershi.aspider.data.datasource.provider;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ershi.aspider.data.datasource.domain.SectorMoneyFlow;
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
 * 东方财富板块资金流向数据源
 * <p>
 * API字段映射：
 * f12: 板块代码, f14: 板块名称, f3: 涨跌幅(%)
 * f62: 主力净流入, f184: 主力净流入占比(%)
 * f66: 超大单净流入, f69: 超大单净流入占比(%)
 * f72: 大单净流入, f75: 大单净流入占比(%)
 * f78: 中单净流入, f81: 中单净流入占比(%)
 * f84: 小单净流入, f87: 小单净流入占比(%)
 * f204: 领涨股名称, f205: 领涨股代码
 * f124: 时间戳
 *
 * @author Ershi-Gu.
 * @since 2025-12-25
 */
@Component
public class EastMoneySectorMoneyFlowDS implements SectorMoneyFlowDataSource {

    private static final Logger log = LoggerFactory.getLogger(EastMoneySectorMoneyFlowDS.class);

    private final CloseableHttpClient httpClient;

    /** 东财板块资金流向API */
    private static final String API_URL = "https://push2.eastmoney.com/api/qt/clist/get?" +
        "pn=1&pz=500&po=1&np=1&fltt=2&invt=2&fid=f62&fs={fs}" +
        "&fields=f12,f14,f3,f62,f184,f66,f69,f72,f75,f78,f81,f84,f87,f204,f205,f124";

    public EastMoneySectorMoneyFlowDS() {
        this.httpClient = HttpClients.createDefault();
    }

    @Override
    public List<SectorMoneyFlow> getSectorMoneyFlow(SectorTypeEnum sectorType) {
        log.info("开始获取{}资金流向数据", sectorType.getDesc());

        String url = API_URL.replace("{fs}", sectorType.getEastMoneyFsParam());

        try {
            String jsonData = fetchData(url);
            if (jsonData == null || jsonData.isEmpty()) {
                log.error("获取{}资金流向数据失败", sectorType.getDesc());
                return new ArrayList<>();
            }

            List<SectorMoneyFlow> result = parseMoneyFlowData(jsonData, sectorType);
            log.info("成功获取 {} 条{}资金流向数据", result.size(), sectorType.getDesc());
            return result;

        } catch (IOException e) {
            log.error("获取{}资金流向数据异常", sectorType.getDesc(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<SectorMoneyFlow> getAllSectorMoneyFlow() {
        List<SectorMoneyFlow> allData = new ArrayList<>();

        for (SectorTypeEnum sectorType : SectorTypeEnum.values()) {
            List<SectorMoneyFlow> data = getSectorMoneyFlow(sectorType);
            allData.addAll(data);
        }

        log.info("共获取 {} 条板块资金流向数据", allData.size());
        return allData;
    }

    /**
     * 发送HTTP请求获取数据
     */
    private String fetchData(String url) throws IOException {
        HttpGet httpGet = new HttpGet(url);

        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        httpGet.setHeader("Referer", "https://data.eastmoney.com/");
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
     * 解析资金流向数据
     */
    private List<SectorMoneyFlow> parseMoneyFlowData(String jsonData, SectorTypeEnum sectorType) {
        List<SectorMoneyFlow> result = new ArrayList<>();

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
                SectorMoneyFlow flow = parseSingleItem(item, sectorType);
                if (flow != null) {
                    result.add(flow);
                }
            }

        } catch (Exception e) {
            log.error("解析资金流向数据失败", e);
        }

        return result;
    }

    /**
     * 解析单条数据
     */
    private SectorMoneyFlow parseSingleItem(JSONObject item, SectorTypeEnum sectorType) {
        try {
            SectorMoneyFlow flow = new SectorMoneyFlow();

            // 板块代码和名称
            flow.setSectorCode(item.getString("f12"));
            flow.setSectorName(item.getString("f14"));
            flow.setSectorType(sectorType.getType());

            // 交易日期（从时间戳转换）
            Long timestamp = item.getLong("f124");
            if (timestamp != null) {
                LocalDate tradeDate = Instant.ofEpochSecond(timestamp)
                    .atZone(ZoneId.of("Asia/Shanghai"))
                    .toLocalDate();
                flow.setTradeDate(tradeDate);
            } else {
                flow.setTradeDate(LocalDate.now());
            }

            // 涨跌幅
            flow.setChangePercent(getBigDecimal(item, "f3"));

            // 主力净流入
            flow.setMainNetInflow(getBigDecimal(item, "f62"));
            flow.setMainNetInflowRatio(getBigDecimal(item, "f184"));

            // 超大单
            flow.setSuperLargeInflow(getBigDecimal(item, "f66"));
            flow.setSuperLargeInflowRatio(getBigDecimal(item, "f69"));

            // 大单
            flow.setLargeInflow(getBigDecimal(item, "f72"));
            flow.setLargeInflowRatio(getBigDecimal(item, "f75"));

            // 中单
            flow.setMediumInflow(getBigDecimal(item, "f78"));
            flow.setMediumInflowRatio(getBigDecimal(item, "f81"));

            // 小单
            flow.setSmallInflow(getBigDecimal(item, "f84"));
            flow.setSmallInflowRatio(getBigDecimal(item, "f87"));

            // 领涨股
            flow.setLeadStock(item.getString("f205"));
            flow.setLeadStockName(item.getString("f204"));

            // 生成唯一ID
            flow.generateUniqueId();

            return flow;

        } catch (Exception e) {
            log.error("解析单条资金流向数据失败", e);
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
        EastMoneySectorMoneyFlowDS ds = new EastMoneySectorMoneyFlowDS();

        // 测试获取行业板块资金流向
        List<SectorMoneyFlow> industryData = ds.getSectorMoneyFlow(SectorTypeEnum.INDUSTRY);
        log.info("行业板块数量: {}", industryData.size());

        if (!industryData.isEmpty()) {
            SectorMoneyFlow first = industryData.get(0);
            log.info("示例数据 - 板块: {}, 涨跌幅: {}%, 主力净流入: {}",
                     first.getSectorName(),
                     first.getChangePercent(),
                     first.getMainNetInflow());
        }

        // 测试获取概念板块资金流向
        List<SectorMoneyFlow> conceptData = ds.getSectorMoneyFlow(SectorTypeEnum.CONCEPT);
        log.info("概念板块数量: {}", conceptData.size());
    }
}