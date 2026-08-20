package com.brotherc.aquant.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationAssetType {

    STOCK("STOCK", "股票"),
    FUND("FUND", "基金");

    private final String type;
    private final String description;

    public static NotificationAssetType fromType(String type) {
        if (type == null || type.isBlank()) {
            return STOCK;
        }
        for (NotificationAssetType assetType : values()) {
            if (assetType.getType().equalsIgnoreCase(type)) {
                return assetType;
            }
        }
        return STOCK;
    }

}
