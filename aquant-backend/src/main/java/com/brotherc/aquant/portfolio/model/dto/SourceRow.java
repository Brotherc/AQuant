package com.brotherc.aquant.portfolio.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@AllArgsConstructor
public class SourceRow {

    @Getter
    private final int rowNumber;

    private final Map<String, String> values;

    public String get(String field) {
        return StringUtils.defaultString(values.get(field));
    }

    public String required(String field) {
        String value = get(field);
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        return value;
    }

}
