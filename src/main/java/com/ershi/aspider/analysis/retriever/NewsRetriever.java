package com.ershi.aspider.analysis.retriever;

import com.ershi.aspider.analysis.retriever.domain.AnalysisQuery;
import com.ershi.aspider.analysis.retriever.domain.NewsRetrievalResult;
import com.ershi.aspider.analysis.retriever.domain.RetrievalSource;
import com.ershi.aspider.analysis.retriever.domain.RetrievedArticle;
import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import com.ershi.aspider.data.datasource.domain.NewsTypeEnum;
import com.ershi.aspider.data.embedding.service.EmbeddingService;
import com.ershi.aspider.data.storage.elasticsearch.service.FinancialArticleStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 新闻检索器
 * <p>
 * 综合混合检索（语义+关键词）、政策类新闻过滤、高重要性新闻过滤三种检索源，
 * 合并去重后按相关性评分降序返回结果。
 *
 * @author Ershi-Gu
 */
@Service
public class NewsRetriever implements DataRetriever<AnalysisQuery, NewsRetrievalResult> {

    private static final Logger log = LoggerFactory.getLogger(NewsRetriever.class);

    private static final int DEFAULT_NEWS_DAYS = 7;
    private static final int DEFAULT_TOP_K = 20;
    private static final int DEFAULT_MIN_IMPORTANCE = 3;

    /** 混合检索基础评分 */
    private static final double HYBRID_BASE_SCORE = 0.9;
    /** 高重要性新闻基础评分 */
    private static final double IMPORTANT_BASE_SCORE = 0.8;
    /** 政策类新闻基础评分 */
    private static final double POLICY_BASE_SCORE = 0.7;
    /** 排名衰减因子：每下降一位扣减的分数 */
    private static final double RANK_DECAY = 0.01;

    private final FinancialArticleStorageService storageService;
    private final EmbeddingService embeddingService;

    public NewsRetriever(FinancialArticleStorageService storageService, EmbeddingService embeddingService) {
        this.storageService = storageService;
        this.embeddingService = embeddingService;
    }

    @Override
    public NewsRetrievalResult retrieve(AnalysisQuery query) {
        AnalysisQuery safeQuery = query != null ? query : new AnalysisQuery();
        int days = safeQuery.getNewsDays() > 0 ? safeQuery.getNewsDays() : DEFAULT_NEWS_DAYS;
        int topK = safeQuery.getNewsTopK() > 0 ? safeQuery.getNewsTopK() : DEFAULT_TOP_K;
        String queryText = resolveQueryText(safeQuery);

        log.info("开始新闻检索，查询词={}, 天数={}, topK={}", queryText, days, topK);

        // 多源检索
        List<RetrievedArticle> sectorArticles = queryText == null
            ? Collections.emptyList()
            : retrieveBySectorName(queryText, days, topK);
        List<RetrievedArticle> policyArticles = retrievePolicyNews(days, topK);
        List<RetrievedArticle> importantArticles = retrieveImportantNews(DEFAULT_MIN_IMPORTANCE, days, topK);

        int totalCandidates = sectorArticles.size() + policyArticles.size() + importantArticles.size();

        // 合并去重，按评分降序截取 topK
        List<RetrievedArticle> merged = mergeAndDeduplicate(topK, sectorArticles, policyArticles, importantArticles);

        log.info("新闻检索完成，候选{}条，去重后返回{}条", totalCandidates, merged.size());

        NewsRetrievalResult result = new NewsRetrievalResult();
        result.setArticles(merged);
        result.setTotalCandidates(totalCandidates);
        result.setFilteredCount(merged.size());
        return result;
    }

    /**
     * 按板块名称进行混合语义检索
     *
     * @param sectorName 板块名称
     * @param days       时间范围（天）
     * @param topK       返回数量
     * @return 检索到的文章列表
     */
    public List<RetrievedArticle> retrieveBySectorName(String sectorName, int days, int topK) {
        if (sectorName == null || sectorName.isBlank()) {
            return Collections.emptyList();
        }

        List<Float> queryVector = toFloatVector(embeddingService.embed(sectorName));
        if (queryVector.isEmpty()) {
            return Collections.emptyList();
        }

        List<FinancialArticle> articles = storageService.hybridSearch(sectorName, queryVector, topK, days);
        return toRetrievedArticles(articles, RetrievalSource.HYBRID_SEARCH, HYBRID_BASE_SCORE);
    }

    /**
     * 检索政策类新闻
     */
    public List<RetrievedArticle> retrievePolicyNews(int days, int topK) {
        List<FinancialArticle> articles = storageService.findByNewsTypeAndDays(NewsTypeEnum.POLICY, days, topK);
        return toRetrievedArticles(articles, RetrievalSource.TYPE_FILTER, POLICY_BASE_SCORE);
    }

    /**
     * 检索高重要性新闻
     */
    public List<RetrievedArticle> retrieveImportantNews(int minImportance, int days, int topK) {
        List<FinancialArticle> articles = storageService.findByImportanceAndDays(minImportance, days, topK);
        return toRetrievedArticles(articles, RetrievalSource.IMPORTANCE_FILTER, IMPORTANT_BASE_SCORE);
    }

    /**
     * 合并多源检索结果并去重
     * <p>
     * 同一文章在多个源中出现时，保留评分最高的记录。
     */
    @SafeVarargs
    private List<RetrievedArticle> mergeAndDeduplicate(int topK, List<RetrievedArticle>... candidates) {
        Map<String, RetrievedArticle> deduplicated = new LinkedHashMap<>();
        for (List<RetrievedArticle> list : candidates) {
            if (list == null) {
                continue;
            }
            for (RetrievedArticle item : list) {
                if (item == null || item.getArticle() == null) {
                    continue;
                }
                String key = resolveUniqueKey(item.getArticle());
                RetrievedArticle existing = deduplicated.get(key);
                if (existing == null || item.getRelevanceScore() > existing.getRelevanceScore()) {
                    deduplicated.put(key, item);
                }
            }
        }

        List<RetrievedArticle> result = new ArrayList<>(deduplicated.values());
        result.sort(Comparator.comparingDouble(RetrievedArticle::getRelevanceScore).reversed());
        if (topK > 0 && result.size() > topK) {
            return new ArrayList<>(result.subList(0, topK));
        }
        return result;
    }

    /**
     * 将原始文章列表转换为带评分的 RetrievedArticle 列表
     * <p>
     * 评分策略：基础分 - 排名位置 * 衰减因子，确保结果在 [0, 1] 范围内。
     */
    private List<RetrievedArticle> toRetrievedArticles(List<FinancialArticle> articles,
                                                        RetrievalSource source, double baseScore) {
        if (articles == null || articles.isEmpty()) {
            return Collections.emptyList();
        }

        List<RetrievedArticle> result = new ArrayList<>(articles.size());
        for (int i = 0; i < articles.size(); i++) {
            FinancialArticle article = articles.get(i);
            if (article == null) {
                continue;
            }
            RetrievedArticle item = new RetrievedArticle();
            item.setArticle(article);
            item.setSource(source);
            item.setRelevanceScore(normalizeScore(baseScore - i * RANK_DECAY));
            result.add(item);
        }
        return result;
    }

    /** 将分数限制在 [0, 1] 范围内 */
    private double normalizeScore(double rawScore) {
        return Math.max(0, Math.min(1, rawScore));
    }

    /** EmbeddingService 返回 List<Double>，Storage 层 hybridSearch 接收 List<Float>，需要转换 */
    private List<Float> toFloatVector(List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            return Collections.emptyList();
        }
        List<Float> result = new ArrayList<>(vector.size());
        for (Double value : vector) {
            if (value != null) {
                result.add(value.floatValue());
            }
        }
        return result;
    }

    /** 优先使用板块名称作为查询词，其次使用板块代码 */
    private String resolveQueryText(AnalysisQuery query) {
        if (query.getSectorName() != null && !query.getSectorName().isBlank()) {
            return query.getSectorName();
        }
        if (query.getSectorCode() != null && !query.getSectorCode().isBlank()) {
            return query.getSectorCode();
        }
        return null;
    }

    /** 根据 uniqueId 或 title+url 组合生成去重键 */
    private String resolveUniqueKey(FinancialArticle article) {
        if (article.getUniqueId() != null && !article.getUniqueId().isBlank()) {
            return article.getUniqueId();
        }
        String title = article.getTitle() != null ? article.getTitle() : "";
        String url = article.getContentUrl() != null ? article.getContentUrl() : "";
        return title + "|" + url;
    }
}
