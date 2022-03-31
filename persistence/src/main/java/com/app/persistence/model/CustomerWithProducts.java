package com.app.persistence.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Builder
@Getter
@ToString
public class CustomerWithProducts {

    private final Customer customer;
    private final List<Product> products;

}
