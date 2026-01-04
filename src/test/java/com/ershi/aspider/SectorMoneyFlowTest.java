package com.ershi.aspider;

import com.ershi.aspider.datasource.domain.SectorMoneyFlow;
import com.ershi.aspider.datasource.domain.SectorTypeEnum;
import com.ershi.aspider.datasource.provider.EastMoneySectorMoneyFlowDS;
import com.ershi.aspider.storage.elasticsearch.service.SectorMoneyFlowStorageService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = ASpiderApplication.class)
  public class SectorMoneyFlowTest {

      @Resource
      private EastMoneySectorMoneyFlowDS sectorMoneyFlowDS;

      @Resource
      private SectorMoneyFlowStorageService storageService;

      /** 测试数据源获取 */
      @Test
      public void testGetSectorMoneyFlow() {
          List<SectorMoneyFlow> data = sectorMoneyFlowDS.getSectorMoneyFlow(SectorTypeEnum.INDUSTRY);
          System.out.println("行业板块数量: " + data.size());

          if (!data.isEmpty()) {
              SectorMoneyFlow first = data.get(0);
              System.out.printf("示例: %s, 涨跌幅: %s%%, 主力净流入: %s%n",
                  first.getSectorName(), first.getChangePercent(), first.getMainNetInflow());
          }
      }

      /** 测试存储到ES（需要先创建索引） */
      @Test
      public void testSaveToEs() {
          List<SectorMoneyFlow> data = sectorMoneyFlowDS.getAllSectorMoneyFlow();
          int count = storageService.batchSaveToEs(data);
          System.out.println("保存成功: " + count);
      }
  }