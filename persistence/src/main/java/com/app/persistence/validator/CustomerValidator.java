package com.app.persistence.validator;

import com.app.persistence.model.Customer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class CustomerValidator implements Validator<Customer> {

    private final Map<String, String> errors;

    public CustomerValidator() {
        this.errors = new HashMap<>();
    }

    @Override
    public Map<String, String> validate(Customer customer) {
        if (customer == null) {
            errors.put("customer", "object is null");
            return errors;
        }
        if (customer.getAge() < 18) {
            errors.put("age", "has to be >= 18");
        }
        validateCash(customer.getCash());
        return errors;
    }

    private void validateCash(BigDecimal cash) {
        if (cash == null) {
            errors.put("cash", "object is null");
        } else if (cash.compareTo(BigDecimal.ZERO) < 0) {
            errors.put("cash", "has to be >= 0");
        }
    }

}
