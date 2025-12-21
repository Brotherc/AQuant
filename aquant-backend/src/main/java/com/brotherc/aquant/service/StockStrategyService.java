package com.brotherc.aquant.service;

import com.brotherc.aquant.model.vo.strategy.DualMAReqVO;
import com.brotherc.aquant.model.vo.strategy.StockTradeSignalVO;
import com.brotherc.aquant.strategy.DualMovingAverageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockStrategyService {

    private final DualMovingAverageStrategy dualMovingAverageStrategy;

    public Page<StockTradeSignalVO> dualMA(DualMAReqVO reqVO, Pageable pageable) {
        List<StockTradeSignalVO> list = dualMovingAverageStrategy.calculate(reqVO.getMaShort(), reqVO.getMaLong());

        Stream<StockTradeSignalVO> stream = list.stream();

        if (StringUtils.isNotBlank(reqVO.getSignal())) {
            stream = stream.filter(vo -> reqVO.getSignal().equalsIgnoreCase(vo.getSignal())
            );
        }

        List<StockTradeSignalVO> filtered = stream.collect(Collectors.toList());

        filtered.sort(Comparator.comparing(StockTradeSignalVO::getCode));

        // 分页
        int total = filtered.size();
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int fromIndex = currentPage * pageSize;

        if (fromIndex >= total) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }

        int toIndex = Math.min(fromIndex + pageSize, total);

        List<StockTradeSignalVO> pageContent = filtered.subList(fromIndex, toIndex);

        return new PageImpl<>(pageContent, pageable, total);
    }

}
