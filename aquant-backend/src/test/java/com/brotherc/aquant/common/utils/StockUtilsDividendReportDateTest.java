package com.brotherc.aquant.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StockUtilsDividendReportDateTest {

    @Test
    @DisplayName("分红历史报告期只应包含半年报和年报")
    void shouldBuildTenYearsOfSupportedDividendReportDates() {
        List<String> reportDates = StockUtils.getDividendReportDates(10);

        assertThat(reportDates).hasSize(20).doesNotHaveDuplicates();
        assertThat(reportDates).allMatch(date -> date.endsWith("0630") || date.endsWith("1231"));
        assertThat(reportDates).isSortedAccordingTo((left, right) -> right.compareTo(left));
    }
}
