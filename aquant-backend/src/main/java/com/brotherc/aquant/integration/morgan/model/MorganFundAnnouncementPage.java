package com.brotherc.aquant.integration.morgan.model;

import com.brotherc.aquant.fund.model.dto.FundPurchaseLimitAnnouncement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MorganFundAnnouncementPage {

    private int totalPages;

    private List<FundPurchaseLimitAnnouncement> content = new ArrayList<>();

}
