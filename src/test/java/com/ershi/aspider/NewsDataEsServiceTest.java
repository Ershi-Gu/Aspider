package com.ershi.aspider;

import com.ershi.aspider.datasource.service.NewsDataExecutor;
import com.ershi.aspider.datasource.NewsDataSource;
import com.ershi.aspider.datasource.domain.NewsDataItem;
import com.ershi.aspider.datasource.service.NewsDataEsService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = ASpiderApplication.class)
public class NewsDataEsServiceTest {

    @Resource
    private NewsDataExecutor newsDataExecutor;

    @Resource
    private NewsDataEsService newsDataEsService;

    @Test
    public void filterDuplicates() {
        List<NewsDataSource> allDataSources = newsDataExecutor.getAllDataSources();

        for (NewsDataSource dataSource : allDataSources) {
            List<NewsDataItem> newsDataItems = dataSource.getNewsData();
            newsDataEsService.filterDuplicates(newsDataItems);
            System.out.println("最终增量数据条数：" + newsDataItems.size());
        }
    }
}
