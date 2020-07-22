package com.example.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Setter
@Getter
public class Cart implements Serializable {
    Integer num;
    BigDecimal price;
    Long id;
}
