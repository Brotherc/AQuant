package com.brotherc.aquant.model.vo.stockfund;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "基金官方申购限制")
public class StockFundPurchaseLimitVO {

    private String source;

    private String sourceName;

    private String salesChannel;

    private String salesChannelName;

    private String businessType;

    private String status;

    private BigDecimal limitAmount;

    private String currency;

    private LocalDate effectiveDate;

    private LocalDate announcementDate;

    private String announcementTitle;

    private String announcementUrl;

}
