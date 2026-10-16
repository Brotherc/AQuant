package com.brotherc.aquant.stock.service;

import com.brotherc.aquant.integration.akshare.service.AKShareService;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 退市股票识别、异常归档及行情数据清理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockDelistedCleanupService {

    private final AKShareService aKShareService;
    private final StockAbnormalService stockAbnormalService;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockQuoteService stockQuoteService;

    /**
     * 先归档交易所名单和本地名称规则识别出的退市股票，再清理异常表代码对应的行情及历史。
     */
    public void clearDelistedStockData() {
        Map<String, String> szDelistedStocks = new LinkedHashMap<>();
        try {
            aKShareService.stockInfoSzDelist("终止上市公司").stream()
                    .filter(stock -> StringUtils.isNotBlank(stock.getStockCode()))
                    .forEach(stock -> szDelistedStocks.put("sz" + stock.getStockCode().trim(), stock.getStockName()));
        } catch (Exception e) {
            log.warn("获取深交所终止上市公司列表失败，退市清理回退到名称规则", e);
        }
        stockAbnormalService.saveDelistedStocks(szDelistedStocks, "stock_info_sz_delist", "深交所终止上市公司");

        Map<String, String> shDelistedStocks = new LinkedHashMap<>();
        try {
            aKShareService.stockInfoShDelist("全部").stream()
                    .filter(stock -> StringUtils.contains(stock.getCompanyName(), "退市")
                            && StringUtils.isNotBlank(stock.getCompanyCode()))
                    .forEach(stock -> shDelistedStocks.put("sh" + stock.getCompanyCode().trim(), stock.getCompanyName()));
        } catch (Exception e) {
            log.warn("获取上交所退市公司列表失败，退市清理回退到名称规则", e);
        }
        stockAbnormalService.saveDelistedStocks(shDelistedStocks, "stock_info_sh_delist", "上交所退市公司（公司简称包含退市）");

        Map<String, String> localDelistedStocks = stockQuoteRepository.findAllCodeNames().stream()
                .filter(stockQuote -> {
                    String code = stockQuote.getCode().toLowerCase(Locale.ROOT);
                    String name = stockQuote.getName();
                    return name.contains("退市")
                            || (code.startsWith("sz") && name.endsWith("退"))
                            || (code.startsWith("bj") && name.endsWith("退"))
                            || (code.startsWith("sh") && name.startsWith("退市"));
                })
                .collect(LinkedHashMap::new,
                        (map, stockQuote) -> map.put(stockQuote.getCode(), stockQuote.getName()),
                        Map::putAll);
        stockAbnormalService.saveDelistedStocks(localDelistedStocks, "stock_quote", "本地行情股票名称符合退市规则");

        List<String> abnormalCodes = stockAbnormalService.findAllCodes();
        if (abnormalCodes.isEmpty()) {
            log.info("未发现异常股票数据，无需清理");
            return;
        }
        stockQuoteService.deleteQuoteAndHistoryByCodes(abnormalCodes);
    }

}
