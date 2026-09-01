package com.brotherc.aquant.industry.model.vo;

import com.brotherc.aquant.industry.model.IndustryDataSource;
import lombok.Data;

@Data
public class IndustrySourceSnapshotVO<T> {

    private IndustryDataSource requestedSource;
    private IndustryDataSource effectiveSource;
    private boolean fallback;
    private boolean stale;
    private boolean available;
    private String message;
    private T content;
}
