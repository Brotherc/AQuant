package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockDividend;
import com.brotherc.aquant.repository.StockDividendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockDividendDedupService {

    private final StockDividendRepository stockDividendRepository;

    @Transactional(rollbackFor = Exception.class)
    public void clearDuplicateLatestAnnouncementDateRows() {
        List<StockDividend> duplicateRows = stockDividendRepository.findDuplicateLatestAnnouncementDateRows();
        if (CollectionUtils.isEmpty(duplicateRows)) {
            log.info("未发现分红重复数据，无需清理");
            return;
        }

        Map<String, List<StockDividend>> groupedRows = duplicateRows.stream()
                .collect(Collectors.groupingBy(row -> row.getStockCode() + "|" + row.getLatestAnnouncementDate()));

        Predicate<StockDividend> hasCoreDividendFields = row ->
                row.getCashDividendRatio() != null
                        || row.getBonusShareTotalRatio() != null
                        || row.getBonusShareRatio() != null
                        || row.getTransferShareRatio() != null;

        List<Long> deleteIds = groupedRows.values().stream()
                .filter(rows -> rows.stream().anyMatch(hasCoreDividendFields))
                .flatMap(rows -> rows.stream()
                        .filter(hasCoreDividendFields.negate())
                        .map(StockDividend::getId))
                .toList();
        if (CollectionUtils.isEmpty(deleteIds)) {
            log.info("发现分红重复组合 {} 组，但未发现可安全删除的弱记录", groupedRows.size());
            return;
        }

        long deleted = stockDividendRepository.deleteByIdIn(deleteIds);
        log.info("分红重复数据清理完成，重复组数: {}, 删除行数: {}", groupedRows.size(), deleted);
    }

}
