package com.app.persistence.converter;

import com.app.persistence.model.CustomerWithProducts;

import java.util.List;

public class CustomerWithProductsConverter extends JsonConverter<List<CustomerWithProducts>> {

    public CustomerWithProductsConverter(String jsonFilename) {
        super(jsonFilename);
    }

}
