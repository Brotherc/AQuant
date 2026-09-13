package com.brotherc.aquant.integration.eastmoney.service;

import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyTickDetail;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 东方财富行情服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EastmoneyFinanceService {

    /** 09:25 开盘集合竞价撮合完成，之前的记录为竞价时段虚拟撮合量，非真实成交 */
    private static final String AUCTION_OPEN_TIME = "09:25:00";

    private final EastmoneyQuoteGateway quoteGateway;

    /**
     * 获取个股当日分笔成交明细 (东财实时行情接口，单请求最多返回最近 5000 笔；
     * 经网关按 push2delay 优先、push2 兜底执行)
     *
     * @param code 股票代码，带交易所前缀，如 sh600519
     * @return 分笔成交明细（时间升序）
     */
    public EastmoneyTickDetail fetchTickDetails(String code) {
        JsonNode dataNode = quoteGateway.executeQuote(builder -> builder
                        .addPathSegments("api/qt/stock/details/get")
                        .addQueryParameter("secid", toSecId(code))
                        .addQueryParameter("fields1", "f1,f2,f3,f4")
                        .addQueryParameter("fields2", "f51,f52,f53,f54,f55")
                        .addQueryParameter("pos", "-5000"))
                .path("data");
        JsonNode details = dataNode.path("details");
        if (!details.isArray() || details.isEmpty() || dataNode.path("code").isMissingNode()) {
            log.warn("东财分笔成交接口数据缺失: symbol={}, bodySize={}", code, details.size());
            throw ExceptionEnum.API_REQUEST_ERROR.toException();
        }

        EastmoneyTickDetail detail = new EastmoneyTickDetail();
        detail.setCode(dataNode.path("code").asText());
        String prePrice = dataNode.path("prePrice").asText();
        if (!prePrice.isBlank()) {
            detail.setPrePrice(new BigDecimal(prePrice));
        }

        for (JsonNode rowNode : details) {
            // 格式: "HH:mm:ss,价格,成交量(手),单数,方向码"
            String[] parts = rowNode.asText().trim().split(",");
            if (parts.length < 5) {
                continue;
            }
            if (parts[0].compareTo(AUCTION_OPEN_TIME) < 0) {
                continue;
            }
            EastmoneyTickDetail.Trade trade = new EastmoneyTickDetail.Trade();
            trade.setTime(parts[0]);
            trade.setPrice(new BigDecimal(parts[1]));
            trade.setVolume(new BigDecimal(parts[2]));
            trade.setOrderCount(new BigDecimal(parts[3]));
            trade.setDirection(Integer.parseInt(parts[4]));
            detail.getTrades().add(trade);
        }
        return detail;
    }

    /**
     * 股票代码转东财 secId：sh -> 1.600519，sz/bj -> 0.000001
     */
    private String toSecId(String code) {
        String plain = StockUtils.getPlainCode(code);
        String market = code.startsWith("sh") ? "1" : "0";
        return market + "." + plain;
    }
}
