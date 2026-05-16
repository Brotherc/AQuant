package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.watchlist.*;
import com.brotherc.aquant.service.StockWatchlistService;
import com.brotherc.aquant.utils.UserContext;
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
        Long userId = UserContext.requireCurrentUserId();
        return ResponseDTO.success(watchlistService.getAllGroups(userId));
    }

    @Operation(summary = "创建自选分组")
    @PostMapping("/group/create")
    public ResponseDTO<Void> createGroup(@RequestBody @Valid WatchlistGroupReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        watchlistService.createGroup(reqVO, userId);
        return ResponseDTO.success();
    }

    @Operation(summary = "修改自选分组名称")
    @PostMapping("/group/update")
    public ResponseDTO<Void> updateGroup(@RequestBody @Valid WatchlistGroupUpdateReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        watchlistService.updateGroup(reqVO, userId);
        return ResponseDTO.success();
    }

    @Operation(summary = "删除自选分组")
    @PostMapping("/group/delete")
    public ResponseDTO<Void> deleteGroup(@RequestBody @Valid WatchlistIdReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        watchlistService.deleteGroup(reqVO.getId(), userId);
        return ResponseDTO.success();
    }

    @Operation(summary = "分组上移/下移")
    @PostMapping("/group/move")
    public ResponseDTO<Void> moveGroup(@RequestBody @Valid WatchlistGroupMoveReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        watchlistService.moveGroup(reqVO, userId);
        return ResponseDTO.success();
    }

    @Operation(summary = "获取分组下的股票列表")
    @GetMapping("/stock/list")
    public ResponseDTO<List<WatchlistStockVO>> getStocksByGroupId(@RequestParam Long groupId) {
        Long userId = UserContext.requireCurrentUserId();
        return ResponseDTO.success(watchlistService.getStocksByGroupId(groupId, userId));
    }

    @Operation(summary = "添加股票到自选")
    @PostMapping("/stock/add")
    public ResponseDTO<Void> addStock(@RequestBody @Valid WatchlistStockReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        watchlistService.addStockToWatchlist(reqVO, userId);
        return ResponseDTO.success();
    }

    @Operation(summary = "从自选中移除股票")
    @PostMapping("/stock/remove")
    public ResponseDTO<Void> removeStock(@RequestBody @Valid WatchlistStockReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        watchlistService.removeStockFromWatchlist(reqVO.getGroupId(), reqVO.getStockCode(), userId);
        return ResponseDTO.success();
    }

    @Operation(summary = "股票重排序")
    @PostMapping("/stock/reorder")
    public ResponseDTO<Void> reorderStock(@RequestBody @Valid WatchlistStockReorderReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        watchlistService.updateStockSort(reqVO, userId);
        return ResponseDTO.success();
    }

    @Operation(summary = "股票移动操作(增量排序)")
    @PostMapping("/stock/move")
    public ResponseDTO<Void> moveStock(@RequestBody @Valid WatchlistStockMoveReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        watchlistService.moveStock(reqVO, userId);
        return ResponseDTO.success();
    }

    @Operation(summary = "修改股票所属分组")
    @PostMapping("/stock/moveGroup")
    public ResponseDTO<Void> moveStockGroup(@RequestBody @Valid WatchlistStockMoveGroupReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        watchlistService.moveStockToGroup(reqVO, userId);
        return ResponseDTO.success();
    }

}
