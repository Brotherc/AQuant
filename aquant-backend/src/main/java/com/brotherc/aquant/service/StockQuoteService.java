package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.model.dto.akshare.StockZhASpot;
import com.brotherc.aquant.model.vo.stockquote.StockQuotePageReqVO;
import com.brotherc.aquant.model.vo.stockquote.StockQuoteVO;
import com.brotherc.aquant.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.repository.StockQuoteRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockQuoteService {

    private final StockQuoteRepository stockQuoteRepository;
    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;

    @Transactional(rollbackFor = Exception.class)
    public List<StockQuote> save(List<StockZhASpot> stockZhASpotList, LocalDateTime now) {
        // 查询所有股票最新行情数据
        List<StockQuote> dbLlist = stockQuoteRepository.findAll();
        // 按股票代码分组
        Map<String, StockQuote> map = dbLlist.stream().collect(Collectors.toMap(StockQuote::getCode, o -> o));

        List<StockQuote> list = new ArrayList<>();

        // 遍历第三方股票数据
        for (StockZhASpot stockZhASpot : stockZhASpotList) {
            StockQuote sq = map.get(stockZhASpot.get代码());
            // 如果不存在则创建
            if (sq == null) {
                sq = new StockQuote();
            }
            sq.setCode(stockZhASpot.get代码());
            sq.setName(stockZhASpot.get名称());
            sq.setLatestPrice(stockZhASpot.get最新价());
            sq.setChangeAmount(stockZhASpot.get涨跌额());
            sq.setChangePercent(stockZhASpot.get涨跌幅());
            sq.setBuyPrice(stockZhASpot.get买入());
            sq.setSellPrice(stockZhASpot.get卖出());
            sq.setPrevClose(stockZhASpot.get昨收());
            sq.setOpenPrice(stockZhASpot.get今开());
            sq.setHighPrice(stockZhASpot.get最高());
            sq.setLowPrice(stockZhASpot.get最低());
            sq.setVolume(stockZhASpot.get成交量());
            sq.setTurnover(stockZhASpot.get成交额());
            sq.setQuoteTime(stockZhASpot.get时间戳());
            sq.setCreatedAt(now);

            setPriceRange(sq, stockZhASpot.get最新价(), stockZhASpot.get最新价(), stockZhASpot.get最新价());

            list.add(sq);
        }
        // 更新股票最新行情
        stockQuoteRepository.saveAll(list);
        return list;
    }

    public void setPriceRange(StockQuote sq, BigDecimal latestPrice, BigDecimal maxPrice, BigDecimal minPrice) {
        // 更新历史最高价
        if (sq.getHistoryHightPrice() != null && maxPrice.compareTo(sq.getHistoryHightPrice()) > 0) {
            sq.setHistoryHightPrice(maxPrice);
        }
        // 更新历史最低价
        if (sq.getHistoryLowPrice() != null && minPrice.compareTo(sq.getHistoryLowPrice()) < 0) {
            sq.setHistoryLowPrice(minPrice);
        }

        if (sq.getHistoryHightPrice() != null && sq.getHistoryLowPrice() != null) {
            // 最大最小差值
            BigDecimal diff = sq.getHistoryHightPrice().subtract(sq.getHistoryLowPrice());

            // 计算百分比：(latest - min) / diff * 100
            BigDecimal percent = BigDecimal.ZERO;
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                percent = latestPrice.subtract(sq.getHistoryLowPrice())
                        .divide(diff, 4, RoundingMode.HALF_UP) // 保留4位小数
                        .multiply(new BigDecimal("100"));
            }
            log.info(" pir: " + percent);
            sq.setPir(percent);
        }
    }

    public Page<StockQuoteVO> getPage(StockQuotePageReqVO reqVO, Pageable pageable) {
        Specification<StockQuote> specification = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // 股票代码（模糊）
            if (StringUtils.isNotBlank(reqVO.getCode())) {
                predicates.add(cb.like(root.get("code"), "%" + reqVO.getCode() + "%"));
            }

            // 股票名称（模糊）
            if (StringUtils.isNotBlank(reqVO.getName())) {
                predicates.add(cb.like(root.get("name"), "%" + reqVO.getName() + "%"));
            }

            // 最新价下限
            if (reqVO.getLatestPriceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("latestPrice"), reqVO.getLatestPriceMin()));
            }

            // 最新价上限
            if (reqVO.getLatestPriceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("latestPrice"), reqVO.getLatestPriceMax()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return stockQuoteRepository.findAll(specification, pageable).map(o -> {
            StockQuoteVO vo = new StockQuoteVO();
            BeanUtils.copyProperties(o, vo);
            return vo;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteQuoteAndHistoryByCodes(List<String> codes) {
        long historyDeleted = stockQuoteHistoryRepository.deleteByCodeIn(codes);
        long quoteDeleted = stockQuoteRepository.deleteByCodeIn(codes);
        log.info("已清理退市股票数据，股票数: {}, stock_quote 删除: {}, stock_quote_history 删除: {}", codes.size(), quoteDeleted, historyDeleted);
    }

}
