package com.brotherc.aquant.model.dto.stockquote;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundHoldingSyncWindow {

    private String requestDate;
    private Integer reportYear;
    private Integer reportQuarter;

}
