package com.brotherc.aquant.watchlist.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "自选分组信息")
public class WatchlistGroupVO {

    @Schema(description = "分组 ID")
    private Long id;

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "分组类型: STOCK-股票自选组, FUND-基金自选组")
    private String type;

    @Schema(description = "排序号")
    private Integer sortNo;

    @Schema(description = "分组下的股票/基金数量")
    private Integer count;

    @Schema(description = "分组下的股票列表")
    private List<WatchlistStockVO> stocks;

}
