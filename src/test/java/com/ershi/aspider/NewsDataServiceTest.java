package com.ershi.aspider;

import com.ershi.aspider.datasource.domain.NewsDataSourceTypeEnum;
import com.ershi.aspider.datasource.job.NewsDataJob;
import com.ershi.aspider.datasource.service.NewsDataFactory;
import com.ershi.aspider.datasource.NewsDataSource;
import com.ershi.aspider.datasource.domain.NewsDataItem;
import com.ershi.aspider.datasource.service.NewsDataService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = ASpiderApplication.class)
public class NewsDataServiceTest {

    @Resource
    private NewsDataFactory newsDataFactory;

    @Resource
    private NewsDataService newsDataService;

    @Resource
    private NewsDataJob newsDataJob;

    @Test
    public void filterDuplicates() {
        List<NewsDataSource> allDataSources = newsDataFactory.getAllDataSources();

        for (NewsDataSource dataSource : allDataSources) {
            List<NewsDataItem> newsDataItems = dataSource.getNewsData();
            newsDataService.filterDuplicates(newsDataItems);
            System.out.println("最终增量数据条数：" + newsDataItems.size());
        }
    }

    @Test
    public void testNewsDataJob() {
        newsDataJob.processSpecificDataSource(NewsDataSourceTypeEnum.EAST_MONEY);
    }
}
