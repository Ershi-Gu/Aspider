package com.ershi.aspider;

import com.ershi.aspider.processor.cleaner.FinancialArticleCleaner;
import com.ershi.aspider.processor.extractor.service.LLMSummaryExecutor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * LLM摘要服务测试
 */
@SpringBootTest(classes = ASpiderApplication.class)
public class LLMSummaryTest {

    @Resource
    private FinancialArticleCleaner financialArticleCleaner;

    @Resource
    private LLMSummaryExecutor llmSummaryExecutor;

    @Test
    public void testGenerateSummary() {
        String content = """
            电力板块再度爆发！最近两年，电力板块兼顾着红利和算力双重属性，表现相当强势。今天，该板块再度爆发。早盘9点30分，惠天热电第一笔交易将股价牢牢封死在涨停板。中闽能源、闽东电力双双一度涨停，恒盛能源、京能电力、深南电A等跟涨。电力ETF早盘一度逆市上涨超过0.7%。那么，电力板块究竟因何大涨？有机构认为，近期伴随2026年度电力交易零售侧签约工作的陆续展开，各地都在出台售电公司超额收益分成政策。从136号文细则发布，以及火电报表端已经体现出来的容量电价、辅助服务提供稳定盈利等视角来看，电力板块公用事业化主线延续，也将促进估值中枢抬升。开盘即涨停今天，整个市场表现并不太强，开盘半小时内，全市场除新股之外，没有一只涨幅超过20%的股票，而惠天热电可能是弱市当中最强的一只股票。该股连续竞价后，第一笔单就封死涨停板。整个电力板块处于强于大盘的格局当中。除惠天热电之外，闽东电力、中闽能源、恒盛能源、京能热电、京能电力、建投能源、华能国际、内蒙华电等纷纷走强。10月规上工业发电量同比增长7.9%，火电在近期市场表现优于其他电源。京唐港动力煤价降至823元/吨，周环比下降1.0%。沿海及内陆电厂日耗环比略有提升，但同比仍呈下降趋势。长江流域来水同比提升59.7%，大渡河、雅砻江来水同比分别下降11.1%和34.9%。多地发布2026年电力交易方案，中长期交易占比维持高位，煤电交易价格浮动区间保持20%不变。另外，电力与储能的结合范式，可能会驱动电力估值提升。12月3日，中国燃气发布公告称，与惠州亿纬锂能股份有限公司签订的战略合作协议。集团在工商业用户侧储能业务方面表现出强劲增长，截至2025年9月30日，已投运规模达到617.7兆瓦时，累计签约装机容量已达1.2吉瓦时，主要项目分布在江苏、浙江及广东等地区。此外，集团将利用人工智能技术提升电力交易的精准度，并通过虚拟电厂等创新模式整合分散式能源资源，拓宽电力新能源业务的盈利渠道。与亿纬锂能的战略合作将聚焦于技术研发、项目开发与市场拓展以及绿色能源生态构建，双方计划在工商业储能、移动储能等领域联合开发项目，并在未来三年内挖掘市场机会，推动新能源产业链高质量发展。136号文发力？136号文是国家发改委、国家能源局于2025年初联合发布的《关于深化新能源上网电价市场化改革促进新能源高质量发展的通知》（发改价格〔2025〕136号），标志着新能源电站收益模式从固定电价向市场化交易转型。近期，各省136号承接细则基本出台完毕，整体来看，136号文要求各省根据自身电力市场运行情况制定政策，各省承接政策呈现区域分化趋势。据相关机构统计，近期伴随2026年度电力交易零售侧签约工作的陆续展开，各地都在出台售电公司超额收益分成政策。河南要求用户也需承担超额亏损，但上限仅10%。广东2026年起，对售电公司月度平均批零差价高于0.01元/千瓦时的超额部分，按1:9比例分享给用户。同时将公示批零差价最大30家及固定价格均价最高30家售电公司名单。目前，河南、陕西、安徽、江西、四川等地区发布了限价政策，对比河南提出的3厘/度，广东限价1分虽不是最低，但是1:9的分成比例明显高于其他地区的“2:8”“5:5”比例，更进一步体现政策向用户端倾斜。有研究机构认为，政策引导售电侧商业模式转变，有望形成稳定电价预期。利润分享机制意味着售电公司套利的风险收益比将被极大压缩，有望引导售电公司与发用双方均形成合理价格，进而获得低风险且合理收益的电量，单向押注发售价差意味着极大的电量期限错配风险难以通过价差弥补，恶性竞价有望逐步退出市场。从四季度开始，电力行业受益于分子端在低基数下冬季电力需求有望同比高增，以及明年长协电价预期改善，行业景气修复信号有望从价格端与业绩端逐步验证。从136号文细则发布，以及火电报表端已经体现出来的容量电价、辅助服务提供稳定盈利等视角来看，电力板块公用事业化主线延续，也将促进估值中枢抬升。中金公司表示，电力交易规则明确中长期交易为压舱石，新能源消纳政策持续优化。火电盈利预期因煤价下行而改善，核电在部分省份综合电价有望提升。绿电估值处于底部，政策支持增强，区域龙头具备稳健量价表现。煤价下行改善火电盈利空间；政策推动新能源大规模开发与高质量消纳；中长期电力交易机制稳定电价预期；区域龙头具备更强的消纳能力和成本控制能力。
            """;

        System.out.println("原文长度：" + content.length());
        System.out.println("---原文---");
        System.out.println(content);

        String summary = llmSummaryExecutor.generateSummary(content);

        System.out.println("---摘要---");
        System.out.println(summary);
        System.out.println("摘要长度：" + summary.length());
    }

    @Test
    public void testGenerateSummaryWithTargetLength() {
        String content = "近日，国务院办公厅印发《关于促进政府投资基金高质量发展的指导意见》，" +
            "这是我国首个政府投资基金的全国性指导意见。《意见》从优化政府投资基金定位、" +
            "完善政府投资基金治理机制、强化政府投资基金监督管理等方面提出具体措施，" +
            "旨在推动政府投资基金更好发挥引导作用，促进政府投资基金高质量发展。";

        System.out.println("原文长度：" + content.length());

        // 测试不同目标长度
        String summary50 = llmSummaryExecutor.generateSummary(content, 50);
        System.out.println("目标50字摘要：" + summary50);
        System.out.println("实际长度：" + summary50.length());

        String summary100 = llmSummaryExecutor.generateSummary(content, 100);
        System.out.println("目标100字摘要：" + summary100);
        System.out.println("实际长度：" + summary100.length());
    }
}