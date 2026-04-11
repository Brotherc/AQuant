package com.brotherc.aquant.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {

    PRICE(1, "价格通知"),
    DUAL_MA(2, "双均线策略");

    private final Integer type;
    private final String description;

    public static NotificationType getByType(Integer type) {
        for (NotificationType nt : values()) {
            if (nt.getType().equals(type)) {
                return nt;
            }
        }
        return null;
    }

}
