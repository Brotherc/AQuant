package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockIndustryBoard;
import com.brotherc.aquant.model.dto.akshare.StockBoardIndustrySummaryThs;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardPageReqVO;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardVO;
import com.brotherc.aquant.repository.StockIndustryBoardRepository;
import com.brotherc.aquant.utils.StockHelper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockIndustryBoardService {

    private final StockIndustryBoardRepository stockIndustryBoardRepository;
    private final StockHelper stockHelper;

    @Transactional(rollbackFor = Exception.class)
    public void save(List<StockBoardIndustrySummaryThs> stockBoardList, LocalDateTime now) {
        List<StockIndustryBoard> dbLlist = stockIndustryBoardRepository.findAll();
        Map<String, StockIndustryBoard> map = dbLlist.stream().collect(Collectors.toMap(StockIndustryBoard::getSectorName, o -> o));

        List<StockIndustryBoard> list = new ArrayList<>();

        for (StockBoardIndustrySummaryThs stockBoard : stockBoardList) {
            StockIndustryBoard sq = map.get(stockBoard.getSectorName());
            if (sq == null) {
                sq = new StockIndustryBoard();
            }
            sq.setSeqNo(stockBoard.getIndex());
            sq.setSectorName(stockBoard.getSectorName());
            sq.setChangePercent(stockBoard.getChangePercent());
            sq.setTotalVolume(stockBoard.getTotalVolume());
            sq.setTotalAmount(stockBoard.getTotalAmount());
            sq.setNetInflow(stockBoard.getNetInflow());
            sq.setRiseCount(stockBoard.getRiseCount());
            sq.setFallCount(stockBoard.getFallCount());
            sq.setAveragePrice(stockBoard.getAveragePrice());
            sq.setLeadingStock(stockBoard.getLeadingStock());
            sq.setLeadingStockPrice(stockBoard.getLeadingStockPrice());
            sq.setLeadingStockChangePercent(stockBoard.getLeadingStockChangePercent());
            LocalDate tradeDate = stockHelper.latestTradeDayFallback(LocalDate.now());
            sq.setTradeDate(tradeDate);
            sq.setCreateTime(now);

            list.add(sq);
        }
        stockIndustryBoardRepository.saveAll(list);
    }

    public Page<StockIndustryBoardVO> stockIndustryBoardPage(StockIndustryBoardPageReqVO reqVO, Pageable pageable) {
        Specification<StockIndustryBoard> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();



            if (StringUtils.isNotBlank(reqVO.getBoardName())) {
                predicates.add(cb.like(root.get("sectorName"), "%" + reqVO.getBoardName() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<StockIndustryBoard> page = stockIndustryBoardRepository.findAll(specification, pageable);

        return page.map(o -> {
            StockIndustryBoardVO vo = new StockIndustryBoardVO();
            BeanUtils.copyProperties(o, vo);
            return vo;
        });
    }

}
