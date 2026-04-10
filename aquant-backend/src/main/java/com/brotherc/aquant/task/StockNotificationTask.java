package com.brotherc.aquant.task;

import com.brotherc.aquant.entity.StockNotification;
import com.brotherc.aquant.model.dto.akshare.StockIndividualInfoEm;
import com.brotherc.aquant.repository.StockNotificationRepository;
import com.brotherc.aquant.service.AKShareService;
import com.brotherc.aquant.service.StockNotificationService;
import com.brotherc.aquant.utils.StockHelper;
import com.brotherc.aquant.utils.StockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockNotificationTask {

    private final StockNotificationService notificationService;
    private final StockNotificationRepository notificationRepository;
    private final AKShareService akShareService;
    private final StockHelper stockHelper;

    /**
     * 股票预警轮询任务
     * 每5秒执行一次
     */
    @Scheduled(fixedRate = 5000)
    public void checkNotifications() {
        // 1. 检查是否为交易日
        if (!stockHelper.isTradeDay(LocalDate.now())) {
            return;
        }

        // 2. 检查是否在交易时间段内
        // A股交易时间：09:30-11:30, 13:00-15:00
        if (!StockUtils.isTradeTime(LocalTime.now())) {
            return;
        }

        // 3. 批量获取所有开启了预警的配置并按股票代码分组
        List<StockNotification> allActiveNotifications = notificationRepository.findAllByIsEnabled(1);
        if (allActiveNotifications.isEmpty()) {
            return;
        }

        Map<String, List<StockNotification>> notificationMap = allActiveNotifications.stream()
                .collect(Collectors.groupingBy(StockNotification::getStockCode));

        log.info("开始执行实时股票预警检测，活跃股票代码总数: {}", notificationMap.size());

        // 4. 遍历分组，获取实时行情并执行预警检查
        for (Map.Entry<String, List<StockNotification>> entry : notificationMap.entrySet()) {
            String stockCode = entry.getKey();
            List<StockNotification> configs = entry.getValue();
            try {
                // 调用东财 EM 实时接口获取最新行情
                StockIndividualInfoEm info = akShareService.stockIndividualInfoEm(stockCode, null);
                if (info != null && info.getLatest() != null) {
                    notificationService.checkAndNotify(info.getStockName(), info.getLatest(), configs);
                }
            } catch (Exception e) {
                log.error("获取实时行情并检查预警失败 [{}]", stockCode, e);
            }
        }
    }

}
