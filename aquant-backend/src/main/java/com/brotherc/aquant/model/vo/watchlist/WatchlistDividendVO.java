package com.brotherc.aquant.model.vo.watchlist;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "自选股票近期主要分红信息")
public class WatchlistDividendVO {

    @Schema(description = "预案公告日期")
    private String proposalAnnouncementDate;

    @Schema(description = "方案进度")
    private String planStatus;

    @Schema(description = "现金分红比例")
    private BigDecimal cashDividendRatio;

    @Schema(description = "送转股份-送股比例")
    private BigDecimal bonusShareRatio;

    @Schema(description = "送转股份-转股比例")
    private BigDecimal transferShareRatio;

}
