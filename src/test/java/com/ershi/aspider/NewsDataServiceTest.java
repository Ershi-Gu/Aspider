package com.ershi.aspider;

import com.ershi.aspider.datasource.domain.NewsDataSourceTypeEnum;
import com.ershi.aspider.datasource.job.NewsDataJob;
import com.ershi.aspider.datasource.service.NewsDataFactory;
import com.ershi.aspider.datasource.provider.NewsDataSource;
import com.ershi.aspider.datasource.domain.NewsDataItem;
import com.ershi.aspider.orchestration.service.NewsDataService;
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
    public void testNewsDataSource() {
        NewsDataSource dataSource = newsDataFactory.getDataSource(NewsDataSourceTypeEnum.EAST_MONEY);
        List<NewsDataItem> newsData = dataSource.getNewsData();
        System.out.println(newsData);
    }

    @Test
    public void testNewsDataJob() {
        newsDataJob.processSpecificDataSource(NewsDataSourceTypeEnum.EAST_MONEY);
    }
}
