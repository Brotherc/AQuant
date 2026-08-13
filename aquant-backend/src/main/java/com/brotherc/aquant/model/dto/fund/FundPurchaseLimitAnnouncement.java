package com.brotherc.aquant.model.dto.fund;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FundPurchaseLimitAnnouncement {

    /**
     * 来源站点内的公告唯一编号，与来源编码共同作为去重键
     */
    private String announcementId;

    /**
     * 公告发布日期
     */
    private LocalDate announcementDate;

    /**
     * 公告标题
     */
    private String title;

}
