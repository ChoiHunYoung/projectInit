package com.sixshop.payment.exception;

import com.sixshop.payment.model.ApiResponse;
import com.sixshop.payment.model.EmptyObject;
import com.sixshop.payment.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionController {
    private void printStackTraceLogger(String errorStr, Exception e) {
        log.error(errorStr + e.getMessage());
    }

    @ExceptionHandler(value = CustomException.class)
    public ResponseEntity<ApiResponse<EmptyObject>> customException(CustomException e) {
        printStackTraceLogger("[CustomException] ", e);
        return ResponseEntity.status(e.getType().getHttpStatusCode()).body(new ApiResponse<>(new ErrorResponse(e.getType().getCode(), e.getType().getValue())));
    }
}
