package com.brotherc.aquant.portfolio.model.vo;

import com.brotherc.aquant.portfolio.entity.UserPortfolio;
import com.brotherc.aquant.portfolio.entity.UserPortfolioCash;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Data
@Schema(description = "持仓会诊快照的数据质量信息")
public class PortfolioDiagnosisQualityVO {

    @Schema(description = "组合内最新行情日期")
    private LocalDate latestQuoteDate;

    @Schema(description = "缺少最新价格的资产代码")
    private List<String> missingPriceAssetCodes;

    @Schema(description = "行情日期早于组合最新行情日期的资产代码")
    private List<String> stalePriceAssetCodes;

    @Schema(description = "组合中无法按基准币种汇总的币种")
    private List<String> unsupportedCurrencies;

    @Schema(description = "数据质量提示")
    private List<String> warnings;

    /**
     * 根据组合、持仓和现金数据生成会诊快照的数据质量信息。
     */
    public static PortfolioDiagnosisQualityVO from(
            UserPortfolio portfolio,
            List<PortfolioPositionVO> positions,
            List<UserPortfolioCash> cashList
    ) {
        LocalDate latestQuoteDate = positions.stream().map(PortfolioPositionVO::getQuoteDate)
                .filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
        List<String> missingPriceAssetCodes = positions.stream()
                .filter(position -> position.getLatestPrice() == null)
                .map(PortfolioPositionVO::getAssetCode).filter(Objects::nonNull).distinct().sorted().toList();
        List<String> stalePriceAssetCodes = latestQuoteDate == null ? List.of() : positions.stream()
                .filter(position -> position.getQuoteDate() != null && position.getQuoteDate().isBefore(latestQuoteDate))
                .map(PortfolioPositionVO::getAssetCode).filter(Objects::nonNull).distinct().sorted().toList();
        List<String> unsupportedCurrencies = Stream.concat(
                        positions.stream().map(PortfolioPositionVO::getCurrency),
                        cashList.stream().map(UserPortfolioCash::getCurrency))
                .filter(Objects::nonNull)
                .filter(currency -> !currency.equals(portfolio.getBaseCurrency()))
                .distinct().sorted().toList();
        List<String> warnings = new ArrayList<>();
        if (positions.isEmpty()) {
            warnings.add("当前组合没有持仓数据");
        }
        if (!missingPriceAssetCodes.isEmpty()) {
            warnings.add("部分持仓缺少最新价格，相关市值和盈亏可能不完整");
        }
        if (!stalePriceAssetCodes.isEmpty()) {
            warnings.add("部分持仓行情日期早于组合内最新行情日期");
        }
        if (!unsupportedCurrencies.isEmpty()) {
            warnings.add("组合包含非基准币种资产，当前未进行汇率折算");
        }

        PortfolioDiagnosisQualityVO result = new PortfolioDiagnosisQualityVO();
        result.setLatestQuoteDate(latestQuoteDate);
        result.setMissingPriceAssetCodes(missingPriceAssetCodes);
        result.setStalePriceAssetCodes(stalePriceAssetCodes);
        result.setUnsupportedCurrencies(unsupportedCurrencies);
        result.setWarnings(warnings);
        return result;
    }

}
