package com.ershi.aspider;

import com.ershi.aspider.datasource.domain.FinancialArticleDSTypeEnum;
import com.ershi.aspider.job.FinancialArticleDataJob;
import com.ershi.aspider.datasource.service.FinancialArticleDSFactory;
import com.ershi.aspider.datasource.provider.FinancialArticleDataSource;
import com.ershi.aspider.datasource.domain.FinancialArticle;
import com.ershi.aspider.orchestration.service.FinancialArticleDataService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = ASpiderApplication.class)
public class FinancialArticleDataServiceTest {

    @Resource
    private FinancialArticleDSFactory financialArticleDSFactory;

    @Resource
    private FinancialArticleDataService financialArticleDataService;

    @Resource
    private FinancialArticleDataJob financialArticleDataJob;

    @Test
    public void testFinancialArticleSource() {
        FinancialArticleDataSource dataSource = financialArticleDSFactory.getDataSource(FinancialArticleDSTypeEnum.EAST_MONEY);
        List<FinancialArticle> financialArticle = dataSource.getFinancialArticle();
        System.out.println(financialArticle);
    }

    @Test
    public void testFinancialArticleJob() {
        financialArticleDataJob.processSpecificDataSource(FinancialArticleDSTypeEnum.EAST_MONEY);
    }
}
