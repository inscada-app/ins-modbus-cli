package com.inscada.modbus.client.model;

import java.util.Collections;
import java.util.List;

public class Result<T> {

    private final T val;
    private final List<String> errors;

    public static Result<Void> createSuccessResult() {
        return new Result<>(null);
    }

    public static <T> Result<T> createSuccessResult(T result) {
        return new Result<>(result);
    }

    public static <T> Result<T> createErrorResult(List<String> errors) {
        return new Result<>(null, errors);
    }

    public static <T> Result<T> createErrorResult(T result, List<String> errors) {
        return new Result<>(result, errors);
    }

    private Result(T val) {
        this.val = val;
        this.errors = Collections.emptyList();
    }

    private Result(T val, List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException("Cannot be null or empty");
        }
        this.val = val;
        this.errors = Collections.unmodifiableList(errors);
    }

    public boolean hasError() {
        return !this.errors.isEmpty();
    }

    public T getVal() {
        return val;
    }

    public List<String> getErrors() {
        return errors;
    }
}
