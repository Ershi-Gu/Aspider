# ASpider 财经信息分析

## 项目介绍

基于多类型数据源 + 信息分析Agent，对财经信息进行实时分析并生成报告。
通过多信息面分析行业板块趋势，解读国家政策与经济动向。

### 系统目标

```
┌─────────────────────────────────────────────────────────────────┐
│                      用户价值主张                                │
├─────────────────────────────────────────────────────────────────┤
│  通过AI分析多维度数据，为用户提供：                              │
│  ✓ 政策解读 - 国家政策对行业板块的影响分析                       │
│  ✓ 板块趋势 - 行业/概念板块的资金流向与热度分析                  │
│  ✓ 关联分析 - 政策+资金+新闻的综合研判                           │
│  ✗ 不提供：具体买卖建议、个股推荐                                │
└─────────────────────────────────────────────────────────────────┘
```

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

## 数据层设计

### 数据源矩阵

| 数据类型 | 数据源 | 采集频率 | 用途 |
|---------|--------|---------|------|
| **财经新闻** | 东方财富 | 每3小时 | 政策解读、行业动态 |
| **板块资金流向** | 东方财富 | 盘中实时(每30分钟) + 收盘后 | 主力资金动向分析 |
| **板块行情数据** | 东方财富 | 每日收盘后 | 板块涨跌趋势 |

### 定时任务汇总

| 任务 | cron表达式 | 执行时间 | 说明 |
|-----|-----------|---------|------|
| 新闻数据采集 | `0 0 0/3 * * ?` | 每3小时 | 采集东财财经新闻 |
| 板块资金盘中采集 | `0 0/30 9-11,13-15 * * MON-FRI` | 交易时段每30分钟 | 实时监控资金流向 |
| 板块资金收盘采集 | `0 30 15 * * MON-FRI` | 每日15:30 | 当日完整数据 |
| 板块行情收盘采集 | `0 35 15 * * MON-FRI` | 每日15:35 | 采集当日板块行情 |
| 过期新闻清理 | `0 0 2 * * ?` | 每日凌晨2:00 | 清理30天前数据 |

### 数据模型

#### FinancialArticle（财经新闻）

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

#### SectorMoneyFlow（板块资金流向）

```
SectorMoneyFlow
├── uniqueId              # 唯一标识 (sectorCode + tradeDate 的MD5)
├── sectorCode            # 板块代码 (如 BK0477)
├── sectorName            # 板块名称 (如 半导体)
├── sectorType            # 板块类型 (INDUSTRY/CONCEPT)
├── tradeDate             # 交易日期
├── changePercent         # 涨跌幅(%)
├── mainNetInflow         # 主力净流入(元)
├── mainNetInflowRatio    # 主力净流入占比(%)
├── superLargeInflow      # 超大单净流入(元)
├── superLargeInflowRatio # 超大单净流入占比(%)
├── largeInflow           # 大单净流入(元)
├── largeInflowRatio      # 大单净流入占比(%)
├── mediumInflow          # 中单净流入(元)
├── mediumInflowRatio     # 中单净流入占比(%)
├── smallInflow           # 小单净流入(元)
├── smallInflowRatio      # 小单净流入占比(%)
├── leadStock             # 领涨股票代码
├── leadStockName         # 领涨股票名称
└── crawlTime             # 爬取时间
```

#### SectorQuote（板块行情）

```
SectorQuote
├── uniqueId           # 唯一标识
├── sectorCode         # 板块代码
├── sectorName         # 板块名称
├── sectorType         # 板块类型
├── tradeDate          # 交易日期
├── openPrice          # 开盘价
├── closePrice         # 收盘价
├── highPrice          # 最高价
├── lowPrice           # 最低价
├── changePercent      # 涨跌幅
├── turnoverRate       # 换手率
├── amount             # 成交额
└── companyCount       # 成分股数量
```

## Agent分析层设计

### Agent架构

```
┌───────────────────────────────────────────────────────────────┐
│                      AnalysisAgentOrchestrator                │
│                         (分析编排器)                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │
│  │ PolicyAgent │  │ SectorAgent │  │ TrendAgent  │           │
│  │  政策分析    │  │  板块分析    │  │  趋势研判   │           │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘           │
│         │                │                │                   │
│         └────────────────┼────────────────┘                   │
│                          ↓                                    │
│                 ┌─────────────────┐                           │
│                 │  ReportGenerator │                          │
│                 │   报告生成器      │                          │
│                 └─────────────────┘                           │
└───────────────────────────────────────────────────────────────┘
```

### Agent职责

| Agent | 输入数据 | 分析维度 | 输出 |
|-------|---------|---------|------|
| **PolicyAgent** | FinancialArticle | 政策类新闻识别→受益/受损板块映射 | 政策影响分析 |
| **SectorAgent** | SectorMoneyFlow + SectorQuote | 资金流向+涨跌幅+换手率 | 板块热度评估 |
| **TrendAgent** | 多日数据 | 连续N日趋势+资金持续性 | 趋势研判 |
| **ReportGenerator** | 各Agent输出 | 综合整理 | 结构化分析报告 |

## 模块结构

```
com.ershi.aspider
├── datasource                    # 数据源模块
│   ├── domain
│   │   ├── FinancialArticle.java              # 财经新闻
│   │   ├── FinancialArticleDSTypeEnum.java    # 新闻数据源类型
│   │   ├── SectorMoneyFlow.java               # 板块资金流向
│   │   ├── SectorQuote.java                   # 板块行情
│   │   └── SectorTypeEnum.java                # 板块类型枚举
│   ├── provider
│   │   ├── FinancialArticleDataSource.java    # 新闻数据源接口
│   │   ├── EastMoneyFinancialArticleDS.java   # 东财新闻实现
│   │   ├── SectorDataSource.java              # 板块数据源接口
│   │   └── EastMoneySectorDS.java             # 东财板块数据实现
│   └── service
│       ├── FinancialArticleDSFactory.java     # 新闻数据源工厂
│       └── SectorDSFactory.java               # 板块数据源工厂
│
├── processor                     # 数据处理模块
│   ├── cleaner                   # 数据清洗（去重、文本清理）
│   └── extractor                 # 摘要提取（LLM摘要生成）
│
├── embedding                     # 向量化模块
│   └── service                   # Embedding服务（通义千问、智谱等）
│
├── storage                       # 存储模块
│   └── elasticsearch
│       ├── FinancialArticleStorageService.java  # 新闻存储
│       ├── SectorMoneyFlowStorageService.java   # 资金流向存储
│       └── SectorQuoteStorageService.java       # 板块行情存储
│
├── orchestration                 # 编排模块
│   └── service
│       ├── FinancialArticleDataService.java     # 新闻数据编排
│       └── SectorDataService.java               # 板块数据编排
│
├── agent                         # Agent分析模块
│   ├── domain
│   │   ├── AnalysisRequest.java       # 分析请求
│   │   ├── AnalysisReport.java        # 分析报告
│   │   ├── PolicyImpact.java          # 政策影响
│   │   ├── SectorHeat.java            # 板块热度
│   │   └── TrendSignal.java           # 趋势信号
│   ├── core
│   │   ├── Agent.java                 # Agent接口
│   │   ├── PolicyAgent.java           # 政策分析Agent
│   │   ├── SectorAgent.java           # 板块分析Agent
│   │   └── TrendAgent.java            # 趋势研判Agent
│   ├── prompt
│   │   ├── PromptTemplate.java        # Prompt模板接口
│   │   ├── PolicyPromptTemplate.java  # 政策分析Prompt
│   │   ├── SectorPromptTemplate.java  # 板块分析Prompt
│   │   └── TrendPromptTemplate.java   # 趋势研判Prompt
│   ├── retriever
│   │   ├── DataRetriever.java         # 数据检索接口
│   │   ├── NewsRetriever.java         # 新闻语义检索
│   │   └── SectorDataRetriever.java   # 板块数据检索
│   └── service
│       ├── AnalysisAgentOrchestrator.java  # Agent编排
│       └── ReportGeneratorService.java     # 报告生成
│
├── api                           # 对外API
│   └── controller
│       ├── AnalysisController.java    # 分析接口
│       └── ReportController.java      # 报告查询接口
│
├── job                           # 定时任务模块
│   ├── FinancialArticleDataJob.java       # 新闻采集+过期清理
│   └── SectorMoneyFlowDataJob.java        # 板块资金实时采集
│
└── common                        # 通用模块
    ├── config                    # 配置类
    └── util                      # 工具类
```

## 执行流程

### 1. 数据采集流程（定时执行）

```
┌─────────────────────────────────────────────────────────────┐
│  定时任务层                                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  FinancialArticleDataJob       SectorMoneyFlowDataJob       │
│  (每3小时 + 凌晨清理)           (盘中每30分钟 + 收盘后)       │
│       │                                │                    │
│       ↓                                ↓                    │
│  ┌─────────────┐               ┌───────────────┐            │
│  │ 东财新闻API  │               │ 东财板块API   │            │
│  └──────┬──────┘               └───────┬───────┘            │
│         │                              │                    │
│         ↓                              ↓                    │
│  ┌─────────────┐               ┌───────────────┐            │
│  │ 清洗+去重   │                │ 资金流向数据  │            │
│  └──────┬──────┘               │ 板块行情数据  │            │
│         │                      └───────┬───────┘            │
│         ↓                              ↓                    │
│  ┌─────────────────────────────────────────────┐            │
│  │              Elasticsearch                   │            │
│  │  financial_article | sector_money_flow |     │            │
│  │                      sector_quote            │            │
│  └─────────────────────────────────────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

### 2. 分析流程（用户触发/定时触发）

```
┌─────────────────────────────────────────────────────────────┐
│  用户请求: "分析今日半导体板块情况"                          │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  AnalysisAgentOrchestrator (分析编排器)                      │
├─────────────────────────────────────────────────────────────┤
│  1. 解析请求 → 识别分析目标(板块:半导体, 时间:今日)          │
│  2. 数据准备阶段                                            │
│     ├─ NewsRetriever: 语义检索相关新闻                      │
│     │   → "半导体" 向量化 → KNN检索 → 相关新闻Top10          │
│     ├─ SectorDataRetriever: 查询板块数据                    │
│     │   → 半导体资金流向、涨跌幅、近N日趋势                  │
│     └─ 延迟向量化处理(如有未处理新闻)                       │
│  3. Agent分析阶段 (可并行)                                  │
│     ├─ PolicyAgent: 分析相关政策新闻影响                    │
│     ├─ SectorAgent: 分析资金流向+热度                       │
│     └─ TrendAgent: 分析趋势信号                             │
│  4. 报告生成                                                │
│     └─ ReportGenerator: 整合各Agent输出                     │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  输出: AnalysisReport                                        │
├─────────────────────────────────────────────────────────────┤
│  【半导体板块分析报告 - 2025-12-05】                         │
│                                                             │
│  📊 板块概况                                                │
│  - 涨跌幅: +2.35%  |  主力净流入: +8.2亿                    │
│  - 换手率: 4.2%    |  领涨股: 中芯国际 +5.8%                │
│                                                             │
│  📰 政策/消息面                                             │
│  - 利好: 国家大基金三期传闻、华为新品发布带动预期            │
│  - 关注: 美国芯片出口管制新规细则待落地                      │
│                                                             │
│  💰 资金面                                                  │
│  - 主力连续3日净流入，超大单占比提升至58%                   │
│  - 北向资金今日净买入半导体ETF 2.1亿                        │
│                                                             │
│  📈 趋势研判                                                │
│  - 短期: 资金持续流入，关注能否突破前高压力位                │
│  - 中期: 政策支持+国产替代逻辑，景气度向上                   │
│                                                             │
│  ⚠️ 风险提示: 本报告仅供参考，不构成投资建议                 │
└─────────────────────────────────────────────────────────────┘
```

## 技术栈

- Java 21 + Spring Boot 3.x
- Elasticsearch（数据存储 + 向量检索）
- 通义千问（摘要提取 + Embedding）
- Google Gemini / 智谱AI（LLM分析）
- 虚拟线程（高并发数据采集）