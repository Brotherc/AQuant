package com.brotherc.aquant.task;

import com.brotherc.aquant.entity.notification.StockNotification;
import com.brotherc.aquant.entity.fund.StockFundInfo;
import com.brotherc.aquant.entity.fund.StockFundNetValue;
import com.brotherc.aquant.enums.NotificationAssetType;
import com.brotherc.aquant.model.dto.tencent.TencentStockQuote;
import com.brotherc.aquant.repository.fund.StockFundInfoRepository;
import com.brotherc.aquant.repository.fund.StockFundNetValueRepository;
import com.brotherc.aquant.repository.notification.StockNotificationRepository;
import com.brotherc.aquant.service.notification.StockNotificationService;
import com.brotherc.aquant.service.stock.TencentFinanceService;
import com.brotherc.aquant.utils.StockHelper;
import com.brotherc.aquant.utils.StockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockNotificationTask {

    private final StockNotificationService notificationService;
    private final StockNotificationRepository notificationRepository;
    private final StockHelper stockHelper;
    private final TencentFinanceService tencentFinanceService;
    private final StockFundInfoRepository stockFundInfoRepository;
    private final StockFundNetValueRepository stockFundNetValueRepository;

    /**
     * 股票通知轮询任务
     * 每5秒执行一次
     */
    @Scheduled(fixedRate = 5000)
    public void checkNotifications() {
        checkStockNotifications();
        checkFundNotifications();
    }

    private void checkStockNotifications() {
        // 1. 检查是否为交易日
        if (!stockHelper.isTradeDay(LocalDate.now())) {
            return;
        }

        // 2. 检查是否在交易时间段内
        // A股交易时间：09:30-11:30, 13:00-15:00
        if (!StockUtils.isTradeTime(LocalTime.now())) {
            return;
        }

        // 3. 批量获取所有开启了通知的配置并按股票代码分组
        List<StockNotification> allActiveNotifications = notificationRepository.findAllByIsEnabledAndAssetType(
                1, NotificationAssetType.STOCK.getType());
        if (allActiveNotifications.isEmpty()) {
            return;
        }

        // 提取所有唯一的股票代码并格式化为 sh/sz 格式
        List<String> uniqueStockCodes = allActiveNotifications.stream()
                .map(StockNotification::getStockCode)
                .distinct()
                .toList();

        log.info("开始执行实时股票通知检测，活跃股票代码总数: {}", uniqueStockCodes.size());

        // 4. 通过服务批量获取行情数据
        Map<String, TencentStockQuote> quoteDataMap = tencentFinanceService.fetchBatchQuotes(uniqueStockCodes);

        // 5. 按股票分组通知逻辑并检测
        Map<String, List<StockNotification>> notificationMap = allActiveNotifications.stream()
                .collect(Collectors.groupingBy(StockNotification::getStockCode));

        for (Map.Entry<String, List<StockNotification>> entry : notificationMap.entrySet()) {
            String stockCode = entry.getKey();
            List<StockNotification> configs = entry.getValue();

            TencentStockQuote data = quoteDataMap.get(stockCode);
            if (data != null && data.getPrice() != null) {
                try {
                    notificationService.checkStockAndNotify(data.getName(), data.getPrice(), configs);
                } catch (Exception e) {
                    log.error("检测通知失败 [{}]", stockCode, e);
                }
            }
        }
    }

    private void checkFundNotifications() {
        List<StockNotification> allActiveNotifications = notificationRepository.findAllByIsEnabledAndAssetType(
                1, NotificationAssetType.FUND.getType());
        if (allActiveNotifications.isEmpty()) {
            return;
        }

        List<String> fundCodes = allActiveNotifications.stream()
                .map(StockNotification::getStockCode)
                .distinct()
                .toList();

        log.info("开始执行基金通知检测，活跃基金代码总数: {}", fundCodes.size());

        List<StockFundInfo> fundInfos = stockFundInfoRepository.findByFundCodeIn(fundCodes);
        Map<String, String> fundNameMap = new HashMap<>();
        for (StockFundInfo fundInfo : fundInfos) {
            if (fundInfo != null && fundInfo.getFundCode() != null && fundInfo.getFundName() != null) {
                fundNameMap.putIfAbsent(fundInfo.getFundCode(), fundInfo.getFundName());
            }
        }

        List<StockFundNetValue> netValues = stockFundNetValueRepository.findByFundCodeInOrderByNavDateDesc(fundCodes);
        Map<String, StockFundNetValue> latestNetValueMap = new HashMap<>();
        for (StockFundNetValue netValue : netValues) {
            if (netValue != null && netValue.getFundCode() != null && netValue.getUnitNav() != null) {
                latestNetValueMap.putIfAbsent(netValue.getFundCode(), netValue);
            }
        }

        Map<String, List<StockNotification>> notificationMap = allActiveNotifications.stream()
                .collect(Collectors.groupingBy(StockNotification::getStockCode));

        for (Map.Entry<String, List<StockNotification>> entry : notificationMap.entrySet()) {
            String fundCode = entry.getKey();
            StockFundNetValue latestNetValue = latestNetValueMap.get(fundCode);
            if (latestNetValue == null) {
                continue;
            }
            try {
                notificationService.checkFundAndNotify(
                        fundNameMap.getOrDefault(fundCode, fundCode),
                        latestNetValue.getUnitNav(),
                        entry.getValue());
            } catch (Exception e) {
                log.error("检测基金通知失败 [{}]", fundCode, e);
            }
        }
    }

}
