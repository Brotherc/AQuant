package com.brotherc.aquant.dividend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分红概览看板数据")
public class DividendOverviewVO {

    @Schema(description = "高分红机会数（最近完整年度股息率 >= 3%）")
    private Integer highDividendOpportunityCount;

    @Schema(description = "连续分红公司数（连续分红 >= 3年）")
    private Integer consecutiveDividendCount;

    @Schema(description = "我的自选分红数")
    private Integer watchlistDividendCount;

    @Schema(description = "近期重点观察数（近30天有公告且股息率 >= 3.5%）")
    private Integer todayFocusCount;

}
