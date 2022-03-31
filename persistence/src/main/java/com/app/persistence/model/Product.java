package com.app.persistence.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Builder
@Getter
@ToString
public class Product {

    private final String name;
    private final Category category;
    private final BigDecimal price;

    public boolean hasCategory(Category category) {
        return this.category == category;
    }

}
