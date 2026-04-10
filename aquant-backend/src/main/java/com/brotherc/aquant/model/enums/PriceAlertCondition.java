package com.brotherc.aquant.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PriceAlertCondition {

    UP("向上突破"),
    DOWN("向下突破");

    private final String description;

    public static PriceAlertCondition fromCode(String code) {
        if (code == null || code.isBlank()) {
            return UP;
        }

        return Arrays.stream(values())
                .filter(condition -> condition.name().equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElse(null);
    }

}
