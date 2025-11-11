package com.ershi.aspider.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Elasticsearch 9 操作工具类 封装常用的ES操作，提供简洁的API
 *
 * @author ershi
 */
public class ElasticsearchUtils {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchUtils.class);

    private final ElasticsearchClient client;

    public ElasticsearchUtils(ElasticsearchClient client) {
        this.client = client;
    }

    // ==================== 索引管理操作 ====================

    /**
     * 创建索引
     *
     * @param indexName 索引名称
     * @return 是否创建成功
     */
    public boolean createIndex(String indexName) {
        try {
            if (indexExists(indexName)) {
                logger.warn("索引已存在 - 索引: {}", indexName);
                return true;
            }

            CreateIndexRequest request = CreateIndexRequest.of(b -> b.index(indexName));

            CreateIndexResponse response = client.indices().create(request);

            if (response.acknowledged()) {
                logger.info("索引创建成功 - 索引: {}", indexName);
                return true;
            } else {
                logger.error("索引创建失败 - 索引: {}", indexName);
                return false;
            }
        } catch (IOException e) {
            logger.error("创建索引失败 - 索引: {}", indexName, e);
            return false;
        }
    }

    /**
     * 创建索引（带设置和映射）
     *
     * @param indexName    索引名称
     * @param settingsJson 设置JSON
     * @param mappingsJson 映射JSON
     * @return 是否创建成功
     */
    public boolean createIndex(String indexName, String settingsJson, String mappingsJson) {
        try {
            if (indexExists(indexName)) {
                logger.warn("索引已存在 - 索引: {}", indexName);
                return true;
            }

            String indexConfigJson = String.format("{\"settings\": %s, \"mappings\": %s}", settingsJson, mappingsJson);

            CreateIndexRequest request =
                CreateIndexRequest.of(b -> b.index(indexName).withJson(new StringReader(indexConfigJson)));

            CreateIndexResponse response = client.indices().create(request);

            if (response.acknowledged()) {
                logger.info("索引创建成功 - 索引: {}", indexName);
                return true;
            } else {
                logger.error("索引创建失败 - 索引: {}", indexName);
                return false;
            }
        } catch (IOException e) {
            logger.error("创建索引失败 - 索引: {}", indexName, e);
            return false;
        }
    }

    /**
     * 创建索引（使用完整的配置JSON）
     *
     * @param indexName  索引名称
     * @param configJson 完整配置JSON（包含settings和mappings）
     * @return 是否创建成功
     */
    public boolean createIndexWithJson(String indexName, String configJson) {
        try {
            if (indexExists(indexName)) {
                logger.warn("索引已存在 - 索引: {}", indexName);
                return true;
            }

            CreateIndexRequest request =
                CreateIndexRequest.of(b -> b.index(indexName).withJson(new StringReader(configJson)));

            CreateIndexResponse response = client.indices().create(request);

            if (response.acknowledged()) {
                logger.info("索引创建成功 - 索引: {}", indexName);
                return true;
            } else {
                logger.error("索引创建失败 - 索引: {}", indexName);
                return false;
            }
        } catch (IOException e) {
            logger.error("创建索引失败 - 索引: {}", indexName, e);
            return false;
        }
    }

    /**
     * 删除索引
     *
     * @param indexName 索引名称
     * @return 是否删除成功
     */
    public boolean deleteIndex(String indexName) {
        try {
            if (!indexExists(indexName)) {
                logger.warn("索引不存在 - 索引: {}", indexName);
                return true;
            }

            DeleteIndexRequest request = DeleteIndexRequest.of(d -> d.index(indexName));

            DeleteIndexResponse response = client.indices().delete(request);

            if (response.acknowledged()) {
                logger.info("索引删除成功 - 索引: {}", indexName);
                return true;
            } else {
                logger.error("索引删除失败 - 索引: {}", indexName);
                return false;
            }
        } catch (IOException e) {
            logger.error("删除索引失败 - 索引: {}", indexName, e);
            return false;
        }
    }

    /**
     * 检查索引是否存在
     *
     * @param indexName 索引名称
     * @return 是否存在
     */
    public boolean indexExists(String indexName) {
        try {
            co.elastic.clients.elasticsearch.indices.ExistsRequest request =
                co.elastic.clients.elasticsearch.indices.ExistsRequest.of(e -> e.index(indexName));

            BooleanResponse response = client.indices().exists(request);
            return response.value();
        } catch (IOException e) {
            logger.error("检查索引是否存在失败 - 索引: {}", indexName, e);
            return false;
        }
    }

    /**
     * 获取索引信息
     *
     * @param indexName 索引名称
     * @return 索引状态信息（映射、设置等），失败返回null
     */
    public GetIndexResponse getIndexInfo(String indexName) {
        try {
            if (!indexExists(indexName)) {
                logger.warn("索引不存在 - 索引: {}", indexName);
                return null;
            }

            GetIndexRequest request = GetIndexRequest.of(g -> g.index(indexName));

            GetIndexResponse response = client.indices().get(request);
            logger.debug("获取索引信息成功 - 索引: {}", indexName);
            return response;
        } catch (IOException e) {
            logger.error("获取索引信息失败 - 索引: {}", indexName, e);
            return null;
        }
    }

    /**
     * 重建索引（删除后重新创建）
     *
     * @param indexName  索引名称
     * @param configJson 配置JSON
     * @return 是否重建成功
     */
    public boolean recreateIndex(String indexName, String configJson) {
        logger.warn("准备重建索引 - 索引: {}", indexName);

        // 删除旧索引
        if (indexExists(indexName)) {
            if (!deleteIndex(indexName)) {
                logger.error("删除旧索引失败，重建终止 - 索引: {}", indexName);
                return false;
            }
        }

        // 创建新索引
        boolean result = createIndexWithJson(indexName, configJson);

        if (result) {
            logger.info("索引重建成功 - 索引: {}", indexName);
        } else {
            logger.error("索引重建失败 - 索引: {}", indexName);
        }

        return result;
    }

    // ==================== 文档索引操作 ====================

    /**
     * 索引单个文档（不指定ID，自动生成）
     *
     * @param index    索引名称
     * @param document 文档对象
     * @param <T>      文档类型
     * @return 生成的文档ID，失败返回null
     */
    public <T> String indexDocument(String index, T document) {
        return indexDocument(index, null, document);
    }

    /**
     * 索引单个文档（指定ID）
     *
     * @param index    索引名称
     * @param id       文档ID
     * @param document 需要存储的文档对象
     * @param <T>      文档类型
     * @return 文档ID，失败返回null
     */
    public <T> String indexDocument(String index, String id, T document) {
        try {
            IndexRequest.Builder<T> builder = new IndexRequest.Builder<T>().index(index).document(document);

            if (id != null && !id.isEmpty()) {
                builder.id(id);
            }

            IndexResponse response = client.index(builder.build());

            if (response.result() == Result.Created || response.result() == Result.Updated) {
                logger.debug("文档索引成功 - 索引: {}, ID: {}, 结果: {}", index, response.id(), response.result());
                return response.id();
            } else {
                logger.warn("文档索引结果异常 - 索引: {}, ID: {}, 结果: {}", index, response.id(), response.result());
                return null;
            }
        } catch (IOException e) {
            logger.error("索引文档失败 - 索引: {}, ID: {}", index, id, e);
            return null;
        }
    }

    /**
     * 批量索引文档
     *
     * @param index     索引名称
     * @param documents 文档列表（Map的key为文档ID，value为文档对象）
     * @param <T>       文档类型
     * @return 成功索引的文档数量
     */
    public <T> int bulkIndexDocuments(String index, Map<String, T> documents) {
        if (documents == null || documents.isEmpty()) {
            logger.warn("批量索引文档列表为空");
            return 0;
        }

        try {
            List<BulkOperation> operations = new ArrayList<>();

            for (Map.Entry<String, T> entry : documents.entrySet()) {
                operations.add(BulkOperation.of(
                    op -> op.index(idx -> idx.index(index).id(entry.getKey()).document(entry.getValue()))));
            }

            BulkRequest request = BulkRequest.of(b -> b.operations(operations));

            BulkResponse response = client.bulk(request);

            int successCount = 0;
            int failCount = 0;

            if (response.errors()) {
                for (var item : response.items()) {
                    if (item.error() != null) {
                        logger.error("批量索引失败项 - ID: {}, 原因: {}", item.id(), item.error().reason());
                        failCount++;
                    } else {
                        successCount++;
                    }
                }
            } else {
                successCount = response.items().size();
            }

            logger.info("批量索引完成 - 索引: {}, 总数: {}, 成功: {}, 失败: {}", index, documents.size(), successCount,
                failCount);

            return successCount;
        } catch (IOException e) {
            logger.error("批量索引文档失败 - 索引: {}", index, e);
            return 0;
        }
    }

    /**
     * 批量索引文档（不指定ID）
     *
     * @param index     索引名称
     * @param documents 文档列表
     * @param <T>       文档类型
     * @return 成功索引的文档数量
     */
    public <T> int bulkIndexDocuments(String index, List<T> documents) {
        if (documents == null || documents.isEmpty()) {
            logger.warn("批量索引文档列表为空");
            return 0;
        }

        try {
            List<BulkOperation> operations = new ArrayList<>();

            for (T document : documents) {
                operations.add(BulkOperation.of(op -> op.index(idx -> idx.index(index).document(document))));
            }

            BulkRequest request = BulkRequest.of(b -> b.operations(operations));

            BulkResponse response = client.bulk(request);

            int successCount = 0;
            int failCount = 0;

            if (response.errors()) {
                for (var item : response.items()) {
                    if (item.error() != null) {
                        logger.error("批量索引失败项 - 原因: {}", item.error().reason());
                        failCount++;
                    } else {
                        successCount++;
                    }
                }
            } else {
                successCount = response.items().size();
            }

            logger.info("批量索引完成 - 索引: {}, 总数: {}, 成功: {}, 失败: {}", index, documents.size(), successCount,
                failCount);

            return successCount;
        } catch (IOException e) {
            logger.error("批量索引文档失败 - 索引: {}", index, e);
            return 0;
        }
    }

    // ==================== 文档查询操作 ====================

    /**
     * 根据ID获取文档
     *
     * @param index 索引名称
     * @param id    文档ID
     * @param clazz 文档类型
     * @param <T>   文档类型
     * @return 文档对象，不存在或失败返回null
     */
    public <T> T getDocumentById(String index, String id, Class<T> clazz) {
        try {
            GetRequest request = GetRequest.of(g -> g.index(index).id(id));

            GetResponse<T> response = client.get(request, clazz);

            if (response.found()) {
                logger.debug("获取文档成功 - 索引: {}, ID: {}", index, id);
                return response.source();
            } else {
                logger.debug("文档不存在 - 索引: {}, ID: {}", index, id);
                return null;
            }
        } catch (IOException e) {
            logger.error("获取文档失败 - 索引: {}, ID: {}", index, id, e);
            return null;
        }
    }

    /**
     * 批量获取文档
     *
     * @param index 索引名称
     * @param ids   文档ID列表
     * @param clazz 文档类型
     * @param <T>   文档类型
     * @return 文档列表
     */
    public <T> List<T> getDocumentsByIds(String index, List<String> ids, Class<T> clazz) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            MgetRequest request = MgetRequest.of(m -> m.index(index).ids(ids));

            MgetResponse<T> response = client.mget(request, clazz);

            List<T> documents = new ArrayList<>();
            for (var item : response.docs()) {
                if (item.isResult() && item.result().found()) {
                    documents.add(item.result().source());
                }
            }

            logger.debug("批量获取文档完成 - 索引: {}, 请求数: {}, 获取数: {}", index, ids.size(), documents.size());
            return documents;
        } catch (IOException e) {
            logger.error("批量获取文档失败 - 索引: {}", index, e);
            return Collections.emptyList();
        }
    }

    // ==================== 文档更新操作 ====================

    /**
     * 更新文档（部分更新）
     *
     * @param index 索引名称
     * @param id    文档ID
     * @param doc   要更新的字段
     * @param clazz 文档类型
     * @param <T>   文档类型
     * @return 是否更新成功
     */
    public <T> boolean updateDocument(String index, String id, T doc, Class<T> clazz) {
        try {
            UpdateRequest<T, T> request = UpdateRequest.of(u -> u.index(index).id(id).doc(doc));

            UpdateResponse<T> response = client.update(request, clazz);

            if (response.result() == Result.Updated || response.result() == Result.NoOp) {
                logger.debug("文档更新成功 - 索引: {}, ID: {}, 结果: {}", index, id, response.result());
                return true;
            } else {
                logger.warn("文档更新结果异常 - 索引: {}, ID: {}, 结果: {}", index, id, response.result());
                return false;
            }
        } catch (IOException e) {
            logger.error("更新文档失败 - 索引: {}, ID: {}", index, id, e);
            return false;
        }
    }

    /**
     * Upsert操作（文档存在则更新，不存在则插入）
     *
     * @param index     索引名称
     * @param id        文档ID
     * @param doc       要更新的文档
     * @param upsertDoc 不存在时插入的文档
     * @param clazz     文档类型
     * @param <T>       文档类型
     * @return 是否操作成功
     */
    public <T> boolean upsertDocument(String index, String id, T doc, T upsertDoc, Class<T> clazz) {
        try {
            UpdateRequest<T, T> request = UpdateRequest.of(u -> u.index(index).id(id).doc(doc).upsert(upsertDoc));

            UpdateResponse<T> response = client.update(request, clazz);

            logger.debug("文档Upsert成功 - 索引: {}, ID: {}, 结果: {}", index, id, response.result());
            return true;
        } catch (IOException e) {
            logger.error("Upsert文档失败 - 索引: {}, ID: {}", index, id, e);
            return false;
        }
    }

    // ==================== 文档删除操作 ====================

    /**
     * 删除单个文档
     *
     * @param index 索引名称
     * @param id    文档ID
     * @return 是否删除成功
     */
    public boolean deleteDocument(String index, String id) {
        try {
            DeleteRequest request = DeleteRequest.of(d -> d.index(index).id(id));

            DeleteResponse response = client.delete(request);

            if (response.result() == Result.Deleted) {
                logger.debug("文档删除成功 - 索引: {}, ID: {}", index, id);
                return true;
            } else if (response.result() == Result.NotFound) {
                logger.debug("文档不存在 - 索引: {}, ID: {}", index, id);
                return false;
            } else {
                logger.warn("文档删除结果异常 - 索引: {}, ID: {}, 结果: {}", index, id, response.result());
                return false;
            }
        } catch (IOException e) {
            logger.error("删除文档失败 - 索引: {}, ID: {}", index, id, e);
            return false;
        }
    }

    /**
     * 批量删除文档
     *
     * @param index 索引名称
     * @param ids   文档ID列表
     * @return 成功删除的文档数量
     */
    public int bulkDeleteDocuments(String index, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            logger.warn("批量删除文档ID列表为空");
            return 0;
        }

        try {
            List<BulkOperation> operations = new ArrayList<>();

            for (String id : ids) {
                operations.add(BulkOperation.of(op -> op.delete(del -> del.index(index).id(id))));
            }

            BulkRequest request = BulkRequest.of(b -> b.operations(operations));

            BulkResponse response = client.bulk(request);

            int successCount = 0;
            int failCount = 0;

            if (response.errors()) {
                for (var item : response.items()) {
                    if (item.error() != null) {
                        logger.error("批量删除失败项 - ID: {}, 原因: {}", item.id(), item.error().reason());
                        failCount++;
                    } else {
                        successCount++;
                    }
                }
            } else {
                successCount = response.items().size();
            }

            logger.info("批量删除完成 - 索引: {}, 总数: {}, 成功: {}, 失败: {}", index, ids.size(), successCount,
                failCount);

            return successCount;
        } catch (IOException e) {
            logger.error("批量删除文档失败 - 索引: {}", index, e);
            return 0;
        }
    }

    /**
     * 根据查询条件删除文档
     *
     * @param index 索引名称
     * @param query 查询条件
     * @return 删除的文档数量
     */
    public long deleteByQuery(String index, Query query) {
        try {
            DeleteByQueryRequest request = DeleteByQueryRequest.of(d -> d.index(index).query(query));

            DeleteByQueryResponse response = client.deleteByQuery(request);

            logger.info("根据查询删除完成 - 索引: {}, 删除数量: {}", index, response.deleted());
            return response.deleted() != null ? response.deleted() : 0;
        } catch (IOException e) {
            logger.error("根据查询删除文档失败 - 索引: {}", index, e);
            return 0;
        }
    }

    // ==================== 搜索操作 ====================

    /**
     * 搜索文档（使用Query对象）
     *
     * @param index 索引名称
     * @param query 查询条件
     * @param from  起始位置
     * @param size  返回数量
     * @param clazz 文档类型
     * @param <T>   文档类型
     * @return 搜索结果列表
     */
    public <T> List<T> search(String index, Query query, int from, int size, Class<T> clazz) {
        try {
            SearchRequest request = SearchRequest.of(s -> s.index(index).query(query).from(from).size(size));

            SearchResponse<T> response = client.search(request, clazz);

            List<T> results = new ArrayList<>();
            for (Hit<T> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    results.add(hit.source());
                }
            }

            logger.debug("搜索完成 - 索引: {}, 命中: {}, 返回: {}", index, response.hits().total().value(),
                results.size());
            return results;
        } catch (IOException e) {
            logger.error("搜索文档失败 - 索引: {}", index, e);
            return Collections.emptyList();
        }
    }

    /**
     * 搜索文档（使用函数式构建查询）
     *
     * @param index        索引名称
     * @param queryBuilder 查询构建器（Function）
     * @param from         起始位置
     * @param size         返回数量
     * @param clazz        文档类型
     * @param <T>          文档类型
     * @return 搜索结果列表
     */
    public <T> List<T> search(String index,
        Function<Query.Builder, co.elastic.clients.util.ObjectBuilder<Query>> queryBuilder, int from, int size,
        Class<T> clazz) {
        Query query = Query.of(queryBuilder);
        return search(index, query, from, size, clazz);
    }

    /**
     * 搜索所有文档
     *
     * @param index 索引名称
     * @param from  起始位置
     * @param size  返回数量
     * @param clazz 文档类型
     * @param <T>   文档类型
     * @return 搜索结果列表
     */
    public <T> List<T> searchAll(String index, int from, int size, Class<T> clazz) {
        return search(index, q -> q.matchAll(m -> m), from, size, clazz);
    }

    /**
     * 全文搜索（匹配查询）
     *
     * @param index 索引名称
     * @param field 字段名
     * @param text  搜索文本
     * @param from  起始位置
     * @param size  返回数量
     * @param clazz 文档类型
     * @param <T>   文档类型
     * @return 搜索结果列表
     */
    public <T> List<T> matchSearch(String index, String field, String text, int from, int size, Class<T> clazz) {
        return search(index, q -> q.match(m -> m.field(field).query(text)), from, size, clazz);
    }

    /**
     * 精确匹配搜索（term查询）
     *
     * @param index 索引名称
     * @param field 字段名
     * @param value 精确值
     * @param from  起始位置
     * @param size  返回数量
     * @param clazz 文档类型
     * @param <T>   文档类型
     * @return 搜索结果列表
     */
    public <T> List<T> termSearch(String index, String field, String value, int from, int size, Class<T> clazz) {
        return search(index, q -> q.term(t -> t.field(field).value(value)), from, size, clazz);
    }

    /**
     * 多字段搜索
     *
     * @param index  索引名称
     * @param fields 字段列表
     * @param text   搜索文本
     * @param from   起始位置
     * @param size   返回数量
     * @param clazz  文档类型
     * @param <T>    文档类型
     * @return 搜索结果列表
     */
    public <T> List<T> multiMatchSearch(String index, List<String> fields, String text, int from, int size,
        Class<T> clazz) {
        return search(index, q -> q.multiMatch(m -> m.fields(fields).query(text)), from, size, clazz);
    }

    // ==================== 统计操作 ====================

    /**
     * 统计文档数量
     *
     * @param index 索引名称
     * @return 文档数量
     */
    public long count(String index) {
        return count(index, q -> q.matchAll(m -> m));
    }

    /**
     * 根据查询条件统计文档数量
     *
     * @param index        索引名称
     * @param queryBuilder 查询构建器（Function）
     * @return 文档数量
     */
    public long count(String index,
        Function<Query.Builder, co.elastic.clients.util.ObjectBuilder<Query>> queryBuilder) {
        try {
            Query query = Query.of(queryBuilder);

            CountRequest request = CountRequest.of(c -> c.index(index).query(query));

            CountResponse response = client.count(request);

            logger.debug("统计完成 - 索引: {}, 数量: {}", index, response.count());
            return response.count();
        } catch (IOException e) {
            logger.error("统计文档数量失败 - 索引: {}", index, e);
            return 0;
        }
    }

    // ==================== 文档存在性检查 ====================

    /**
     * 检查文档是否存在
     *
     * @param index 索引名称
     * @param id    文档ID
     * @return 是否存在
     */
    public boolean documentExists(String index, String id) {
        try {
            ExistsRequest request = ExistsRequest.of(e -> e.index(index).id(id));

            return client.exists(request).value();
        } catch (IOException e) {
            logger.error("检查文档是否存在失败 - 索引: {}, ID: {}", index, id, e);
            return false;
        }
    }

    // ==================== 刷新操作 ====================

    /**
     * 刷新索引（使数据立即可搜索）
     *
     * @param indices 索引名称列表
     * @return 是否刷新成功
     */
    public boolean refresh(String... indices) {
        try {
            client.indices().refresh(r -> r.index(List.of(indices)));
            logger.debug("索引刷新成功 - 索引: {}", String.join(", ", indices));
            return true;
        } catch (IOException e) {
            logger.error("刷新索引失败 - 索引: {}", String.join(", ", indices), e);
            return false;
        }
    }
}