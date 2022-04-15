package com.app.persistence.model;

import java.util.function.Function;

public interface CustomerUtils {
    Function<Customer, Integer> toAge = customer -> customer.age;
}
