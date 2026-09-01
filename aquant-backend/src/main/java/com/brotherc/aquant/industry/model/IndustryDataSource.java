package com.brotherc.aquant.industry.model;

public enum IndustryDataSource {
    THS,
    EM;

    public IndustryDataSource fallback() {
        return this == THS ? EM : THS;
    }
}
