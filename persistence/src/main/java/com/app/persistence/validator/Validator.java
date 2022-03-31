package com.app.persistence.validator;

import com.app.persistence.validator.exception.ValidatorException;

import java.util.Map;
import java.util.stream.Collectors;

public interface Validator<T> {

    Map<String, String> validate(T t);

    static <T> void validate(T t, Validator<T> validator) {
        Map<String, String> errors = validator.validate(t);
        if (!errors.isEmpty()) {
            throw new ValidatorException(joinErrors(errors));
        }
    }

    private static String joinErrors(Map<String, String> errors) {
        return errors.entrySet()
                .stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
    }

}

