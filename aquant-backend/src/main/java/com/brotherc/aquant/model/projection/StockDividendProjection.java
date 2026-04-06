package com.brotherc.aquant.model.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 股票分红数据查询投影，仅包含统计所需的字段
 */
public interface StockDividendProjection {
    
    String getStockCode();
    
    String getStockName();
    
    LocalDate getLatestAnnouncementDate();
    
    BigDecimal getCashDividendRatio();
    
    BigDecimal getBonusShareRatio();
    
    BigDecimal getTransferShareRatio();
}
