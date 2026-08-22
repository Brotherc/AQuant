package com.brotherc.aquant.integration.efund.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EFundAnnouncementPage {

    private int totalPages;

    private List<EFundAnnouncement> content = new ArrayList<>();

}
