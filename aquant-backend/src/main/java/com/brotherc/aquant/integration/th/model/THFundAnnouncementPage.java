package com.brotherc.aquant.integration.th.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class THFundAnnouncementPage {

    private int totalPages;

    private List<THFundAnnouncement> content = new ArrayList<>();

}
