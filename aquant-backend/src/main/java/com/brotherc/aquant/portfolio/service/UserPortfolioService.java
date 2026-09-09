package com.brotherc.aquant.portfolio.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.common.utils.DigestUtils;
import com.brotherc.aquant.common.utils.UserContext;
import com.brotherc.aquant.fund.entity.StockFundNetValue;
import com.brotherc.aquant.fund.repository.StockFundNetValueRepository;
import com.brotherc.aquant.portfolio.entity.*;
import com.brotherc.aquant.portfolio.model.vo.*;
import com.brotherc.aquant.portfolio.repository.*;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPortfolioService {

    private static final Set<String> ASSET_TYPES = Set.of("STOCK", "ETF", "FUND", "BOND", "CASH");
    private static final Set<String> POSITION_IN_TYPES = Set.of("BUY", "SUBSCRIBE", "POSITION_INIT", "TRANSFER_IN", "DIVIDEND_SHARE");
    private static final Set<String> POSITION_OUT_TYPES = Set.of("SELL", "REDEEM", "TRANSFER_OUT");
    private static final Set<String> AMOUNT_ONLY_TYPES = Set.of("DIVIDEND_CASH", "FEE", "TAX", "INTEREST");

    private final UserPortfolioRepository portfolioRepository;
    private final UserBrokerAccountRepository accountRepository;
    private final UserPortfolioImportBatchRepository importBatchRepository;
    private final UserPortfolioTradeRepository tradeRepository;
    private final UserPortfolioPositionRepository positionRepository;
    private final UserPortfolioCashRepository cashRepository;
    private final UserPortfolioAccountSnapshotRepository accountSnapshotRepository;
    private final UserPortfolioPositionSnapshotRepository positionSnapshotRepository;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockFundNetValueRepository fundNetValueRepository;

    @Transactional(rollbackFor = Exception.class)
    public List<UserPortfolioVO> getPortfolios() {
        Long userId = UserContext.requireCurrentUserId();
        List<UserPortfolio> portfolios = portfolioRepository
                .findAllByUserIdAndDeletedFalseOrderByDefaultPortfolioDescCreateTimeAsc(userId);
        if (portfolios.isEmpty()) {
            UserPortfolio portfolio = new UserPortfolio();
            portfolio.setUserId(userId);
            portfolio.setName("默认组合");
            portfolio.setDefaultPortfolio(true);
            portfolios = List.of(portfolioRepository.save(portfolio));
        }
        List<Long> portfolioIds = portfolios.stream().map(UserPortfolio::getId).toList();
        Map<Long, Long> accountCount = accountRepository
                .findAllByPortfolioIdInAndUserIdAndDeletedFalse(portfolioIds, userId).stream()
                .collect(Collectors.groupingBy(UserBrokerAccount::getPortfolioId, Collectors.counting()));
        return portfolios.stream().map(portfolio -> toPortfolioVO(portfolio,
                accountCount.getOrDefault(portfolio.getId(), 0L).intValue())).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public UserPortfolioVO savePortfolio(PortfolioSaveReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        String name = reqVO.getName().trim();
        UserPortfolio portfolio;
        if (reqVO.getId() == null) {
            if (portfolioRepository.existsByUserIdAndNameAndDeletedFalse(userId, name)) {
                throw ExceptionEnum.PORTFOLIO_NAME_DUPLICATE.toException();
            }
            portfolio = new UserPortfolio();
            portfolio.setUserId(userId);
            if (portfolioRepository.findAllByUserIdAndDeletedFalseOrderByDefaultPortfolioDescCreateTimeAsc(userId).isEmpty()) {
                portfolio.setDefaultPortfolio(true);
            }
        } else {
            portfolio = getPortfolio(reqVO.getId(), userId);
            if (!name.equals(portfolio.getName())
                    && portfolioRepository.existsByUserIdAndNameAndDeletedFalse(userId, name)) {
                throw ExceptionEnum.PORTFOLIO_NAME_DUPLICATE.toException();
            }
        }
        portfolio.setName(name);
        portfolio.setBaseCurrency(StringUtils.defaultIfBlank(reqVO.getBaseCurrency(), "CNY").toUpperCase());
        portfolio.setBenchmarkCode(StringUtils.trimToNull(reqVO.getBenchmarkCode()));
        if (Boolean.TRUE.equals(reqVO.getDefaultPortfolio())) {
            portfolio.setDefaultPortfolio(true);
        } else if (!Boolean.TRUE.equals(portfolio.getDefaultPortfolio())) {
            portfolio.setDefaultPortfolio(false);
        }
        if (portfolio.getDefaultPortfolio()) {
            for (UserPortfolio item : portfolioRepository
                    .findAllByUserIdAndDeletedFalseOrderByDefaultPortfolioDescCreateTimeAsc(userId)) {
                if (!Objects.equals(item.getId(), portfolio.getId()) && Boolean.TRUE.equals(item.getDefaultPortfolio())) {
                    item.setDefaultPortfolio(false);
                    portfolioRepository.save(item);
                }
            }
        }
        UserPortfolio savedPortfolio = portfolioRepository.save(portfolio);
        int accountCount = Math.toIntExact(accountRepository
                .countByPortfolioIdAndUserIdAndDeletedFalse(savedPortfolio.getId(), userId));
        return toPortfolioVO(savedPortfolio, accountCount);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePortfolio(Long portfolioId) {
        Long userId = UserContext.requireCurrentUserId();
        UserPortfolio portfolio = getPortfolio(portfolioId, userId);
        portfolio.setDeleted(true);
        portfolio.setDefaultPortfolio(false);
        portfolioRepository.save(portfolio);
        for (UserBrokerAccount account : accountRepository
                .findAllByPortfolioIdAndUserIdAndDeletedFalseOrderByCreateTimeAsc(portfolioId, userId)) {
            account.setDeleted(true);
            account.setStatus("DISABLED");
            accountRepository.save(account);
        }
        List<UserPortfolio> remaining = portfolioRepository
                .findAllByUserIdAndDeletedFalseOrderByDefaultPortfolioDescCreateTimeAsc(userId);
        if (!remaining.isEmpty() && remaining.stream().noneMatch(UserPortfolio::getDefaultPortfolio)) {
            remaining.get(0).setDefaultPortfolio(true);
            portfolioRepository.save(remaining.get(0));
        }
    }

    @Transactional(readOnly = true)
    public List<BrokerAccountVO> getAccounts(Long portfolioId) {
        Long userId = UserContext.requireCurrentUserId();
        getPortfolio(portfolioId, userId);
        return accountRepository.findAllByPortfolioIdAndUserIdAndDeletedFalseOrderByCreateTimeAsc(portfolioId, userId)
                .stream().map(this::toAccountVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public BrokerAccountVO saveAccount(BrokerAccountSaveReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        getPortfolio(reqVO.getPortfolioId(), userId);
        UserBrokerAccount account;
        String previousBrokerCode = null;
        String previousAccountHash = null;
        if (reqVO.getId() == null) {
            account = new UserBrokerAccount();
            account.setUserId(userId);
        } else {
            account = getAccount(reqVO.getId(), userId);
            previousBrokerCode = account.getBrokerCode();
            previousAccountHash = account.getAccountNoHash();
        }
        account.setPortfolioId(reqVO.getPortfolioId());
        account.setAccountName(reqVO.getAccountName().trim());
        account.setBrokerCode(reqVO.getBrokerCode().trim().toUpperCase());
        account.setBrokerName(StringUtils.trimToNull(reqVO.getBrokerName()));
        account.setAccountType(StringUtils.defaultIfBlank(reqVO.getAccountType(), "SECURITIES").toUpperCase());
        account.setSyncMode(StringUtils.defaultIfBlank(reqVO.getSyncMode(), "MANUAL").toUpperCase());
        if (previousAccountHash != null && !account.getBrokerCode().equals(previousBrokerCode)
                && StringUtils.isBlank(reqVO.getAccountNo())) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_TRADE_PARAMS_ILLEGAL,
                    "修改券商时需要重新输入券商账号");
        }
        if (StringUtils.isNotBlank(reqVO.getAccountNo())) {
            String accountNo = reqVO.getAccountNo().replaceAll("\\s+", "");
            String accountHash = DigestUtils.sha256((userId + "|" + account.getBrokerCode() + "|" + accountNo)
                    .getBytes(StandardCharsets.UTF_8));
            if ((!accountHash.equals(previousAccountHash) || !account.getBrokerCode().equals(previousBrokerCode))
                    && accountRepository.existsByUserIdAndBrokerCodeAndAccountNoHashAndDeletedFalse(
                    userId, account.getBrokerCode(), accountHash)) {
                throw ExceptionEnum.PORTFOLIO_ACCOUNT_DUPLICATE.toException();
            }
            account.setAccountNoHash(accountHash);
            account.setAccountNoMasked(maskAccountNo(accountNo));
        }
        return toAccountVO(accountRepository.save(account));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long accountId) {
        UserBrokerAccount account = getAccount(accountId, UserContext.requireCurrentUserId());
        account.setDeleted(true);
        account.setStatus("DISABLED");
        accountRepository.save(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public PortfolioImportResultVO saveTrades(PortfolioTradeBatchReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        UserBrokerAccount account = getAccount(reqVO.getAccountId(), userId);
        String source = StringUtils.defaultIfBlank(reqVO.getSource(), "MANUAL").toUpperCase();

        UserPortfolioImportBatch batch = new UserPortfolioImportBatch();
        batch.setAccountId(account.getId());
        batch.setSource(source);
        batch.setSourceFileName(StringUtils.trimToNull(reqVO.getSourceFileName()));
        batch.setSourceFileHash(StringUtils.trimToNull(reqVO.getSourceFileHash()));
        batch.setStatus("PROCESSING");
        batch.setTotalCount(reqVO.getTrades().size());
        batch = importBatchRepository.save(batch);

        int successCount = 0;
        int skipCount = 0;
        for (PortfolioTradeSaveReqVO item : reqVO.getTrades()) {
            validateTrade(item);
            UserPortfolioTrade trade = toTrade(account.getId(), batch.getId(), source, item);
            if (tradeRepository.existsByAccountIdAndDedupKey(account.getId(), trade.getDedupKey())) {
                skipCount++;
            } else {
                tradeRepository.save(trade);
                successCount++;
            }
        }
        rebuildPositions(account);
        batch.setSuccessCount(successCount);
        batch.setSkipCount(skipCount);
        batch.setStatus("SUCCESS");
        batch.setFinishTime(LocalDateTime.now());
        importBatchRepository.save(batch);
        return new PortfolioImportResultVO(batch.getId(), batch.getTotalCount(), successCount, skipCount);
    }

    @Transactional(rollbackFor = Exception.class)
    public PortfolioImportResultVO initializePosition(PortfolioPositionInitReqVO reqVO) {
        PortfolioTradeSaveReqVO trade = new PortfolioTradeSaveReqVO();
        trade.setAssetType(reqVO.getAssetType());
        trade.setMarket(reqVO.getMarket());
        trade.setAssetCode(reqVO.getAssetCode());
        trade.setAssetName(reqVO.getAssetName());
        trade.setTradeType("POSITION_INIT");
        trade.setTradeTime(reqVO.getPositionTime() == null ? LocalDateTime.now() : reqVO.getPositionTime());
        trade.setQuantity(reqVO.getQuantity());
        trade.setPrice(reqVO.getCostPrice());
        trade.setGrossAmount(reqVO.getQuantity().multiply(reqVO.getCostPrice()));
        trade.setCurrency(reqVO.getCurrency());
        trade.setRemark(reqVO.getRemark());

        PortfolioTradeBatchReqVO batch = new PortfolioTradeBatchReqVO();
        batch.setAccountId(reqVO.getAccountId());
        batch.setSource("MANUAL");
        batch.setTrades(List.of(trade));
        return saveTrades(batch);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reverseTrade(Long accountId, Long tradeId) {
        Long userId = UserContext.requireCurrentUserId();
        UserBrokerAccount account = getAccount(accountId, userId);
        UserPortfolioTrade trade = tradeRepository.findByIdAndAccountId(tradeId, accountId)
                .orElseThrow(ExceptionEnum.PORTFOLIO_TRADE_PARAMS_ILLEGAL::toException);
        if (!"REVERSED".equals(trade.getStatus())) {
            trade.setStatus("REVERSED");
            tradeRepository.save(trade);
            rebuildPositions(account);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reverseImportBatch(Long accountId, Long batchId) {
        Long userId = UserContext.requireCurrentUserId();
        UserBrokerAccount account = getAccount(accountId, userId);
        UserPortfolioImportBatch batch = importBatchRepository.findByIdAndAccountId(batchId, accountId)
                .orElseThrow(ExceptionEnum.PORTFOLIO_TRADE_PARAMS_ILLEGAL::toException);
        if (!"REVERSED".equals(batch.getStatus())) {
            List<UserPortfolioTrade> trades = tradeRepository.findAllByImportBatchIdAndAccountId(batchId, accountId);
            for (UserPortfolioTrade trade : trades) {
                trade.setStatus("REVERSED");
            }
            tradeRepository.saveAll(trades);
            batch.setStatus("REVERSED");
            batch.setFinishTime(LocalDateTime.now());
            importBatchRepository.save(batch);
            rebuildPositions(account);
        }
    }

    @Transactional(readOnly = true)
    public Page<PortfolioTradeVO> getTrades(Long accountId, Pageable pageable) {
        getAccount(accountId, UserContext.requireCurrentUserId());
        return tradeRepository.findAllByAccountIdOrderByTradeTimeDescIdDesc(accountId, pageable)
                .map(this::toTradeVO);
    }

    @Transactional(readOnly = true)
    public List<UserPortfolioImportBatch> getImportBatches(Long accountId) {
        getAccount(accountId, UserContext.requireCurrentUserId());
        return importBatchRepository.findAllByAccountIdOrderByCreateTimeDesc(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveCash(PortfolioCashSaveReqVO reqVO) {
        getAccount(reqVO.getAccountId(), UserContext.requireCurrentUserId());
        if ((reqVO.getAvailableBalance() != null && reqVO.getAvailableBalance().compareTo(reqVO.getTotalBalance()) > 0)
                || (reqVO.getFrozenBalance() != null && reqVO.getFrozenBalance().compareTo(reqVO.getTotalBalance()) > 0)) {
            throw ExceptionEnum.PORTFOLIO_TRADE_PARAMS_ILLEGAL.toException();
        }
        String currency = reqVO.getCurrency().toUpperCase();
        UserPortfolioCash cash = cashRepository.findByAccountIdAndCurrency(reqVO.getAccountId(), currency)
                .orElseGet(UserPortfolioCash::new);
        cash.setAccountId(reqVO.getAccountId());
        cash.setCurrency(currency);
        cash.setTotalBalance(reqVO.getTotalBalance());
        cash.setAvailableBalance(reqVO.getAvailableBalance());
        cash.setFrozenBalance(reqVO.getFrozenBalance());
        cashRepository.save(cash);
    }

    @Transactional(readOnly = true)
    public List<UserPortfolioCash> getCash(Long accountId) {
        getAccount(accountId, UserContext.requireCurrentUserId());
        return cashRepository.findAllByAccountIdOrderByCurrencyAsc(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<PortfolioPositionVO> getPositions(Long portfolioId, Long accountId) {
        Long userId = UserContext.requireCurrentUserId();
        UserPortfolio portfolio = getPortfolio(portfolioId, userId);
        List<UserBrokerAccount> accounts = getTargetAccounts(portfolioId, accountId, userId);
        refreshPositionPrices(accounts);
        return buildPositionVOs(portfolio, accounts);
    }

    @Transactional(rollbackFor = Exception.class)
    public PortfolioOverviewVO getOverview(Long portfolioId) {
        Long userId = UserContext.requireCurrentUserId();
        UserPortfolio portfolio = getPortfolio(portfolioId, userId);
        List<UserBrokerAccount> accounts = getTargetAccounts(portfolioId, null, userId);
        refreshPositionPrices(accounts);
        List<PortfolioPositionVO> positions = buildPositionVOs(portfolio, accounts);
        List<Long> accountIds = accounts.stream().map(UserBrokerAccount::getId).toList();
        List<UserPortfolioCash> cashList = accountIds.isEmpty() ? List.of() : cashRepository.findAllByAccountIdIn(accountIds);

        BigDecimal cashAmount = cashList.stream()
                .filter(cash -> portfolio.getBaseCurrency().equals(cash.getCurrency()))
                .map(UserPortfolioCash::getTotalBalance).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal marketValue = positions.stream().filter(item -> portfolio.getBaseCurrency().equals(item.getCurrency()))
                .map(PortfolioPositionVO::getMarketValue).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal costAmount = positions.stream().filter(item -> portfolio.getBaseCurrency().equals(item.getCurrency()))
                .map(PortfolioPositionVO::getCostAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unrealizedProfit = positions.stream().filter(item -> portfolio.getBaseCurrency().equals(item.getCurrency()))
                .map(PortfolioPositionVO::getUnrealizedProfit).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PortfolioOverviewVO result = new PortfolioOverviewVO();
        result.setPortfolioId(portfolio.getId());
        result.setPortfolioName(portfolio.getName());
        result.setBaseCurrency(portfolio.getBaseCurrency());
        result.setCashAmount(cashAmount);
        result.setMarketValue(marketValue);
        result.setTotalAsset(cashAmount.add(marketValue));
        result.setCostAmount(costAmount);
        result.setUnrealizedProfit(unrealizedProfit);
        result.setUnrealizedProfitRate(costAmount.signum() == 0 ? null
                : unrealizedProfit.multiply(BigDecimal.valueOf(100)).divide(costAmount, 4, RoundingMode.HALF_UP));
        result.setPositionCount(positions.size());
        result.setUnpricedAssetCount((int) positions.stream().filter(item -> item.getLatestPrice() == null).count());
        result.setUnsupportedCurrencyCount((int) (cashList.stream()
                .filter(cash -> !portfolio.getBaseCurrency().equals(cash.getCurrency())).count()
                + positions.stream().filter(position -> !portfolio.getBaseCurrency().equals(position.getCurrency())).count()));
        result.setCalculateTime(LocalDateTime.now());
        result.setPositions(positions);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void generateSnapshot(Long portfolioId, LocalDate snapshotDate) {
        Long userId = UserContext.requireCurrentUserId();
        UserPortfolio portfolio = getPortfolio(portfolioId, userId);
        generateSnapshot(portfolio, getTargetAccounts(portfolioId, null, userId), snapshotDate);
    }

    @Transactional(readOnly = true)
    public List<Long> getActivePortfolioIds() {
        return portfolioRepository.findAllByDeletedFalseOrderByIdAsc().stream().map(UserPortfolio::getId).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void generateSystemSnapshot(Long portfolioId, LocalDate snapshotDate) {
        UserPortfolio portfolio = portfolioRepository.findById(portfolioId)
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .orElseThrow(ExceptionEnum.PORTFOLIO_NOT_FOUND::toException);
        List<UserBrokerAccount> accounts = accountRepository
                .findAllByPortfolioIdAndUserIdAndDeletedFalseOrderByCreateTimeAsc(portfolioId, portfolio.getUserId());
        generateSnapshot(portfolio, accounts, snapshotDate);
    }

    private void generateSnapshot(
            UserPortfolio portfolio, List<UserBrokerAccount> accounts, LocalDate snapshotDate
    ) {
        refreshPositionPrices(accounts);
        LocalDate date = snapshotDate == null ? LocalDate.now() : snapshotDate;

        for (UserBrokerAccount account : accounts) {
            List<UserPortfolioPosition> positions = positionRepository.findAllByAccountIdOrderByMarketValueDesc(account.getId());
            BigDecimal cashAmount = cashRepository.findByAccountIdAndCurrency(account.getId(), portfolio.getBaseCurrency())
                    .map(UserPortfolioCash::getTotalBalance).orElse(BigDecimal.ZERO);
            BigDecimal marketValue = positions.stream().filter(item -> portfolio.getBaseCurrency().equals(item.getCurrency()))
                    .map(UserPortfolioPosition::getMarketValue).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal costAmount = positions.stream().filter(item -> portfolio.getBaseCurrency().equals(item.getCurrency()))
                    .map(UserPortfolioPosition::getCostAmount).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal profit = positions.stream().filter(item -> portfolio.getBaseCurrency().equals(item.getCurrency()))
                    .map(UserPortfolioPosition::getUnrealizedProfit).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            UserPortfolioAccountSnapshot snapshot = accountSnapshotRepository
                    .findByAccountIdAndSnapshotDate(account.getId(), date).orElseGet(UserPortfolioAccountSnapshot::new);
            snapshot.setAccountId(account.getId());
            snapshot.setSnapshotDate(date);
            snapshot.setCurrency(portfolio.getBaseCurrency());
            snapshot.setCashAmount(cashAmount);
            snapshot.setMarketValue(marketValue);
            snapshot.setTotalAsset(cashAmount.add(marketValue));
            snapshot.setCostAmount(costAmount);
            snapshot.setUnrealizedProfit(profit);
            snapshot.setUnpricedAssetCount((int) positions.stream().filter(item -> item.getLatestPrice() == null).count());
            snapshot = accountSnapshotRepository.save(snapshot);

            positionSnapshotRepository.deleteByAccountSnapshotId(snapshot.getId());
            List<UserPortfolioPositionSnapshot> details = new ArrayList<>();
            for (UserPortfolioPosition position : positions) {
                UserPortfolioPositionSnapshot detail = new UserPortfolioPositionSnapshot();
                detail.setAccountSnapshotId(snapshot.getId());
                detail.setAccountId(account.getId());
                detail.setAssetType(position.getAssetType());
                detail.setMarket(position.getMarket());
                detail.setAssetCode(position.getAssetCode());
                detail.setAssetName(position.getAssetName());
                detail.setCurrency(position.getCurrency());
                detail.setQuantity(position.getQuantity());
                detail.setCostPrice(position.getCostPrice());
                detail.setCostAmount(position.getCostAmount());
                detail.setLatestPrice(position.getLatestPrice());
                detail.setMarketValue(position.getMarketValue());
                detail.setUnrealizedProfit(position.getUnrealizedProfit());
                details.add(detail);
            }
            positionSnapshotRepository.saveAll(details);
        }
    }

    @Transactional(readOnly = true)
    public List<UserPortfolioAccountSnapshot> getSnapshots(Long portfolioId, LocalDate startDate, LocalDate endDate) {
        Long userId = UserContext.requireCurrentUserId();
        getPortfolio(portfolioId, userId);
        List<Long> accountIds = getTargetAccounts(portfolioId, null, userId).stream()
                .map(UserBrokerAccount::getId).toList();
        if (accountIds.isEmpty()) {
            return List.of();
        }
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusYears(1) : startDate;
        if (start.isAfter(end)) {
            throw ExceptionEnum.PORTFOLIO_TRADE_PARAMS_ILLEGAL.toException();
        }
        return accountSnapshotRepository.findAllByAccountIdInAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                accountIds, start, end);
    }

    private void rebuildPositions(UserBrokerAccount account) {
        Map<String, UserPortfolioPosition> positionMap = new LinkedHashMap<>();
        for (UserPortfolioTrade trade : tradeRepository
                .findAllByAccountIdAndStatusOrderByTradeTimeAscIdAsc(account.getId(), "NORMAL")) {
            if (!POSITION_IN_TYPES.contains(trade.getTradeType()) && !POSITION_OUT_TYPES.contains(trade.getTradeType())) {
                continue;
            }
            String key = trade.getAssetType() + ":" + trade.getAssetCode();
            UserPortfolioPosition position = positionMap.computeIfAbsent(key, ignored -> {
                UserPortfolioPosition value = new UserPortfolioPosition();
                value.setAccountId(account.getId());
                value.setAssetType(trade.getAssetType());
                value.setMarket(trade.getMarket());
                value.setAssetCode(trade.getAssetCode());
                value.setAssetName(trade.getAssetName());
                value.setCurrency(trade.getCurrency());
                value.setQuantity(BigDecimal.ZERO);
                value.setCostAmount(BigDecimal.ZERO);
                return value;
            });
            BigDecimal quantity = trade.getQuantity();
            if (POSITION_IN_TYPES.contains(trade.getTradeType())) {
                position.setQuantity(position.getQuantity().add(quantity));
                if (!"DIVIDEND_SHARE".equals(trade.getTradeType())) {
                    position.setCostAmount(position.getCostAmount().add(calculateIncomingCost(trade)));
                }
            } else {
                if (position.getQuantity().compareTo(quantity) < 0) {
                    throw new BusinessException(ExceptionEnum.PORTFOLIO_POSITION_INSUFFICIENT,
                            trade.getAssetCode() + "卖出或转出数量超过当前持仓");
                }
                BigDecimal reducedCost = position.getQuantity().signum() == 0 ? BigDecimal.ZERO
                        : position.getCostAmount().multiply(quantity)
                        .divide(position.getQuantity(), 8, RoundingMode.HALF_UP);
                position.setQuantity(position.getQuantity().subtract(quantity));
                position.setCostAmount(position.getCostAmount().subtract(reducedCost));
            }
            position.setAssetName(StringUtils.defaultIfBlank(trade.getAssetName(), position.getAssetName()));
        }

        positionRepository.deleteByAccountId(account.getId());
        LocalDateTime now = LocalDateTime.now();
        List<UserPortfolioPosition> positions = positionMap.values().stream()
                .filter(position -> position.getQuantity().signum() > 0)
                .peek(position -> {
                    position.setAvailableQuantity(position.getQuantity());
                    position.setCostPrice(position.getCostAmount().divide(position.getQuantity(), 8, RoundingMode.HALF_UP));
                    position.setCalculateTime(now);
                }).toList();
        positionRepository.saveAll(positions);
        refreshPositionPrices(List.of(account));
    }

    private void refreshPositionPrices(List<UserBrokerAccount> accounts) {
        if (accounts.isEmpty()) {
            return;
        }
        List<Long> accountIds = accounts.stream().map(UserBrokerAccount::getId).toList();
        List<UserPortfolioPosition> positions = positionRepository.findAllByAccountIdInOrderByMarketValueDesc(accountIds);
        List<String> quoteCodes = positions.stream()
                .filter(position -> "STOCK".equals(position.getAssetType()) || "ETF".equals(position.getAssetType()))
                .map(UserPortfolioPosition::getAssetCode).distinct().toList();
        Map<String, StockQuote> quoteMap = quoteCodes.isEmpty() ? Map.of() : stockQuoteRepository.findByCodeIn(quoteCodes).stream()
                .collect(Collectors.toMap(StockQuote::getCode, quote -> quote, (first, second) -> first));

        for (UserPortfolioPosition position : positions) {
            BigDecimal latestPrice = null;
            LocalDate quoteDate = null;
            if ("STOCK".equals(position.getAssetType()) || "ETF".equals(position.getAssetType())) {
                StockQuote quote = quoteMap.get(position.getAssetCode());
                if (quote != null) {
                    latestPrice = quote.getLatestPrice();
                    quoteDate = quote.getCreatedAt() == null ? null : quote.getCreatedAt().toLocalDate();
                    position.setAssetName(StringUtils.defaultIfBlank(position.getAssetName(), quote.getName()));
                }
            } else if ("FUND".equals(position.getAssetType())) {
                List<StockFundNetValue> netValues = fundNetValueRepository.findLatestByFundCode(
                        plainCode(position.getAssetCode()), org.springframework.data.domain.PageRequest.of(0, 1));
                if (!netValues.isEmpty()) {
                    latestPrice = netValues.get(0).getUnitNav();
                    quoteDate = netValues.get(0).getNavDate() == null ? null : netValues.get(0).getNavDate().toLocalDate();
                }
            }
            position.setLatestPrice(latestPrice);
            position.setQuoteDate(quoteDate);
            position.setMarketValue(latestPrice == null ? null
                    : latestPrice.multiply(position.getQuantity()).setScale(4, RoundingMode.HALF_UP));
            position.setUnrealizedProfit(position.getMarketValue() == null ? null
                    : position.getMarketValue().subtract(position.getCostAmount()));
            position.setCalculateTime(LocalDateTime.now());
        }
        positionRepository.saveAll(positions);
    }

    private List<PortfolioPositionVO> buildPositionVOs(UserPortfolio portfolio, List<UserBrokerAccount> accounts) {
        if (accounts.isEmpty()) {
            return List.of();
        }
        Map<Long, String> accountNames = accounts.stream()
                .collect(Collectors.toMap(UserBrokerAccount::getId, UserBrokerAccount::getAccountName));
        List<UserPortfolioPosition> positions = positionRepository.findAllByAccountIdInOrderByMarketValueDesc(accountNames.keySet());
        BigDecimal totalMarketValue = positions.stream()
                .filter(position -> portfolio.getBaseCurrency().equals(position.getCurrency()))
                .map(UserPortfolioPosition::getMarketValue).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal denominator = totalMarketValue;
        return positions.stream().map(position -> {
            PortfolioPositionVO vo = new PortfolioPositionVO();
            vo.setId(position.getId());
            vo.setAccountId(position.getAccountId());
            vo.setAccountName(accountNames.get(position.getAccountId()));
            vo.setAssetType(position.getAssetType());
            vo.setMarket(position.getMarket());
            vo.setAssetCode(position.getAssetCode());
            vo.setAssetName(position.getAssetName());
            vo.setCurrency(position.getCurrency());
            vo.setQuantity(position.getQuantity());
            vo.setAvailableQuantity(position.getAvailableQuantity());
            vo.setCostPrice(position.getCostPrice());
            vo.setCostAmount(position.getCostAmount());
            vo.setLatestPrice(position.getLatestPrice());
            vo.setMarketValue(position.getMarketValue());
            vo.setUnrealizedProfit(position.getUnrealizedProfit());
            vo.setUnrealizedProfitRate(position.getCostAmount() == null || position.getCostAmount().signum() == 0
                    || position.getUnrealizedProfit() == null ? null
                    : position.getUnrealizedProfit().multiply(BigDecimal.valueOf(100))
                    .divide(position.getCostAmount(), 4, RoundingMode.HALF_UP));
            vo.setPositionRatio(position.getMarketValue() == null || denominator.signum() == 0
                    || !portfolio.getBaseCurrency().equals(position.getCurrency()) ? null
                    : position.getMarketValue().multiply(BigDecimal.valueOf(100))
                    .divide(denominator, 4, RoundingMode.HALF_UP));
            vo.setQuoteDate(position.getQuoteDate());
            return vo;
        }).toList();
    }

    private UserPortfolioTrade toTrade(Long accountId, Long batchId, String source, PortfolioTradeSaveReqVO reqVO) {
        UserPortfolioTrade trade = new UserPortfolioTrade();
        trade.setAccountId(accountId);
        trade.setImportBatchId(batchId);
        trade.setAssetType(reqVO.getAssetType().toUpperCase());
        trade.setMarket(StringUtils.upperCase(StringUtils.trimToNull(reqVO.getMarket())));
        trade.setAssetCode(normalizeAssetCode(trade.getAssetType(), trade.getMarket(), reqVO.getAssetCode()));
        trade.setAssetName(StringUtils.trimToNull(reqVO.getAssetName()));
        trade.setTradeType(reqVO.getTradeType().toUpperCase());
        trade.setTradeTime(reqVO.getTradeTime());
        trade.setSettlementDate(reqVO.getSettlementDate());
        trade.setQuantity(reqVO.getQuantity());
        trade.setPrice(reqVO.getPrice());
        trade.setGrossAmount(reqVO.getGrossAmount());
        trade.setCommission(zeroIfNull(reqVO.getCommission()));
        trade.setStampDuty(zeroIfNull(reqVO.getStampDuty()));
        trade.setTransferFee(zeroIfNull(reqVO.getTransferFee()));
        trade.setOtherFee(zeroIfNull(reqVO.getOtherFee()));
        trade.setNetAmount(reqVO.getNetAmount());
        trade.setCurrency(StringUtils.defaultIfBlank(reqVO.getCurrency(), "CNY").toUpperCase());
        trade.setSource(source);
        trade.setSourceTradeId(StringUtils.trimToNull(reqVO.getSourceTradeId()));
        trade.setRemark(StringUtils.trimToNull(reqVO.getRemark()));
        String uniqueSource = StringUtils.defaultIfBlank(trade.getSourceTradeId(),
                String.join("|", trade.getAssetType(), StringUtils.defaultString(trade.getAssetCode()), trade.getTradeType(),
                        trade.getTradeTime().toString(), String.valueOf(trade.getQuantity()), String.valueOf(trade.getPrice()),
                        String.valueOf(trade.getGrossAmount()), String.valueOf(trade.getNetAmount())));
        trade.setDedupKey(DigestUtils.sha256((accountId + "|" + source + "|" + uniqueSource)
                .getBytes(StandardCharsets.UTF_8)));
        return trade;
    }

    private void validateTrade(PortfolioTradeSaveReqVO reqVO) {
        String assetType = StringUtils.upperCase(reqVO.getAssetType());
        String tradeType = StringUtils.upperCase(reqVO.getTradeType());
        if (!ASSET_TYPES.contains(assetType)
                || (!POSITION_IN_TYPES.contains(tradeType) && !POSITION_OUT_TYPES.contains(tradeType)
                && !AMOUNT_ONLY_TYPES.contains(tradeType))) {
            throw ExceptionEnum.PORTFOLIO_TRADE_PARAMS_ILLEGAL.toException();
        }
        boolean positionTrade = POSITION_IN_TYPES.contains(tradeType) || POSITION_OUT_TYPES.contains(tradeType);
        if (positionTrade && (StringUtils.isBlank(reqVO.getAssetCode()) || reqVO.getQuantity() == null
                || reqVO.getQuantity().signum() <= 0)) {
            throw ExceptionEnum.PORTFOLIO_TRADE_PARAMS_ILLEGAL.toException();
        }
        if (positionTrade && !"DIVIDEND_SHARE".equals(tradeType)
                && reqVO.getGrossAmount() == null && (reqVO.getPrice() == null || reqVO.getPrice().signum() < 0)) {
            throw ExceptionEnum.PORTFOLIO_TRADE_PARAMS_ILLEGAL.toException();
        }
    }

    private BigDecimal calculateIncomingCost(UserPortfolioTrade trade) {
        BigDecimal gross = trade.getGrossAmount();
        if (gross == null) {
            gross = trade.getPrice().multiply(trade.getQuantity());
        }
        return gross.abs().add(totalFee(trade));
    }

    private BigDecimal totalFee(UserPortfolioTrade trade) {
        return zeroIfNull(trade.getCommission()).add(zeroIfNull(trade.getStampDuty()))
                .add(zeroIfNull(trade.getTransferFee())).add(zeroIfNull(trade.getOtherFee()));
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeAssetCode(String assetType, String market, String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        String normalized = code.trim().toLowerCase();
        if ("FUND".equals(assetType) || "BOND".equals(assetType) || "CASH".equals(assetType)) {
            return normalized;
        }
        if (normalized.startsWith("sh") || normalized.startsWith("sz") || normalized.startsWith("bj")) {
            return normalized;
        }
        String prefix = StringUtils.lowerCase(market);
        if (prefix == null || !Set.of("sh", "sz", "bj").contains(prefix)) {
            prefix = normalized.startsWith("6") ? "sh"
                    : (normalized.startsWith("4") || normalized.startsWith("8") || normalized.startsWith("9")) ? "bj" : "sz";
        }
        return prefix + normalized;
    }

    private String plainCode(String code) {
        return code != null && code.length() > 6 ? code.substring(code.length() - 6) : code;
    }

    private String maskAccountNo(String accountNo) {
        if (accountNo.length() <= 4) {
            return "****";
        }
        return "****" + accountNo.substring(accountNo.length() - 4);
    }

    private UserPortfolio getPortfolio(Long portfolioId, Long userId) {
        return portfolioRepository.findByIdAndUserIdAndDeletedFalse(portfolioId, userId)
                .orElseThrow(ExceptionEnum.PORTFOLIO_NOT_FOUND::toException);
    }

    private UserBrokerAccount getAccount(Long accountId, Long userId) {
        return accountRepository.findByIdAndUserIdAndDeletedFalse(accountId, userId)
                .orElseThrow(ExceptionEnum.PORTFOLIO_ACCOUNT_NOT_FOUND::toException);
    }

    private List<UserBrokerAccount> getTargetAccounts(Long portfolioId, Long accountId, Long userId) {
        if (accountId == null) {
            return accountRepository.findAllByPortfolioIdAndUserIdAndDeletedFalseOrderByCreateTimeAsc(portfolioId, userId);
        }
        UserBrokerAccount account = getAccount(accountId, userId);
        if (!Objects.equals(account.getPortfolioId(), portfolioId)) {
            throw ExceptionEnum.PORTFOLIO_ACCOUNT_NOT_FOUND.toException();
        }
        return List.of(account);
    }

    private UserPortfolioVO toPortfolioVO(UserPortfolio portfolio, int accountCount) {
        UserPortfolioVO vo = new UserPortfolioVO();
        vo.setId(portfolio.getId());
        vo.setName(portfolio.getName());
        vo.setBaseCurrency(portfolio.getBaseCurrency());
        vo.setBenchmarkCode(portfolio.getBenchmarkCode());
        vo.setDefaultPortfolio(portfolio.getDefaultPortfolio());
        vo.setAccountCount(accountCount);
        vo.setCreateTime(portfolio.getCreateTime());
        return vo;
    }

    private BrokerAccountVO toAccountVO(UserBrokerAccount account) {
        BrokerAccountVO vo = new BrokerAccountVO();
        vo.setId(account.getId());
        vo.setPortfolioId(account.getPortfolioId());
        vo.setAccountName(account.getAccountName());
        vo.setBrokerCode(account.getBrokerCode());
        vo.setBrokerName(account.getBrokerName());
        vo.setAccountNoMasked(account.getAccountNoMasked());
        vo.setAccountType(account.getAccountType());
        vo.setSyncMode(account.getSyncMode());
        vo.setStatus(account.getStatus());
        vo.setCreateTime(account.getCreateTime());
        return vo;
    }

    private PortfolioTradeVO toTradeVO(UserPortfolioTrade trade) {
        PortfolioTradeVO vo = new PortfolioTradeVO();
        vo.setId(trade.getId());
        vo.setImportBatchId(trade.getImportBatchId());
        vo.setAssetType(trade.getAssetType());
        vo.setMarket(trade.getMarket());
        vo.setAssetCode(trade.getAssetCode());
        vo.setAssetName(trade.getAssetName());
        vo.setTradeType(trade.getTradeType());
        vo.setTradeTime(trade.getTradeTime());
        vo.setSettlementDate(trade.getSettlementDate());
        vo.setQuantity(trade.getQuantity());
        vo.setPrice(trade.getPrice());
        vo.setGrossAmount(trade.getGrossAmount());
        vo.setTotalFee(totalFee(trade));
        vo.setNetAmount(trade.getNetAmount());
        vo.setCurrency(trade.getCurrency());
        vo.setSource(trade.getSource());
        vo.setSourceTradeId(trade.getSourceTradeId());
        vo.setStatus(trade.getStatus());
        vo.setRemark(trade.getRemark());
        return vo;
    }
}
