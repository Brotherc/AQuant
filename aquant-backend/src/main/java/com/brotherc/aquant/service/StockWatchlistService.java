package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.entity.StockWatchlistGroup;
import com.brotherc.aquant.entity.StockWatchlistStock;
import com.brotherc.aquant.exception.BusinessException;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.vo.watchlist.WatchlistGroupReqVO;
import com.brotherc.aquant.model.vo.watchlist.WatchlistGroupVO;
import com.brotherc.aquant.model.vo.watchlist.WatchlistStockReqVO;
import com.brotherc.aquant.model.vo.watchlist.WatchlistStockVO;
import com.brotherc.aquant.repository.StockQuoteRepository;
import com.brotherc.aquant.repository.StockWatchlistGroupRepository;
import com.brotherc.aquant.repository.StockWatchlistStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockWatchlistService {

    private final StockWatchlistGroupRepository groupRepository;
    private final StockWatchlistStockRepository stockRepository;
    private final StockQuoteRepository quoteRepository;

    public List<WatchlistGroupVO> getAllGroups() {
        List<StockWatchlistGroup> groups = groupRepository.findAllByOrderBySortNoAsc();
        return groups.stream().map(g -> {
            WatchlistGroupVO vo = new WatchlistGroupVO();
            vo.setId(g.getId());
            vo.setName(g.getName());
            vo.setSortNo(g.getSortNo());
            return vo;
        }).collect(Collectors.toList());
    }

    public List<WatchlistStockVO> getStocksByGroupId(Long groupId) {
        List<StockWatchlistStock> watchlistStocks = stockRepository.findByGroupIdOrderBySortNoDesc(groupId);
        if (CollectionUtils.isEmpty(watchlistStocks)) {
            return new ArrayList<>();
        }

        // 这里的 code 在仓库里可能带 sh/sz 前缀，也可能不带，需要根据 StockQuote 的实际情况处理
        // 假设 StockQuote.code 是带前缀的，而 watchlist 存的 stockCode 是 6位代码，或者一致。
        // 根据之前的代码 `o.getCode().substring(2)`，StockQuote.code 应该是带前缀的（如 sh600519）

        List<String> codes6 = watchlistStocks.stream().map(StockWatchlistStock::getStockCode).collect(Collectors.toList());

        // 智能补全并批量查询
        List<String> candidates = new ArrayList<>();
        for (String c6 : codes6) {
            candidates.add("sh" + c6);
            candidates.add("sz" + c6);
            candidates.add("bj" + c6);
        }

        List<StockQuote> quotes = quoteRepository.findByCodeIn(candidates);
        // 如果依然有漏掉的，再查询全部进行补全（兜底逻辑）
        if (quotes.size() < codes6.size()) {
            quotes = quoteRepository.findAll();
        }

        Map<String, StockQuote> quoteMap = quotes.stream()
                .collect(Collectors.toMap(q -> {
                    String c = q.getCode();
                    return c.length() > 6 ? c.substring(c.length() - 6) : c;
                }, q -> q, (a, b) -> a));

        return watchlistStocks.stream().map(ws -> {
            WatchlistStockVO vo = new WatchlistStockVO();
            vo.setStockCode(ws.getStockCode());
            vo.setSortNo(ws.getSortNo());
            StockQuote quote = quoteMap.get(ws.getStockCode());
            if (quote != null) {
                vo.setStockName(quote.getName());
                vo.setLatestPrice(quote.getLatestPrice());
                vo.setChangePercent(quote.getChangePercent());
            }
            return vo;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public StockWatchlistGroup createGroup(WatchlistGroupReqVO reqVO) {
        if (groupRepository.existsByName(reqVO.getName())) {
            throw new BusinessException(ExceptionEnum.WATCHLIST_GROUP_NAME_DUPLICATE);
        }
        StockWatchlistGroup group = new StockWatchlistGroup();
        group.setName(reqVO.getName());
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());

        // 自动计算排序号：当前最大值 + 1
        Integer maxSort = groupRepository.findAll().stream()
                .map(StockWatchlistGroup::getSortNo)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        group.setSortNo(maxSort + 1);

        return groupRepository.save(group);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long groupId) {
        groupRepository.deleteById(groupId);
        stockRepository.deleteByGroupId(groupId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addStockToWatchlist(WatchlistStockReqVO reqVO) {
        String inputCode = reqVO.getStockCode();

        // 智能补全逻辑
        String standardizedCode = inputCode.length() > 6 ? inputCode.substring(inputCode.length() - 6) : inputCode;

        StockQuote quote;
        // 1. 尝试直接匹配（带或不带前缀）
        quote = quoteRepository.findByCode(inputCode);

        // 2. 如果没找到，尝试补全常见前缀并批量查询
        if (quote == null && inputCode.length() == 6) {
            List<String> candidates = List.of("sh" + inputCode, "sz" + inputCode, "bj" + inputCode);
            List<StockQuote> found = quoteRepository.findByCodeIn(candidates);
            if (!found.isEmpty()) {
                quote = found.get(0);
            }
        }

        // 3. 否则查询全部股票进行后缀匹配（兜底逻辑）
        if (quote == null) {
            quote = quoteRepository.findAll().stream()
                    .filter(q -> q.getCode().endsWith(standardizedCode))
                    .findFirst()
                    .orElse(null);
        }

        if (quote == null) {
            throw new BusinessException(ExceptionEnum.STOCK_NOT_FOUND);
        }

        if (stockRepository.existsByGroupIdAndStockCode(reqVO.getGroupId(), standardizedCode)) {
            return;
        }
        StockWatchlistStock ws = new StockWatchlistStock();
        ws.setGroupId(reqVO.getGroupId());
        ws.setStockCode(standardizedCode);
        ws.setCreatedAt(LocalDateTime.now());

        // 自动计算排序号：取最大值 + 1，配合 Desc 排序即可实现最新添加在最前
        Integer maxSort = stockRepository.findByGroupIdOrderBySortNoDesc(reqVO.getGroupId()).stream()
                .map(StockWatchlistStock::getSortNo)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        ws.setSortNo(maxSort + 1);

        stockRepository.save(ws);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeStockFromWatchlist(Long groupId, String stockCode) {
        stockRepository.deleteByGroupIdAndStockCode(groupId, stockCode);
    }

}
