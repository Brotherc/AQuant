package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockNotification;
import com.brotherc.aquant.entity.StockQuoteHistory;
import com.brotherc.aquant.enums.TradeSignal;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.enums.NotificationType;
import com.brotherc.aquant.model.vo.notification.StockNotificationReqVO;
import com.brotherc.aquant.model.vo.notification.StockNotificationVO;
import com.brotherc.aquant.repository.StockNotificationRepository;
import com.brotherc.aquant.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.utils.StockUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockNotificationService {

    private final StockNotificationRepository notificationRepository;
    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * 获取用户指定股票的预警配置
     */
    public List<StockNotificationVO> getByUserIdAndStockCode(Long userId, String stockCode) {
        return notificationRepository.findAllByUserIdAndStockCode(userId, stockCode).stream()
                .map(this::convertToVO)
                .toList();
    }

    /**
     * 保存预警配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(StockNotificationReqVO reqVO, Long userId) {
        if (userId == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }

        StockNotification notification;
        if (reqVO.getId() != null) {
            notification = notificationRepository.findByIdAndUserId(reqVO.getId(), userId)
                    .orElseThrow(() -> ExceptionEnum.SYS_CHECK_ERROR.toException());
        } else {
            notification = new StockNotification();
            notification.setUserId(userId);
            notification.setStockCode(reqVO.getStockCode());
        }

        notification.setType(reqVO.getType());
        notification.setThresholdValue(reqVO.getThresholdValue());
        notification.setParams(reqVO.getParams());
        notification.setIsEnabled(reqVO.getIsEnabled() != null ? reqVO.getIsEnabled() : 1);

        notificationRepository.save(notification);
    }

    /**
     * 删除预警配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        StockNotification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ExceptionEnum.SYS_CHECK_ERROR.toException());
        notificationRepository.delete(notification);
    }

    /**
     * 检查并触发预警 (核心逻辑)
     */
    public void checkAndNotify(String stockName, BigDecimal latestPrice, List<StockNotification> activeNotifications) {
        if (activeNotifications == null || activeNotifications.isEmpty()) {
            return;
        }

        for (StockNotification config : activeNotifications) {
            try {
                if (config.getType().equals(NotificationType.PRICE.getType())) {
                    checkPriceAlert(config, stockName, latestPrice);
                } else if (config.getType().equals(NotificationType.DUAL_MA.getType())) {
                    checkDualMAAlert(config, stockName, latestPrice);
                }
            } catch (Exception e) {
                log.error("Failed to check notification for user {}: {}", config.getUserId(), e.getMessage());
            }
        }
    }

    private void checkPriceAlert(StockNotification config, String stockName, BigDecimal latestPrice) {
        if (config.getThresholdValue() == null) return;

        // 简单的价格到达判断
        // 注意：为了避免重复，可以判断当前价格是否处于预警点附近，且今天还没发过
        boolean triggered = latestPrice.compareTo(config.getThresholdValue()) >= 0; 
        
        // 此处逻辑可根据需求细化，如：向上突破、向下突破等
        // 这里暂定为：只要价格高于设定的阈值就提醒，但限制频率（每24小时一次）
        if (triggered && isCoolDownPassed(config)) {
            sendNotify(config, String.format("【价格预警】%s(%s) 当前价 %s 已达到设定值 %s", 
                stockName, config.getStockCode(), latestPrice, config.getThresholdValue()));
            updateLastNotifyTime(config);
        }
    }

    private void checkDualMAAlert(StockNotification config, String stockName, BigDecimal latestPrice) {
        try {
            JsonNode params = objectMapper.readTree(config.getParams());
            int maShort = params.path("maShort").asInt(5);
            int maLong = params.path("maLong").asInt(20);
            String historyCode = StockUtils.wrapExchangePrefix(config.getStockCode());

            int needDays = maLong + 1;
            List<StockQuoteHistory> history = stockQuoteHistoryRepository.findLatestByCode(historyCode, needDays);
            
            if (history.size() < needDays) return;

            Collections.reverse(history);

            // 昨天
            BigDecimal yesterdayShort = avg(history.subList(history.size() - maShort - 1, history.size() - 1));
            BigDecimal yesterdayLong = avg(history.subList(history.size() - maLong - 1, history.size() - 1));

            // 今天 (基于当前最新价)
            // 构造模拟的“今天数据”集进行计算
            BigDecimal todayShort = avgWithLatest(history.subList(history.size() - maShort, history.size() - 1), latestPrice, maShort);
            BigDecimal todayLong = avgWithLatest(history.subList(history.size() - maLong, history.size() - 1), latestPrice, maLong);

            TradeSignal signal = TradeSignal.HOLD;
            if (yesterdayShort.compareTo(yesterdayLong) <= 0 && todayShort.compareTo(todayLong) > 0) {
                signal = TradeSignal.BUY;
            } else if (yesterdayShort.compareTo(yesterdayLong) >= 0 && todayShort.compareTo(todayLong) < 0) {
                signal = TradeSignal.SELL;
            }

            if (signal != TradeSignal.HOLD && isCoolDownPassed(config)) {
                sendNotify(config, String.format("【策略预警】%s(%s) 触发双均线(%d, %d) %s 信号，当前价 %s", 
                    stockName, config.getStockCode(), maShort, maLong, signal.name(), latestPrice));
                updateLastNotifyTime(config);
            }

        } catch (JsonProcessingException e) {
            log.error("Invalid params format for notification {}: {}", config.getId(), config.getParams());
        }
    }

    private boolean isCoolDownPassed(StockNotification config) {
        if (config.getLastNotifyAt() == null) return true;
        // 简单频率限制：同一条预警24小时内只发一次
        return config.getLastNotifyAt().plusHours(24).isBefore(LocalDateTime.now());
    }

    private void updateLastNotifyTime(StockNotification config) {
        config.setLastNotifyAt(LocalDateTime.now());
        notificationRepository.save(config);
    }

    private void sendNotify(StockNotification config, String content) {
        // TODO: 真正的通知推送逻辑 (Websocket/Email/DingTalk)
        // 目前仅打印日志或记录到通知表
        log.info(">>> [NOTIFY] UserID: {}, Content: {}", config.getUserId(), content);
    }

    private BigDecimal avg(List<StockQuoteHistory> list) {
        return list.stream()
                .map(StockQuoteHistory::getClosePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(list.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal avgWithLatest(List<StockQuoteHistory> history, BigDecimal latestPrice, int ma) {
        BigDecimal sum = history.stream()
                .map(StockQuoteHistory::getClosePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.add(latestPrice).divide(BigDecimal.valueOf(ma), 4, RoundingMode.HALF_UP);
    }

    private StockNotificationVO convertToVO(StockNotification entity) {
        StockNotificationVO vo = new StockNotificationVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

}
