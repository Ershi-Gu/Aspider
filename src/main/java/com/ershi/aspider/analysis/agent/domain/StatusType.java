package com.ershi.aspider.analysis.agent.domain;

/**
 * 分析状态类型
 *
 * @author Ershi-Gu
 */
public enum StatusType {

    /** 正常完成 */
    NORMAL,

    /** 降级（部分功能不可用，但仍返回结果） */
    DEGRADED,

    /** 失败 */
    FAILED
}
