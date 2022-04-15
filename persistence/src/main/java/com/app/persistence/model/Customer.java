package com.app.persistence.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Builder
@Getter
@ToString
public class Customer {
    final String name;
    final String surname;
    final Integer age;
    final BigDecimal cash;
}
