package com.brotherc.aquant.integration.nf.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NFFundAnnouncementPage {

    private int totalPages;

    private List<NFFundAnnouncement> content = new ArrayList<>();

}
