package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockIndustryBoard;
import com.brotherc.aquant.model.dto.akshare.StockBoardIndustryNameEm;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardPageReqVO;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardVO;
import com.brotherc.aquant.repository.StockIndustryBoardRepository;
import com.brotherc.aquant.utils.StockUtils;
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

    @Transactional(rollbackFor = Exception.class)
    public void save(List<StockBoardIndustryNameEm> stockBoardList, LocalDateTime now) {
        List<StockIndustryBoard> dbLlist = stockIndustryBoardRepository.findAll();
        Map<String, StockIndustryBoard> map = dbLlist.stream().collect(Collectors.toMap(StockIndustryBoard::getBoardCode, o -> o));

        List<StockIndustryBoard> list = new ArrayList<>();

        for (StockBoardIndustryNameEm stockBoard : stockBoardList) {
            StockIndustryBoard sq = map.get(stockBoard.getBlockCode());
            if (sq == null) {
                sq = new StockIndustryBoard();
            }
            sq.setBoardCode(stockBoard.getBlockCode());
            sq.setBoardName(stockBoard.getBlockName());
            sq.setRankNo(stockBoard.getRank());
            sq.setLatestPrice(stockBoard.getLatestPrice());
            sq.setChangeAmount(stockBoard.getChangeAmount());
            sq.setChangePercent(stockBoard.getChangePercent());
            sq.setTotalMarketValue(stockBoard.getTotalMarketValue());
            sq.setTurnoverRate(stockBoard.getTurnoverRate());
            sq.setUpCount(stockBoard.getUpCount());
            sq.setDownCount(stockBoard.getDownCount());
            sq.setLeadingStockName(stockBoard.getLeadingStock());
            sq.setLeadingStockChangePercent(stockBoard.getLeadingStockChangePercent());
            String tradeDate = StockUtils.latestTradeDayFallback(LocalDate.now()).toString();
            sq.setTradeDate(tradeDate);
            sq.setCreatedAt(now);

            list.add(sq);
        }
        stockIndustryBoardRepository.saveAll(list);
    }

    public Page<StockIndustryBoardVO> stockIndustryBoardPage(StockIndustryBoardPageReqVO reqVO, Pageable pageable) {
        Specification<StockIndustryBoard> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(reqVO.getBoardCode())) {
                predicates.add(cb.equal(root.get("boardCode"), reqVO.getBoardCode()));
            }

            if (StringUtils.isNotBlank(reqVO.getBoardName())) {
                predicates.add(cb.like(root.get("boardName"), "%" + reqVO.getBoardName() + "%"));
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
