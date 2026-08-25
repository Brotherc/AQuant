package com.brotherc.aquant.integration.th.model;

import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitAnnouncement;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class THFundAnnouncement extends FundPurchaseLimitAnnouncement {

    private String detailUrl;

    private String attachmentUrl;

}
