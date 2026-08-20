package com.brotherc.aquant.stock.model.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundHoldingSyncWindow {

    private String requestDate;
    private Integer reportYear;
    private Integer reportQuarter;

}
