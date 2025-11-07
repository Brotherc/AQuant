package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "stock_sync")
public class StockSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Long value;

}
