package com.brotherc.aquant.notification.service;

import com.brotherc.aquant.notification.entity.StockNotification;
import com.brotherc.aquant.fund.entity.StockFundNetValue;
import com.brotherc.aquant.stock.entity.StockQuoteHistory;
import com.brotherc.aquant.sys.entity.SysUser;
import com.brotherc.aquant.common.enums.NotificationAssetType;
import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.enums.TradeSignal;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.common.enums.NotificationType;
import com.brotherc.aquant.common.enums.PriceAlertCondition;
import com.brotherc.aquant.notification.model.vo.StockNotificationReqVO;
import com.brotherc.aquant.notification.model.vo.StockNotificationVO;
import com.brotherc.aquant.fund.repository.StockFundNetValueRepository;
import com.brotherc.aquant.notification.repository.StockNotificationRepository;
import com.brotherc.aquant.stock.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.sys.repository.SysUserRepository;
import com.brotherc.aquant.common.utils.StockUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockNotificationService {

    private static final String CONDITION = "condition";
    private static final long OBSERVED_PRICE_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000;
    private final ConcurrentMap<Long, ObservedPrice> lastObservedPriceMap = new ConcurrentHashMap<>();

    @Value("${aquant.stock.max-notification-stock-count:600}")
    private Integer maxNotificationStockCount;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    private final StockNotificationRepository notificationRepository;
    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;
    private final StockFundNetValueRepository stockFundNetValueRepository;
    private final SysUserRepository sysUserRepository;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    /**
     * 获取用户指定标的的通知配置
     */
    public List<StockNotificationVO> getByUserIdAndStockCode(Long userId, String stockCode, String assetType) {
        String normalizedAssetType = NotificationAssetType.fromType(assetType).getType();
        return notificationRepository.findAllByUserIdAndStockCodeAndAssetType(userId, stockCode, normalizedAssetType).stream()
                .map(this::convertToVO)
                .toList();
    }

    /**
     * 保存通知配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(StockNotificationReqVO reqVO, Long userId) {
        if (userId == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }

        String assetType = NotificationAssetType.fromType(reqVO.getAssetType()).getType();
        StockNotification notification;
        if (reqVO.getId() != null) {
            notification = notificationRepository.findByIdAndUserId(reqVO.getId(), userId)
                    .orElseThrow(ExceptionEnum.SYS_CHECK_ERROR::toException);
        } else {
            notification = new StockNotification();
            notification.setUserId(userId);
            notification.setStockCode(reqVO.getStockCode());
        }

        notification.setAssetType(assetType);
        notification.setType(reqVO.getType());
        notification.setThresholdValue(reqVO.getThresholdValue());
        if (NotificationType.PRICE.getType().equals(reqVO.getType())) {
            notification.setParams(normalizePriceAlertParams(reqVO.getParams()));
        } else {
            notification.setParams(reqVO.getParams());
        }
        notification.setIsEnabled(reqVO.getIsEnabled() != null ? reqVO.getIsEnabled() : 1);
        notification.setNotifyStrategy(reqVO.getNotifyStrategy() != null ? reqVO.getNotifyStrategy() : 1);

        checkDuplicate(notification, userId);
        checkStockCountLimit(notification);

        StockNotification saved = notificationRepository.save(notification);
        clearLastObservedPrice(saved.getId());
    }

    private void checkDuplicate(StockNotification notification, Long userId) {
        List<StockNotification> existing = notificationRepository.findAllByUserIdAndStockCodeAndAssetType(
                userId, notification.getStockCode(), notification.getAssetType());
        for (StockNotification item : existing) {
            // 排除自身（编辑情况）
            if (notification.getId() != null && notification.getId().equals(item.getId())) {
                continue;
            }

            boolean typeMatch = item.getType().equals(notification.getType());
            boolean valueMatch = (item.getThresholdValue() == null && notification.getThresholdValue() == null)
                    || (item.getThresholdValue() != null && item.getThresholdValue().compareTo(notification.getThresholdValue()) == 0);
            boolean paramsMatch = (item.getParams() == null && notification.getParams() == null)
                    || (item.getParams() != null && item.getParams().equals(notification.getParams()));

            if (typeMatch && valueMatch && paramsMatch) {
                throw ExceptionEnum.STOCK_NOTIFICATION_DUPLICATE.toException();
            }
        }
    }

    private void checkStockCountLimit(StockNotification notification) {
        String stockCode = notification.getStockCode();
        String assetType = notification.getAssetType();
        // 如果该标的目前没有任何活跃通知，则保存后将成为一个新的监控标的
        boolean isMonitored = notificationRepository.existsByStockCodeAndAssetType(stockCode, assetType);
        if (!isMonitored) {
            long currentCount = notificationRepository.countActiveStockCodes(assetType);
            if (currentCount >= maxNotificationStockCount) {
                throw ExceptionEnum.STOCK_NOTIFICATION_STOCK_COUNT_LIMIT.toException();
            }
        }
    }

    /**
     * 删除通知配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        StockNotification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(ExceptionEnum.SYS_CHECK_ERROR::toException);
        notificationRepository.delete(notification);
        clearLastObservedPrice(notification.getId());
    }

    /**
     * 检查并触发股票通知
     */
    public void checkStockAndNotify(String stockName, BigDecimal latestPrice, List<StockNotification> activeNotifications) {
        if (activeNotifications == null || activeNotifications.isEmpty()) {
            return;
        }

        pruneExpiredObservedPrices(System.currentTimeMillis());

        for (StockNotification config : activeNotifications) {
            try {
                if (config.getType().equals(NotificationType.PRICE.getType())) {
                    checkThresholdAlert(config, stockName, latestPrice, "当前价", "价格通知");
                } else if (config.getType().equals(NotificationType.DUAL_MA.getType())) {
                    checkStockDualMAAlert(config, stockName, latestPrice);
                }
            } catch (Exception e) {
                log.error("Failed to check notification for user {}: {}", config.getUserId(), e.getMessage());
            }
        }
    }

    /**
     * 检查并触发基金通知
     */
    public void checkFundAndNotify(String fundName, BigDecimal latestNetValue, List<StockNotification> activeNotifications) {
        if (activeNotifications == null || activeNotifications.isEmpty()) {
            return;
        }

        pruneExpiredObservedPrices(System.currentTimeMillis());

        for (StockNotification config : activeNotifications) {
            try {
                if (config.getType().equals(NotificationType.PRICE.getType())) {
                    checkThresholdAlert(config, fundName, latestNetValue, "最新净值", "净值通知");
                } else if (config.getType().equals(NotificationType.DUAL_MA.getType())) {
                    checkFundDualMAAlert(config, fundName, latestNetValue);
                }
            } catch (Exception e) {
                log.error("Failed to check fund notification for user {}: {}", config.getUserId(), e.getMessage());
            }
        }
    }

    private void checkThresholdAlert(
            StockNotification config, String targetName, BigDecimal latestValue, String valueLabel, String notificationTitle
    ) {
        if (config.getThresholdValue() == null) return;

        BigDecimal threshold = config.getThresholdValue();
        PriceAlertCondition condition = parsePriceAlertCondition(config.getParams(), false);
        if (condition == null) {
            log.warn("Invalid threshold alert params for notification {}: {}", config.getId(), config.getParams());
            return;
        }

        Long notificationId = config.getId();
        if (notificationId == null) {
            log.warn("Threshold alert notification without id, skip crossing detection for target {}", config.getStockCode());
            return;
        }

        long now = System.currentTimeMillis();
        BigDecimal previousPrice = getObservedPrice(notificationId, now);
        lastObservedPriceMap.put(notificationId, new ObservedPrice(latestValue, now + OBSERVED_PRICE_TTL_MILLIS));

        if (previousPrice == null) {
            // 初次观测校验：如果当前已满足条件且通过频率限制，则补发通知
            boolean initialMet;
            if (PriceAlertCondition.DOWN == condition) {
                initialMet = latestValue.compareTo(threshold) <= 0;
            } else {
                initialMet = latestValue.compareTo(threshold) >= 0;
            }

            if (initialMet && isCoolDownPassed(config)) {
                sendNotify(config, String.format("【%s】%s(%s) %s %s 已%s设定值 %s (初次观测捕获)",
                        notificationTitle, targetName, config.getStockCode(), valueLabel, latestValue,
                        condition.getDescription(), threshold));
                updateLastNotifyTime(config);
            }
            return;
        }

        boolean triggered;
        if (PriceAlertCondition.DOWN == condition) {
            triggered = previousPrice.compareTo(threshold) > 0 && latestValue.compareTo(threshold) <= 0;
        } else {
            triggered = previousPrice.compareTo(threshold) < 0 && latestValue.compareTo(threshold) >= 0;
        }

        if (triggered && isCoolDownPassed(config)) {
            sendNotify(config, String.format("【%s】%s(%s) %s设定值 %s，%s %s",
                    notificationTitle, targetName, config.getStockCode(), condition.getDescription(), threshold,
                    valueLabel, latestValue));
            updateLastNotifyTime(config);
        }
    }

    private void checkStockDualMAAlert(StockNotification config, String stockName, BigDecimal latestPrice) {
        try {
            JsonNode params = objectMapper.readTree(config.getParams());
            int maShort = params.path("maShort").asInt(5);
            int maLong = params.path("maLong").asInt(20);
            String condition = params.path(CONDITION).asText("UP");
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
                if ("UP".equalsIgnoreCase(condition)) {
                    signal = TradeSignal.BUY;
                }
            } else if (yesterdayShort.compareTo(yesterdayLong) >= 0 && todayShort.compareTo(todayLong) < 0) {
                if ("DOWN".equalsIgnoreCase(condition)) {
                    signal = TradeSignal.SELL;
                }
            }

            if (signal != TradeSignal.HOLD && isCoolDownPassed(config)) {
                sendNotify(config, String.format("【策略通知】%s(%s) 触发双均线(%d, %d) %s 信号，当前价 %s", 
                    stockName, config.getStockCode(), maShort, maLong, signal.name(), latestPrice));
                updateLastNotifyTime(config);
            }

        } catch (JsonProcessingException e) {
            log.error("Invalid params format for notification {}: {}", config.getId(), config.getParams());
        }
    }

    private void checkFundDualMAAlert(StockNotification config, String fundName, BigDecimal latestNetValue) {
        try {
            JsonNode params = objectMapper.readTree(config.getParams());
            int maShort = params.path("maShort").asInt(5);
            int maLong = params.path("maLong").asInt(20);
            String condition = params.path(CONDITION).asText("UP");

            int needDays = maLong + 1;
            List<StockFundNetValue> history = stockFundNetValueRepository.findLatestByFundCode(
                    config.getStockCode(), PageRequest.of(0, needDays));
            if (history.size() < needDays) return;

            Collections.reverse(history);

            BigDecimal previousShort = avgFund(history.subList(history.size() - maShort - 1, history.size() - 1));
            BigDecimal previousLong = avgFund(history.subList(history.size() - maLong - 1, history.size() - 1));
            BigDecimal latestShort = avgFund(history.subList(history.size() - maShort, history.size()));
            BigDecimal latestLong = avgFund(history.subList(history.size() - maLong, history.size()));

            TradeSignal signal = TradeSignal.HOLD;
            if (previousShort.compareTo(previousLong) <= 0 && latestShort.compareTo(latestLong) > 0) {
                if ("UP".equalsIgnoreCase(condition)) {
                    signal = TradeSignal.BUY;
                }
            } else if (previousShort.compareTo(previousLong) >= 0 && latestShort.compareTo(latestLong) < 0) {
                if ("DOWN".equalsIgnoreCase(condition)) {
                    signal = TradeSignal.SELL;
                }
            }

            if (signal != TradeSignal.HOLD && isCoolDownPassed(config)) {
                sendNotify(config, String.format("【策略通知】%s(%s) 触发双均线(%d, %d) %s 信号，最新净值 %s",
                        fundName, config.getStockCode(), maShort, maLong, signal.name(), latestNetValue));
                updateLastNotifyTime(config);
            }

        } catch (JsonProcessingException e) {
            log.error("Invalid fund dual MA params format for notification {}: {}", config.getId(), config.getParams());
        }
    }

    private boolean isCoolDownPassed(StockNotification config) {
        if (config.getLastNotifyAt() == null) return true;

        Integer strategy = config.getNotifyStrategy();
        if (strategy == null || strategy == 1) {
            // 每日一次：判断日期是否为今天
            return !config.getLastNotifyAt().toLocalDate().isEqual(LocalDate.now());
        } else if (strategy == 2) {
            // 持续重复：1 分钟冷却
            return config.getLastNotifyAt().plusMinutes(1).isBefore(LocalDateTime.now());
        }

        // 默认兜底：24 小时
        return config.getLastNotifyAt().plusHours(24).isBefore(LocalDateTime.now());
    }

    private void updateLastNotifyTime(StockNotification config) {
        config.setLastNotifyAt(LocalDateTime.now());
        notificationRepository.save(config);
    }

    private void sendNotify(StockNotification config, String content) {
        log.info(">>> [NOTIFY] UserID: {}, Content: {}", config.getUserId(), content);
        try {
            Long userId = config.getUserId();
            SysUser user = sysUserRepository.findById(userId).orElse(null);
            if (user != null && user.getEmail() != null && !user.getEmail().trim().isEmpty() && mailFrom != null && !mailFrom.isEmpty()) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(user.getEmail());
                String assetName = NotificationAssetType.fromType(config.getAssetType()).getDescription();
                message.setSubject("AQuant " + assetName + "预警通知 - " + config.getStockCode());
                message.setText(content);
                mailSender.send(message);
                log.info("Successfully sent email notification to {}", user.getEmail());
            } else {
                log.warn("Cannot send email: User {} email is missing or spring.mail.username is not configured.", userId);
            }
        } catch (Exception e) {
            log.error("Failed to send email notification for user {}: {}", config.getUserId(), e.getMessage());
        }
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

    private BigDecimal avgFund(List<StockFundNetValue> list) {
        return list.stream()
                .map(StockFundNetValue::getUnitNav)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(list.size()), 4, RoundingMode.HALF_UP);
    }

    private StockNotificationVO convertToVO(StockNotification entity) {
        StockNotificationVO vo = new StockNotificationVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private PriceAlertCondition parsePriceAlertCondition(String paramsText, boolean strict) {
        if (paramsText == null || paramsText.isBlank()) {
            return PriceAlertCondition.UP;
        }

        try {
            JsonNode params = objectMapper.readTree(paramsText);
            PriceAlertCondition condition = PriceAlertCondition.fromCode(params.path(CONDITION).asText(null));
            if (condition != null) {
                return condition;
            }
        } catch (JsonProcessingException e) {
            if (!strict) {
                return null;
            }
        }

        if (strict) {
            throw new BusinessException(ExceptionEnum.STOCK_NOTIFICATION_PRICE_ALERT_PARAMS_ILLEGAL);
        }
        return null;
    }

    private String normalizePriceAlertParams(String paramsText) {
        PriceAlertCondition condition = parsePriceAlertCondition(paramsText, true);
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(CONDITION, condition.name()));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ExceptionEnum.STOCK_NOTIFICATION_PRICE_ALERT_PARAMS_ILLEGAL);
        }
    }

    private void clearLastObservedPrice(Long notificationId) {
        if (notificationId != null) {
            lastObservedPriceMap.remove(notificationId);
        }
    }

    private BigDecimal getObservedPrice(Long notificationId, long now) {
        ObservedPrice observedPrice = lastObservedPriceMap.get(notificationId);
        if (observedPrice == null) {
            return null;
        }
        if (observedPrice.expiredAtMillis() <= now) {
            lastObservedPriceMap.remove(notificationId, observedPrice);
            return null;
        }
        return observedPrice.price();
    }

    private void pruneExpiredObservedPrices(long now) {
        for (Map.Entry<Long, ObservedPrice> entry : lastObservedPriceMap.entrySet()) {
            ObservedPrice observedPrice = entry.getValue();
            if (observedPrice != null && observedPrice.expiredAtMillis() <= now) {
                lastObservedPriceMap.remove(entry.getKey(), observedPrice);
            }
        }
    }

    private record ObservedPrice(BigDecimal price, long expiredAtMillis) {
    }

}
