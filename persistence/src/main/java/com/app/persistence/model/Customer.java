package com.app.persistence.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Builder
@Getter
@ToString
public class Customer {

    private final String name;
    private final String surname;
    private final Integer age;
    private final BigDecimal cash;

}
