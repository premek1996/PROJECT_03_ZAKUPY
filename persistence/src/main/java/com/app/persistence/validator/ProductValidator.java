package com.app.persistence.validator;

import com.app.persistence.model.Product;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ProductValidator implements Validator<Product> {

    private final Map<String, String> errors;

    public ProductValidator() {
        this.errors = new HashMap<>();
    }

    @Override
    public Map<String, String> validate(Product product) {
        if (product == null) {
            errors.put("product", "object is null");
            return errors;
        }
        validatePrice(product.getPrice());
        return errors;
    }

    private void validatePrice(BigDecimal price) {
        if (price == null) {
            errors.put("price", "object is null");
        } else if (price.compareTo(BigDecimal.ZERO) < 0) {
            errors.put("price", "has to be >= 0");
        }
    }

}
