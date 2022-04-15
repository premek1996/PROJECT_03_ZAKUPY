package com.app.persistence.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Builder
@ToString
public class Product {

    final String name;
    final Category category;
    final BigDecimal price;

    public boolean hasCategory(Category category) {
        return this.category == category;
    }
}
