package com.brotherc.aquant.portfolio.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.common.utils.UserContext;
import com.brotherc.aquant.portfolio.model.dto.PortfolioTradeFileParseResult;
import com.brotherc.aquant.portfolio.entity.UserBrokerAccount;
import com.brotherc.aquant.portfolio.model.vo.PortfolioCashSaveReqVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioImportResultVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeBatchReqVO;
import com.brotherc.aquant.portfolio.model.vo.PortfolioTradeImportPreviewVO;
import com.brotherc.aquant.portfolio.repository.UserBrokerAccountRepository;
import com.brotherc.aquant.portfolio.repository.UserPortfolioImportBatchRepository;
import com.brotherc.aquant.portfolio.service.importer.BrokerTradeFileAdapter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioTradeImportService {

    private static final int PREVIEW_ROW_LIMIT = 20;
    private static final int ERROR_PREVIEW_LIMIT = 100;

    private final PortfolioTradeFileParser fileParser;
    private final UserPortfolioService portfolioService;
    private final UserBrokerAccountRepository accountRepository;
    private final UserPortfolioImportBatchRepository importBatchRepository;
    private final List<BrokerTradeFileAdapter> brokerTradeFileAdapters;

    public PortfolioTradeImportPreviewVO preview(Long accountId, String brokerCodeOverride, MultipartFile file) {
        String brokerCode = resolveBrokerCode(accountId, brokerCodeOverride);
        PortfolioTradeFileParseResult parsed = parse(brokerCode, file);
        PortfolioTradeImportPreviewVO preview = new PortfolioTradeImportPreviewVO();
        preview.setFileName(parsed.getFileName());
        preview.setFileHash(parsed.getFileHash());
        preview.setTotalCount(parsed.getTotalCount());
        preview.setValidCount(parsed.getTrades().size());
        preview.setErrorCount(parsed.getErrors().size());
        preview.setLatestCashBalance(parsed.getLatestCashBalance());
        preview.setCashBalanceDate(parsed.getCashBalanceDate());
        preview.setCashBalanceCurrency(parsed.getCashBalanceCurrency());
        preview.setAlreadyImported(importBatchRepository.existsByAccountIdAndSourceFileHashAndStatus(
                accountId, parsed.getFileHash(), "SUCCESS"));
        preview.setSampleTrades(parsed.getTrades().stream().limit(PREVIEW_ROW_LIMIT).toList());
        preview.setErrors(parsed.getErrors().stream().limit(ERROR_PREVIEW_LIMIT).toList());
        return preview;
    }

    @Transactional(rollbackFor = Exception.class)
    public PortfolioImportResultVO importFile(Long accountId, String brokerCodeOverride, MultipartFile file,
                                              boolean syncCashBalance) {
        String brokerCode = resolveBrokerCode(accountId, brokerCodeOverride);
        PortfolioTradeFileParseResult parsed = parse(brokerCode, file);
        if (!parsed.getErrors().isEmpty()) {
            String firstError = parsed.getErrors().get(0).getMessage();
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "文件有 " + parsed.getErrors().size() + " 行校验失败，首个错误：第 "
                            + parsed.getErrors().get(0).getRowNumber() + " 行 " + firstError);
        }
        if (importBatchRepository.existsByAccountIdAndSourceFileHashAndStatus(
                accountId, parsed.getFileHash(), "SUCCESS")) {
            throw ExceptionEnum.PORTFOLIO_IMPORT_FILE_DUPLICATE.toException();
        }

        PortfolioTradeBatchReqVO request = new PortfolioTradeBatchReqVO();
        request.setAccountId(accountId);
        request.setSource("FILE");
        request.setSourceFileName(parsed.getFileName());
        request.setSourceFileHash(parsed.getFileHash());
        request.setTrades(parsed.getTrades());
        PortfolioImportResultVO result = portfolioService.saveTrades(request);
        if (syncCashBalance && parsed.getLatestCashBalance() != null) {
            if (parsed.getLatestCashBalance().signum() < 0) {
                throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                        "券商文件中的最新资金余额不能小于 0");
            }
            PortfolioCashSaveReqVO cashRequest = new PortfolioCashSaveReqVO();
            cashRequest.setAccountId(accountId);
            cashRequest.setCurrency(parsed.getCashBalanceCurrency());
            cashRequest.setTotalBalance(parsed.getLatestCashBalance());
            portfolioService.saveCash(cashRequest);
        }
        return result;
    }

    public byte[] createTemplate(String format) {
        return fileParser.createTemplate(format);
    }

    private PortfolioTradeFileParseResult parse(String brokerCode, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > PortfolioTradeFileParser.MAX_FILE_SIZE) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID,
                    "文件不能为空且不能超过 5 MB");
        }
        try {
            byte[] content = file.getBytes();
            // 优先按指定/账户券商匹配适配器；都不中时按文件内容签名兜底识别（避免账户券商代码
            // 与实际文件来源不符——如东财账户挂着 CMS 代码——导致走错解析器）
            return brokerTradeFileAdapters.stream()
                    .filter(adapter -> adapter.supports(brokerCode, file.getOriginalFilename(), content))
                    .findFirst()
                    .or(() -> brokerTradeFileAdapters.stream()
                            .filter(adapter -> adapter.supports("*", file.getOriginalFilename(), content))
                            .findFirst())
                    .map(adapter -> adapter.parse(file.getOriginalFilename(), content))
                    .orElseGet(() -> fileParser.parse(file.getOriginalFilename(), content));
        } catch (IOException e) {
            throw new BusinessException(ExceptionEnum.PORTFOLIO_IMPORT_FILE_INVALID, "读取上传文件失败");
        }
    }

    /**
     * 导入券商代码：显式指定（前端券商选择器）优先，其次回落账户配置的 brokerCode。
     * MANUAL 是同步方式的缺省值而非券商代码，不作为匹配依据。
     */
    private String resolveBrokerCode(Long accountId, String brokerCodeOverride) {
        UserBrokerAccount account = validateAccount(accountId);
        if (StringUtils.isNotBlank(brokerCodeOverride) && !"MANUAL".equalsIgnoreCase(brokerCodeOverride)) {
            return brokerCodeOverride;
        }
        return StringUtils.defaultIfBlank(account.getBrokerCode(), "STANDARD");
    }

    private UserBrokerAccount validateAccount(Long accountId) {
        if (accountId == null) {
            throw ExceptionEnum.PORTFOLIO_ACCOUNT_NOT_FOUND.toException();
        }
        return accountRepository.findByIdAndUserIdAndDeletedFalse(accountId, UserContext.requireCurrentUserId())
                .orElseThrow(ExceptionEnum.PORTFOLIO_ACCOUNT_NOT_FOUND::toException);
    }

}
