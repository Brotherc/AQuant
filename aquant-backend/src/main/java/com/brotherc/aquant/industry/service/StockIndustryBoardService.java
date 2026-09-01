package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.utils.StockHelper;
import com.brotherc.aquant.industry.entity.StockIndustryBoard;
import com.brotherc.aquant.industry.model.vo.StockIndustryBoardPageReqVO;
import com.brotherc.aquant.industry.model.vo.StockIndustryBoardVO;
import com.brotherc.aquant.industry.repository.StockIndustryBoardRepository;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustrySummaryThs;
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
        List<StockIndustryBoard> databaseList = stockIndustryBoardRepository.findAll();
        Map<String, StockIndustryBoard> boardByName = databaseList.stream()
                .collect(Collectors.toMap(StockIndustryBoard::getSectorName, item -> item));
        List<StockIndustryBoard> saveList = new ArrayList<>();

        for (StockBoardIndustrySummaryThs board : stockBoardList) {
            StockIndustryBoard target = boardByName.get(board.getSectorName());
            if (target == null) {
                target = new StockIndustryBoard();
            }
            target.setSeqNo(board.getIndex());
            target.setSectorName(board.getSectorName());
            target.setChangePercent(board.getChangePercent());
            target.setTotalVolume(board.getTotalVolume());
            target.setTotalAmount(board.getTotalAmount());
            target.setNetInflow(board.getNetInflow());
            target.setRiseCount(board.getRiseCount());
            target.setFallCount(board.getFallCount());
            target.setAveragePrice(board.getAveragePrice());
            target.setLeadingStock(board.getLeadingStock());
            target.setLeadingStockPrice(board.getLeadingStockPrice());
            target.setLeadingStockChangePercent(board.getLeadingStockChangePercent());
            target.setTradeDate(stockHelper.latestTradeDayFallback(LocalDate.now()));
            target.setCreateTime(now);
            saveList.add(target);
        }
        stockIndustryBoardRepository.saveAll(saveList);
    }

    public Page<StockIndustryBoardVO> stockIndustryBoardPage(StockIndustryBoardPageReqVO reqVO, Pageable pageable) {
        Specification<StockIndustryBoard> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.isNotBlank(reqVO.getBoardName())) {
                predicates.add(criteriaBuilder.like(root.get("sectorName"), "%" + reqVO.getBoardName() + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return stockIndustryBoardRepository.findAll(specification, pageable).map(board -> {
            StockIndustryBoardVO view = new StockIndustryBoardVO();
            BeanUtils.copyProperties(board, view);
            return view;
        });
    }
}
