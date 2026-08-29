package com.brotherc.aquant.integration.htf.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HTFFundAnnouncement {

    private String announcementId;

    private LocalDate announcementDate;

    private String title;

    private String detailUrl;

    private String attachmentUrl;

}
