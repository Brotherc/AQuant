package com.brotherc.aquant.model.dto.ccbfund;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CCBFundAnnouncementParseResult {

    private boolean matchedTargetFund;

    private List<CCBFundPurchaseRule> rules = new ArrayList<>();

}
