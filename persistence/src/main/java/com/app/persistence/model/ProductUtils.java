package com.app.persistence.model;

import java.math.BigDecimal;
import java.util.function.Function;

public interface ProductUtils {
    Function<Product, BigDecimal> toPrice = product -> product.price;
    Function<Product, Category> toCategory = product -> product.category;
}
