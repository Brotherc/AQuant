package com.brotherc.aquant.portfolio.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BrokerAccountVO {

    private Long id;
    private Long portfolioId;
    private String accountName;
    private String brokerCode;
    private String brokerName;
    private String accountNoMasked;
    private String accountType;
    private String syncMode;
    private String status;
    private LocalDateTime createTime;
}
