package com.ershi.aspider.processor.cleaner;

import lombok.Data;

/**
 * ES文档映射类（仅用于查询uniqueId，用于去重）
 *
 *
 * @author Ershi-Gu.
 * @since 2025-11-13
 */
@Data
public class FinancialArticleClearEsDTO {
    /** 去重标识（title + url 的 MD5） */
    private String uniqueId;
}
