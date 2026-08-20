package com.brotherc.aquant.integration.ccb.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CCBFundAnnouncementParseResult {

    private boolean matchedTargetFund;

    private List<CCBFundPurchaseRule> rules = new ArrayList<>();

}
