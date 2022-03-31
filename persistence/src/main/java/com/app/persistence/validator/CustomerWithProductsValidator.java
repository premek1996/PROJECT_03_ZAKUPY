package com.app.persistence.validator;

import com.app.persistence.model.CustomerWithProducts;

import java.util.HashMap;
import java.util.Map;

public class CustomerWithProductsValidator implements Validator<CustomerWithProducts> {

    private final Map<String, String> errors;

    public CustomerWithProductsValidator() {
        this.errors = new HashMap<>();
    }

    @Override
    public Map<String, String> validate(CustomerWithProducts customerWithProducts) {
        if (customerWithProducts == null) {
            errors.put("customerWithProducts", "object is null");
            return errors;
        }
        errors.putAll(new CustomerValidator().validate(customerWithProducts.getCustomer()));
        ProductValidator productValidator = new ProductValidator();
        customerWithProducts.getProducts().forEach(product -> errors.putAll(productValidator.validate(product)));
        return errors;
    }

}
