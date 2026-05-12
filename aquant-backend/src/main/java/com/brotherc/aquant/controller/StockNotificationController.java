package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.notification.StockNotificationDeleteReqVO;
import com.brotherc.aquant.model.vo.notification.StockNotificationReqVO;
import com.brotherc.aquant.model.vo.notification.StockNotificationVO;
import com.brotherc.aquant.service.StockNotificationService;
import com.brotherc.aquant.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "股票提醒管理")
@RestController
@RequestMapping("/stock/notification")
@RequiredArgsConstructor
public class StockNotificationController {

    private final StockNotificationService notificationService;

    @Operation(summary = "获取股票的提醒设置")
    @GetMapping("/list")
    public ResponseDTO<List<StockNotificationVO>> list(@RequestParam String stockCode) {
        Long userId = UserContext.requireCurrentUserId();
        return ResponseDTO.success(notificationService.getByUserIdAndStockCode(userId, stockCode));
    }

    @Operation(summary = "保存提醒设置")
    @PostMapping("/save")
    public ResponseDTO<Void> save(@RequestBody @Valid StockNotificationReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        notificationService.save(reqVO, userId);
        return ResponseDTO.success();
    }

    @Operation(summary = "删除提醒设置")
    @PostMapping("/delete")
    public ResponseDTO<Void> delete(@RequestBody @Valid StockNotificationDeleteReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        notificationService.delete(reqVO.getId(), userId);
        return ResponseDTO.success();
    }

}
