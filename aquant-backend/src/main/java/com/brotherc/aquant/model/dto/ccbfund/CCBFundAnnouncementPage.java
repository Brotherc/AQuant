package com.brotherc.aquant.model.dto.ccbfund;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CCBFundAnnouncementPage {

    /**
     * 当前页公告列表
     */
    private List<CCBFundAnnouncement> content = new ArrayList<>();

    /**
     * 建信官网公告接口返回的总页数，用于控制分页扫描范围
     */
    private Integer totalPages = 0;

}
