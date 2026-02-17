package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.watchlist.*;
import com.brotherc.aquant.service.StockWatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "自选股票")
@RestController
@RequestMapping("/stockWatchlist")
@RequiredArgsConstructor
public class StockWatchlistController {

    private final StockWatchlistService watchlistService;

    @Operation(summary = "获取所有自选分组")
    @GetMapping("/group/list")
    public ResponseDTO<List<WatchlistGroupVO>> getAllGroups() {
        return ResponseDTO.success(watchlistService.getAllGroups());
    }

    @Operation(summary = "创建自选分组")
    @PostMapping("/group/create")
    public ResponseDTO<Void> createGroup(@RequestBody @Valid WatchlistGroupReqVO reqVO) {
        watchlistService.createGroup(reqVO);
        return ResponseDTO.success();
    }

    @Operation(summary = "删除自选分组")
    @PostMapping("/group/delete")
    public ResponseDTO<Void> deleteGroup(@RequestBody @Valid WatchlistIdReqVO reqVO) {
        watchlistService.deleteGroup(reqVO.getId());
        return ResponseDTO.success();
    }

    @Operation(summary = "获取分组下的股票列表")
    @GetMapping("/stock/list")
    public ResponseDTO<List<WatchlistStockVO>> getStocksByGroupId(@RequestParam Long groupId) {
        return ResponseDTO.success(watchlistService.getStocksByGroupId(groupId));
    }

    @Operation(summary = "添加股票到自选")
    @PostMapping("/stock/add")
    public ResponseDTO<Void> addStock(@RequestBody @Valid WatchlistStockReqVO reqVO) {
        watchlistService.addStockToWatchlist(reqVO);
        return ResponseDTO.success();
    }

    @Operation(summary = "从自选中移除股票")
    @PostMapping("/stock/remove")
    public ResponseDTO<Void> removeStock(@RequestBody @Valid WatchlistStockReqVO reqVO) {
        watchlistService.removeStockFromWatchlist(reqVO.getGroupId(), reqVO.getStockCode());
        return ResponseDTO.success();
    }

}
