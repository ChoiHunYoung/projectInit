package com.sixshop.payment.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CustomException extends RuntimeException {
    private ExceptionType type;

    public CustomException(ExceptionType type) {
        super(type.getValue());
        this.type = type;
    }

    public ExceptionType getType() {
        return type;
    }
}
