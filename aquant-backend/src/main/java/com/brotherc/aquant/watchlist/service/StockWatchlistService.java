package com.brotherc.aquant.watchlist.service;

import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.watchlist.entity.StockWatchlistGroup;
import com.brotherc.aquant.watchlist.entity.StockWatchlistStock;
import com.brotherc.aquant.common.enums.NotificationAssetType;
import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.watchlist.model.vo.WatchlistGroupReqVO;
import com.brotherc.aquant.watchlist.model.vo.WatchlistGroupVO;
import com.brotherc.aquant.watchlist.model.vo.WatchlistGroupUpdateReqVO;
import com.brotherc.aquant.watchlist.model.vo.WatchlistStockReqVO;
import com.brotherc.aquant.watchlist.model.vo.WatchlistStockVO;
import com.brotherc.aquant.watchlist.model.vo.WatchlistStockMoveGroupReqVO;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import com.brotherc.aquant.watchlist.repository.StockWatchlistGroupRepository;
import com.brotherc.aquant.watchlist.repository.StockWatchlistStockRepository;
import com.brotherc.aquant.indicator.repository.StockValuationMetricsRepository;
import com.brotherc.aquant.indicator.repository.StockDupontAnalysisRepository;
import com.brotherc.aquant.notification.repository.StockNotificationRepository;
import com.brotherc.aquant.indicator.entity.StockValuationMetrics;
import com.brotherc.aquant.indicator.entity.StockDupontAnalysis;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.brotherc.aquant.dividend.repository.StockDividendRepository;
import com.brotherc.aquant.dividend.entity.StockDividend;
import com.brotherc.aquant.fund.entity.StockFundInfo;
import com.brotherc.aquant.fund.entity.StockFundNetValue;
import com.brotherc.aquant.fund.repository.StockFundInfoRepository;
import com.brotherc.aquant.fund.repository.StockFundNetValueRepository;
import com.brotherc.aquant.watchlist.model.vo.WatchlistDividendVO;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockWatchlistService {

    private final StockWatchlistGroupRepository groupRepository;
    private final StockWatchlistStockRepository stockRepository;
    private final StockQuoteRepository quoteRepository;
    private final StockValuationMetricsRepository valuationMetricsRepository;
    private final StockDupontAnalysisRepository dupontAnalysisRepository;
    private final StockDividendRepository dividendRepository;
    private final StockNotificationRepository notificationRepository;
    private final StockFundInfoRepository stockFundInfoRepository;
    private final StockFundNetValueRepository stockFundNetValueRepository;

    public List<WatchlistGroupVO> getAllGroups(Long userId, String type) {
        if (userId == null) {
            return new ArrayList<>();
        }
        List<StockWatchlistGroup> groups;
        if (StringUtils.isNotBlank(type)) {
            groups = groupRepository.findAllByUserIdAndTypeOrderBySortNoAsc(userId, type);
        } else {
            groups = groupRepository.findAllByUserIdOrderBySortNoAsc(userId);
        }
        if (CollectionUtils.isEmpty(groups)) {
            return new ArrayList<>();
        }
        List<Long> groupIds = groups.stream().map(StockWatchlistGroup::getId).toList();
        List<StockWatchlistStock> allStocks = stockRepository.findByGroupIdIn(groupIds);
        Map<Long, Long> countMap = allStocks.stream()
                .collect(Collectors.groupingBy(StockWatchlistStock::getGroupId, Collectors.counting()));

        return groups.stream().map(g -> {
            WatchlistGroupVO vo = new WatchlistGroupVO();
            vo.setId(g.getId());
            vo.setName(g.getName());
            vo.setType(g.getType());
            vo.setSortNo(g.getSortNo());
            vo.setCount(countMap.getOrDefault(g.getId(), 0L).intValue());
            return vo;
        }).toList();
    }

    public List<WatchlistStockVO> getStocksByGroupId(Long groupId, Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        // 校验归属
        StockWatchlistGroup group = groupRepository.findByIdAndUserId(groupId, userId)
                .orElseThrow(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND::toException);

        List<StockWatchlistStock> watchlistStocks = stockRepository.findByGroupIdOrderBySortNoDesc(groupId);
        if (CollectionUtils.isEmpty(watchlistStocks)) {
            return new ArrayList<>();
        }

        List<String> codes6 = watchlistStocks.stream().map(StockWatchlistStock::getStockCode).toList();

        String groupType = group.getType();
        if ("FUND".equalsIgnoreCase(groupType)) {
            List<String> notificationCodes = notificationRepository.findDistinctStockCodeByUserIdAndAssetTypeAndStockCodeIn(
                    userId, NotificationAssetType.FUND.getType(), codes6);
            List<StockFundInfo> fundInfos = stockFundInfoRepository.findByFundCodeIn(codes6);
            Map<String, StockFundInfo> infoMap = fundInfos.stream()
                    .collect(Collectors.toMap(StockFundInfo::getFundCode, f -> f, (a, b) -> a));

            List<StockFundNetValue> netValues = stockFundNetValueRepository.findByFundCodeInOrderByNavDateDesc(codes6);
            Map<String, StockFundNetValue> latestNetValueMap = new java.util.HashMap<>();
            for (StockFundNetValue nv : netValues) {
                if (nv != null && nv.getFundCode() != null) {
                    latestNetValueMap.putIfAbsent(nv.getFundCode(), nv);
                }
            }

            return watchlistStocks.stream().map(ws -> {
                WatchlistStockVO vo = new WatchlistStockVO();
                vo.setTargetType("FUND");
                vo.setStockCode(ws.getStockCode());
                vo.setSortNo(ws.getSortNo());
                vo.setHasNotification(notificationCodes.contains(ws.getStockCode()));

                StockFundInfo fundInfo = infoMap.get(ws.getStockCode());
                if (fundInfo != null) {
                    vo.setStockName(fundInfo.getFundName());
                    vo.setFundType(fundInfo.getFundType());
                } else {
                    vo.setStockName(ws.getStockCode());
                }

                StockFundNetValue netValue = latestNetValueMap.get(ws.getStockCode());
                if (netValue != null) {
                    vo.setUnitNetValue(netValue.getUnitNav());
                    vo.setDailyGrowthRate(netValue.getDailyGrowthRate());
                    vo.setNetValueDate(netValue.getNavDate() != null ? netValue.getNavDate().toLocalDate().toString() : null);
                    vo.setLatestPrice(netValue.getUnitNav());
                    vo.setChangePercent(netValue.getDailyGrowthRate());
                }

                return vo;
            }).toList();
        }

        List<String> notificationCodes = notificationRepository.findDistinctStockCodeByUserIdAndAssetTypeAndStockCodeIn(
                userId, NotificationAssetType.STOCK.getType(), codes6);

        // 智能补全并批量查询核心行情
        List<String> candidates = new ArrayList<>();
        for (String c6 : codes6) {
            candidates.add("sh" + c6);
            candidates.add("sz" + c6);
            candidates.add("bj" + c6);
        }

        List<StockQuote> quotes = quoteRepository.findByCodeIn(candidates);
        if (quotes.size() < codes6.size()) {
            quotes = quoteRepository.findAll();
        }

        Map<String, StockQuote> quoteMap = quotes.stream()
                .collect(Collectors.toMap(q -> {
                    String c = q.getCode();
                    return c.length() > 6 ? c.substring(c.length() - 6) : c;
                }, q -> q, (a, b) -> a));

        List<StockValuationMetrics> valuationList = valuationMetricsRepository.findByStockCodeIn(candidates);
        Map<String, StockValuationMetrics> valuationMap = valuationList.stream()
                .collect(Collectors.toMap(v -> {
                    String c = v.getStockCode();
                    if(c == null) return "";
                    return c.length() > 6 ? c.substring(c.length() - 6) : c;
                }, v -> v, (a, b) -> a));

        List<StockDupontAnalysis> dupontList = dupontAnalysisRepository.findByStockCodeIn(candidates);
        Map<String, StockDupontAnalysis> dupontMap = dupontList.stream()
                .collect(Collectors.toMap(d -> {
                    String c = d.getStockCode();
                    if(c == null) return "";
                    return c.length() > 6 ? c.substring(c.length() - 6) : c;
                }, d -> d, (a, b) -> a));

        List<StockDividend> dividendList = dividendRepository.findByStockCodeIn(codes6);
        Map<String, List<WatchlistDividendVO>> dividendMap = dividendList.stream()
                .collect(Collectors.groupingBy(d -> {
                    String c = d.getStockCode();
                    if(c == null) return "";
                    return c.length() > 6 ? c.substring(c.length() - 6) : c;
                }, Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                        .sorted(
                                Comparator.comparing(StockDividend::getProposalAnnouncementDate,
                                                Comparator.nullsLast(Comparator.reverseOrder()))
                                        .thenComparing(StockDividend::getReportDate,
                                                Comparator.nullsLast(Comparator.reverseOrder()))
                        )
                        .limit(2)
                        .map(d -> {
                            WatchlistDividendVO vo = new WatchlistDividendVO();
                            vo.setProposalAnnouncementDate(d.getProposalAnnouncementDate() != null ? d.getProposalAnnouncementDate().toString() : null);
                            vo.setPlanStatus(d.getPlanStatus());
                            vo.setCashDividendRatio(d.getCashDividendRatio());
                            vo.setBonusShareRatio(d.getBonusShareRatio());
                            vo.setTransferShareRatio(d.getTransferShareRatio());
                            return vo;
                        }).toList())));

        return watchlistStocks.stream().map(ws -> {
            WatchlistStockVO vo = new WatchlistStockVO();
            vo.setTargetType("STOCK");
            vo.setStockCode(ws.getStockCode());
            vo.setSortNo(ws.getSortNo());
            
            StockQuote quote = quoteMap.get(ws.getStockCode());
            if (quote != null) {
                vo.setStockName(quote.getName());
                vo.setLatestPrice(quote.getLatestPrice());
                vo.setChangePercent(quote.getChangePercent());
            }

            StockValuationMetrics valuation = valuationMap.get(ws.getStockCode());
            if (valuation != null) {
                vo.setPe(valuation.getPeTtm());
                vo.setPeg(valuation.getPeg());
            }

            StockDupontAnalysis dupont = dupontMap.get(ws.getStockCode());
            if (dupont != null) {
                vo.setRoe(dupont.getRoe3yAvg()); 
            }
            
            vo.setRecentDividends(dividendMap.get(ws.getStockCode()));
            vo.setHasNotification(notificationCodes.contains(ws.getStockCode()));
            
            return vo;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public StockWatchlistGroup createGroup(WatchlistGroupReqVO reqVO, Long userId) {
        if (userId == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }
        String groupType = StringUtils.isNotBlank(reqVO.getType()) ? reqVO.getType() : "STOCK";
        if (groupRepository.existsByUserIdAndNameAndType(userId, reqVO.getName(), groupType)) {
            throw new BusinessException(ExceptionEnum.WATCHLIST_GROUP_NAME_DUPLICATE);
        }
        StockWatchlistGroup group = new StockWatchlistGroup();
        group.setUserId(userId);
        group.setName(reqVO.getName());
        group.setType(groupType);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());

        Integer maxSort = groupRepository.findAllByUserIdOrderBySortNoAsc(userId).stream()
                .map(StockWatchlistGroup::getSortNo)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        group.setSortNo(maxSort + 1);

        return groupRepository.save(group);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(WatchlistGroupUpdateReqVO reqVO, Long userId) {
        if (userId == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }
        StockWatchlistGroup group = groupRepository.findByIdAndUserId(reqVO.getId(), userId)
                .orElseThrow(() -> new BusinessException(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND));

        if (!group.getName().equals(reqVO.getName()) && groupRepository.existsByUserIdAndName(userId, reqVO.getName())) {
            throw new BusinessException(ExceptionEnum.WATCHLIST_GROUP_NAME_DUPLICATE);
        }

        group.setName(reqVO.getName());
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long groupId, Long userId) {
        if (userId == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }
        groupRepository.findByIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessException(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND));

        groupRepository.deleteById(groupId);
        stockRepository.deleteByGroupId(groupId);
    }

    /**
     * 分组上移/下移（按 sortNo 升序展示）
     */
    @Transactional(rollbackFor = Exception.class)
    public void moveGroup(com.brotherc.aquant.watchlist.model.vo.WatchlistGroupMoveReqVO reqVO, Long userId) {
        if (userId == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }

        groupRepository.findByIdAndUserId(reqVO.getId(), userId)
                .orElseThrow(() -> new BusinessException(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND));

        List<StockWatchlistGroup> groups = groupRepository.findAllByUserIdOrderBySortNoAsc(userId);
        int index = -1;
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).getId().equals(reqVO.getId())) {
                index = i;
                break;
            }
        }
        if (index == -1) return;

        String action = reqVO.getAction();
        StockWatchlistGroup current = groups.get(index);

        if ("UP".equalsIgnoreCase(action)) {
            if (index == 0) return;
            StockWatchlistGroup prev = groups.get(index - 1);
            int currentSort = current.getSortNo() != null ? current.getSortNo() : 0;
            int prevSort = prev.getSortNo() != null ? prev.getSortNo() : 0;

            if (currentSort == prevSort) {
                current.setSortNo(prevSort - 1);
            } else {
                current.setSortNo(prevSort);
                prev.setSortNo(currentSort);
                groupRepository.save(prev);
            }
            groupRepository.save(current);
        } else if ("DOWN".equalsIgnoreCase(action)) {
            if (index == groups.size() - 1) return;
            StockWatchlistGroup next = groups.get(index + 1);
            int currentSort = current.getSortNo() != null ? current.getSortNo() : 0;
            int nextSort = next.getSortNo() != null ? next.getSortNo() : 0;

            if (currentSort == nextSort) {
                current.setSortNo(nextSort + 1);
            } else {
                current.setSortNo(nextSort);
                next.setSortNo(currentSort);
                groupRepository.save(next);
            }
            groupRepository.save(current);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void addStockToWatchlist(WatchlistStockReqVO reqVO, Long userId) {
        doAddStockToWatchlist(reqVO, userId);
    }

    private void doAddStockToWatchlist(WatchlistStockReqVO reqVO, Long userId) {
        if (userId == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }
        // 校验分组所有人
        StockWatchlistGroup group = groupRepository.findByIdAndUserId(reqVO.getGroupId(), userId)
                .orElseThrow(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND::toException);

        String inputCode = reqVO.getStockCode();
        String standardizedCode = inputCode.length() > 6 ? inputCode.substring(inputCode.length() - 6) : inputCode;

        if ("FUND".equalsIgnoreCase(group.getType())) {
            Optional<StockFundInfo> fundOpt = stockFundInfoRepository.findByFundCode(standardizedCode);
            if (fundOpt.isEmpty() && (standardizedCode.length() != 6 || !standardizedCode.matches("\\d{6}"))) {
                throw new BusinessException(ExceptionEnum.FUND_NOT_FOUND);
            }
        } else {
            StockQuote quote = quoteRepository.findByCode(inputCode);
            if (quote == null && inputCode.length() == 6) {
                List<String> candidates = List.of("sh" + inputCode, "sz" + inputCode, "bj" + inputCode);
                List<StockQuote> found = quoteRepository.findByCodeIn(candidates);
                if (!found.isEmpty()) {
                    quote = found.get(0);
                }
            }
            if (quote == null) {
                quote = quoteRepository.findAll().stream()
                        .filter(q -> q.getCode().endsWith(standardizedCode))
                        .findFirst()
                        .orElse(null);
            }

            if (quote == null) {
                throw new BusinessException(ExceptionEnum.STOCK_NOT_FOUND);
            }
        }

        if (stockRepository.existsByGroupIdAndStockCode(reqVO.getGroupId(), standardizedCode)) {
            return;
        }
        StockWatchlistStock ws = new StockWatchlistStock();
        ws.setGroupId(reqVO.getGroupId());
        ws.setStockCode(standardizedCode);
        ws.setCreatedAt(LocalDateTime.now());

        Integer maxSort = stockRepository.findByGroupIdOrderBySortNoDesc(reqVO.getGroupId()).stream()
                .map(StockWatchlistStock::getSortNo)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        ws.setSortNo(maxSort + 1);

        stockRepository.save(ws);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeStockFromWatchlist(Long groupId, String stockCode, Long userId) {
        if (userId == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }
        groupRepository.findByIdAndUserId(groupId, userId)
                .orElseThrow(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND::toException);

        stockRepository.deleteByGroupIdAndStockCode(groupId, stockCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStockSort(com.brotherc.aquant.watchlist.model.vo.WatchlistStockReorderReqVO reqVO, Long userId) {
        if (userId == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }
        groupRepository.findByIdAndUserId(reqVO.getGroupId(), userId)
                .orElseThrow(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND::toException);

        Long groupId = reqVO.getGroupId();
        List<String> codes = reqVO.getStockCodes();

        List<StockWatchlistStock> stocks = stockRepository.findByGroupIdOrderBySortNoDesc(groupId);
        Map<String, StockWatchlistStock> stockMap = stocks.stream()
                .collect(Collectors.toMap(StockWatchlistStock::getStockCode, s -> s));

        int size = codes.size();
        for (int i = 0; i < size; i++) {
            String code = codes.get(i);
            StockWatchlistStock s = stockMap.get(code);
            if (s != null) {
                s.setSortNo(size - i);
                stockRepository.save(s);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void moveStock(com.brotherc.aquant.watchlist.model.vo.WatchlistStockMoveReqVO reqVO, Long userId) {
        if (userId == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }
        groupRepository.findByIdAndUserId(reqVO.getGroupId(), userId)
                .orElseThrow(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND::toException);

        Long groupId = reqVO.getGroupId();
        String stockCode = reqVO.getStockCode();
        String action = reqVO.getAction();

        List<StockWatchlistStock> stocks = stockRepository.findByGroupIdOrderBySortNoDesc(groupId);
        int index = -1;
        for (int i = 0; i < stocks.size(); i++) {
            if (stocks.get(i).getStockCode().equals(stockCode)) {
                index = i;
                break;
            }
        }
        if (index == -1) return;

        StockWatchlistStock current = stocks.get(index);

        if ("TOP".equalsIgnoreCase(action)) {
            if (index == 0) return;
            int maxSortNo = stocks.stream()
                    .map(StockWatchlistStock::getSortNo)
                    .filter(java.util.Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0);
            current.setSortNo(maxSortNo + 1);
            stockRepository.save(current);
        } else if ("UP".equalsIgnoreCase(action)) {
            if (index == 0) return;
            StockWatchlistStock prev = stocks.get(index - 1);
            int currentSort = current.getSortNo() != null ? current.getSortNo() : 0;
            int prevSort = prev.getSortNo() != null ? prev.getSortNo() : 0;
            
            if (currentSort == prevSort) {
                current.setSortNo(prevSort + 1);
            } else {
                current.setSortNo(prevSort);
                prev.setSortNo(currentSort);
                stockRepository.save(prev);
            }
            stockRepository.save(current);
        } else if ("DOWN".equalsIgnoreCase(action)) {
            if (index == stocks.size() - 1) return;
            StockWatchlistStock next = stocks.get(index + 1);
            int currentSort = current.getSortNo() != null ? current.getSortNo() : 0;
            int nextSort = next.getSortNo() != null ? next.getSortNo() : 0;
            
            if (currentSort == nextSort) {
                current.setSortNo(nextSort - 1);
            } else {
                current.setSortNo(nextSort);
                next.setSortNo(currentSort);
                stockRepository.save(next);
            }
            stockRepository.save(current);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void moveStockToGroup(WatchlistStockMoveGroupReqVO reqVO, Long userId) {
        if (userId == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }
        // 校验两个分组归属
        groupRepository.findByIdAndUserId(reqVO.getFromGroupId(), userId)
                .orElseThrow(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND::toException);
        groupRepository.findByIdAndUserId(reqVO.getToGroupId(), userId)
                .orElseThrow(ExceptionEnum.WATCHLIST_GROUP_NOT_FOUND::toException);

        String stockCode = reqVO.getStockCode();
        Long fromGroupId = reqVO.getFromGroupId();
        Long toGroupId = reqVO.getToGroupId();

        if (fromGroupId.equals(toGroupId)) {
            return;
        }

        stockRepository.deleteByGroupIdAndStockCode(fromGroupId, stockCode);

        if (!stockRepository.existsByGroupIdAndStockCode(toGroupId, stockCode)) {
            WatchlistStockReqVO addReq = new WatchlistStockReqVO();
            addReq.setGroupId(toGroupId);
            addReq.setStockCode(stockCode);
            doAddStockToWatchlist(addReq, userId);
        }
    }

}
