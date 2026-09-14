package com.brotherc.aquant.portfolio.controller;

import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.common.model.dto.ResponseDTO;
import com.brotherc.aquant.portfolio.entity.UserPortfolioAccountSnapshot;
import com.brotherc.aquant.portfolio.entity.UserPortfolioCash;
import com.brotherc.aquant.portfolio.entity.UserPortfolioImportBatch;
import com.brotherc.aquant.portfolio.model.vo.*;
import com.brotherc.aquant.portfolio.service.UserPortfolioService;
import com.brotherc.aquant.portfolio.service.PortfolioTradeImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "用户持仓")
@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class UserPortfolioController {

    private final UserPortfolioService portfolioService;
    private final PortfolioTradeImportService tradeImportService;

    @Operation(summary = "查询当前用户的投资组合")
    @GetMapping("/list")
    public ResponseDTO<List<UserPortfolioVO>> list() {
        return ResponseDTO.success(portfolioService.getPortfolios());
    }

    @Operation(summary = "创建或修改投资组合")
    @PostMapping("/save")
    public ResponseDTO<UserPortfolioVO> save(@RequestBody @Valid PortfolioSaveReqVO reqVO) {
        return ResponseDTO.success(portfolioService.savePortfolio(reqVO));
    }

    @Operation(summary = "删除投资组合")
    @PostMapping("/delete")
    public ResponseDTO<Void> delete(@RequestParam Long portfolioId) {
        portfolioService.deletePortfolio(portfolioId);
        return ResponseDTO.success();
    }

    @Operation(summary = "查询投资组合下的券商账户")
    @GetMapping("/account/list")
    public ResponseDTO<List<BrokerAccountVO>> accountList(@RequestParam Long portfolioId) {
        return ResponseDTO.success(portfolioService.getAccounts(portfolioId));
    }

    @Operation(summary = "创建或修改券商账户")
    @PostMapping("/account/save")
    public ResponseDTO<BrokerAccountVO> saveAccount(@RequestBody @Valid BrokerAccountSaveReqVO reqVO) {
        return ResponseDTO.success(portfolioService.saveAccount(reqVO));
    }

    @Operation(summary = "删除券商账户")
    @PostMapping("/account/delete")
    public ResponseDTO<Void> deleteAccount(@RequestParam Long accountId) {
        portfolioService.deleteAccount(accountId);
        return ResponseDTO.success();
    }

    @Operation(summary = "批量保存交易流水，重复流水自动跳过")
    @PostMapping("/trade/save")
    public ResponseDTO<PortfolioImportResultVO> saveTrades(@RequestBody @Valid PortfolioTradeBatchReqVO reqVO) {
        return ResponseDTO.success(portfolioService.saveTrades(reqVO));
    }

    @Operation(summary = "校验并预览 CSV/Excel 交易流水文件")
    @PostMapping(value = "/trade/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseDTO<PortfolioTradeImportPreviewVO> previewTradeFile(
            @RequestParam Long accountId, @RequestPart("file") MultipartFile file) {
        return ResponseDTO.success(tradeImportService.preview(accountId, file));
    }

    @Operation(summary = "导入 CSV/Excel 交易流水文件")
    @PostMapping(value = "/trade/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseDTO<PortfolioImportResultVO> importTradeFile(
            @RequestParam Long accountId,
            @RequestParam(defaultValue = "true") boolean syncCashBalance,
            @RequestPart("file") MultipartFile file) {
        return ResponseDTO.success(tradeImportService.importFile(accountId, file, syncCashBalance));
    }

    @Operation(summary = "下载交易流水导入模板")
    @GetMapping("/trade/import/template")
    public ResponseEntity<byte[]> downloadTradeTemplate(@RequestParam(defaultValue = "xlsx") String format) {
        String extension = "csv".equalsIgnoreCase(format) ? "csv" : "xlsx";
        MediaType mediaType = "csv".equals(extension)
                ? new MediaType("text", "csv", StandardCharsets.UTF_8)
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("AQuant标准交易数据导入模板." + extension, StandardCharsets.UTF_8).build().toString())
                .body(tradeImportService.createTemplate(extension));
    }

    @Operation(summary = "分页查询投资组合或账户交易流水")
    @GetMapping("/trade/page")
    public ResponseDTO<Page<PortfolioTradeVO>> tradePage(
            @RequestParam(required = false) Long portfolioId,
            @RequestParam(required = false) Long accountId,
            @ParameterObject Pageable pageable) {
        if (portfolioId != null) {
            return ResponseDTO.success(portfolioService.getTrades(portfolioId, accountId, pageable));
        }
        if (accountId != null) {
            return ResponseDTO.success(portfolioService.getTrades(accountId, pageable));
        }
        throw ExceptionEnum.PORTFOLIO_TRADE_PARAMS_ILLEGAL.toException();
    }

    @Operation(summary = "按标的代码查询用户所有账户的历史交易流水")
    @GetMapping("/trade/byAsset")
    public ResponseDTO<List<PortfolioTradeVO>> tradeListByAsset(@RequestParam String assetCode) {
        return ResponseDTO.success(portfolioService.getTradesByAssetCode(assetCode));
    }

    @Operation(summary = "冲正交易流水")
    @PostMapping("/trade/reverse")
    public ResponseDTO<Void> reverseTrade(
            @RequestParam(required = false) Long accountId,
            @RequestParam Long tradeId) {
        if (accountId == null) {
            portfolioService.reverseTrade(tradeId);
        } else {
            portfolioService.reverseTrade(accountId, tradeId);
        }
        return ResponseDTO.success();
    }

    @Operation(summary = "撤销整个交易导入批次")
    @PostMapping("/importBatch/reverse")
    public ResponseDTO<Void> reverseImportBatch(@RequestParam Long accountId, @RequestParam Long batchId) {
        portfolioService.reverseImportBatch(accountId, batchId);
        return ResponseDTO.success();
    }

    @Operation(summary = "查询交易导入批次")
    @GetMapping("/importBatch/list")
    public ResponseDTO<List<UserPortfolioImportBatch>> importBatchList(@RequestParam Long accountId) {
        return ResponseDTO.success(portfolioService.getImportBatches(accountId));
    }

    @Operation(summary = "查询投资组合下的交易导入批次")
    @GetMapping("/batch/list")
    public ResponseDTO<List<UserPortfolioImportBatch>> batchList(
            @RequestParam Long portfolioId,
            @RequestParam(required = false) Long accountId) {
        return ResponseDTO.success(portfolioService.getImportBatches(portfolioId, accountId));
    }

    @Operation(summary = "按批次撤销导入交易")
    @PostMapping("/batch/reverse")
    public ResponseDTO<Void> reverseBatch(@RequestParam Long batchId) {
        portfolioService.reverseImportBatch(batchId);
        return ResponseDTO.success();
    }

    @Operation(summary = "维护账户现金余额")
    @PostMapping("/cash/save")
    public ResponseDTO<Void> saveCash(@RequestBody @Valid PortfolioCashSaveReqVO reqVO) {
        portfolioService.saveCash(reqVO);
        return ResponseDTO.success();
    }

    @Operation(summary = "查询投资组合或账户现金余额")
    @GetMapping("/cash/list")
    public ResponseDTO<List<UserPortfolioCash>> cashList(
            @RequestParam(required = false) Long portfolioId,
            @RequestParam(required = false) Long accountId) {
        if (portfolioId != null) {
            return ResponseDTO.success(portfolioService.getCash(portfolioId, accountId));
        }
        if (accountId != null) {
            return ResponseDTO.success(portfolioService.getCash(accountId));
        }
        throw ExceptionEnum.PORTFOLIO_TRADE_PARAMS_ILLEGAL.toException();
    }

    @Operation(summary = "查询投资组合持仓")
    @GetMapping("/position/list")
    public ResponseDTO<List<PortfolioPositionVO>> positionList(
            @RequestParam Long portfolioId, @RequestParam(required = false) Long accountId) {
        return ResponseDTO.success(portfolioService.getPositions(portfolioId, accountId));
    }

    @Operation(summary = "录入账户期初持仓")
    @PostMapping("/position/initialize")
    public ResponseDTO<PortfolioImportResultVO> initializePosition(
            @RequestBody @Valid PortfolioPositionInitReqVO reqVO) {
        return ResponseDTO.success(portfolioService.initializePosition(reqVO));
    }

    @Operation(summary = "查询投资组合资产概览")
    @GetMapping("/overview")
    public ResponseDTO<PortfolioOverviewVO> overview(@RequestParam Long portfolioId) {
        return ResponseDTO.success(portfolioService.getOverview(portfolioId));
    }

    @Operation(summary = "生成或刷新指定日期的持仓快照")
    @PostMapping("/snapshot/generate")
    public ResponseDTO<Void> generateSnapshot(
            @RequestParam Long portfolioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate) {
        portfolioService.generateSnapshot(portfolioId, snapshotDate);
        return ResponseDTO.success();
    }

    @Operation(summary = "查询投资组合的账户资产快照")
    @GetMapping("/snapshot/list")
    public ResponseDTO<List<UserPortfolioAccountSnapshot>> snapshotList(
            @RequestParam Long portfolioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseDTO.success(portfolioService.getSnapshots(portfolioId, startDate, endDate));
    }

}
