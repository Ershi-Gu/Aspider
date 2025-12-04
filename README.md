# ASpider 财经信息分析

## 项目介绍
基于多类型数据源 + 信息分析Agent，对财经信息进行实时分析并生成报告。
通过多信息面分析行业板块趋势，解读国家政策与经济动向。

## 系统架构

### 整体设计：延迟向量化架构
采用**抓取与向量化分离**的设计，降低AI成本，提升数据时效性。

```
┌─────────────────────────────────────────────────────────────┐
│  阶段一：定时抓取（零AI成本）                                │
├─────────────────────────────────────────────────────────────┤
│  数据源抓取 → 清洗去重 → 存储原始数据到ES                    │
│  存储字段: uniqueId, title, content, contentUrl, publishTime │
│  不执行: 摘要提取、向量化                                    │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  阶段二：用户查询时（按需处理）                              │
├─────────────────────────────────────────────────────────────┤
│  1. 查询ES：获取时间窗口内未处理的数据                       │
│  2. 批量处理：摘要提取(LLM) → 向量化(Embedding) → 更新ES     │
│  3. 语义检索：用户query向量化 → 相似度匹配                   │
│  4. Agent分析：将检索结果交给分析Agent生成报告               │
└─────────────────────────────────────────────────────────────┘
```

### 设计优势
| 特性 | 说明 |
|-----|------|
| 成本可控 | 仅对查询时间窗口内的数据消耗AI额度 |
| 语义检索 | 向量化支持"半导体"匹配"集成电路"等语义关联 |
| 数据复用 | 已向量化数据持久保存，后续查询直接复用 |
| 架构简洁 | 通过processed标记区分数据状态 |

## 数据模型

```
FinancialArticle
├── uniqueId        # 去重标识（title + url 的 MD5）
├── title           # 标题
├── titleVector     # 标题向量（延迟生成）
├── contentUrl      # 文章详情URL
├── content         # 正文内容
├── summary         # 摘要（延迟生成）
├── summaryVector   # 摘要向量（延迟生成）
├── publishTime     # 发布时间
├── crawlTime       # 爬取时间
├── processed       # 是否已完成向量化处理
└── processedTime   # 向量化处理时间
```

## 模块结构

```
com.ershi.aspider
├── datasource          # 数据源模块
│   ├── domain          # 数据模型定义
│   ├── provider        # 数据源实现（东方财富等）
│   └── service         # 数据源工厂
├── processor           # 数据处理模块
│   ├── cleaner         # 数据清洗（去重、文本清理）
│   └── extractor       # 摘要提取（LLM摘要生成）
├── embedding           # 向量化模块
│   └── service         # Embedding服务（通义千问、智谱等）
├── storage             # 存储模块
│   └── elasticsearch   # ES存储服务
├── orchestration       # 编排模块
│   └── service         # 流程编排服务
└── job                 # 定时任务模块
```

## 执行流程

### 1. 数据抓取流程（定时执行）
```
FinancialArticleJob.processSpecificDataSource()
    → FinancialArticleFactory.getDataSource()      # 获取数据源
    → FinancialArticleSource.getFinancialArticle()         # 抓取数据
    → FinancialArticleCleaner.clean()              # 清洗去重
    → FinancialArticleStorageService.batchSaveToEs() # 存储原始数据
```

### 2. 查询处理流程（用户触发）
```
用户查询请求
    → 查询ES获取时间窗口内未处理数据（processed=false）
    → ContentExtractor.extractBatch()      # 批量摘要提取
    → EmbeddingExecutor.embedTexts()       # 批量向量化
    → 更新ES（标记processed=true）
    → 执行语义检索
    → 返回结果给分析Agent
```

## 技术栈
- Java 17 + Spring Boot
- Elasticsearch（数据存储 + 向量检索）
- 通义千问（摘要提取 + Embedding）
- 智谱AI（备选Embedding服务）
   