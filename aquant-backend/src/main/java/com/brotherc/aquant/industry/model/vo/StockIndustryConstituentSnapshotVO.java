package com.brotherc.aquant.industry.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockIndustryConstituentSnapshotVO {

    private String industry;
    private LocalDateTime sourceUpdatedAt;
    private boolean stale;
    private boolean available;
    private String message;
    private List<StockIndustryConstituentVO> content;
}
