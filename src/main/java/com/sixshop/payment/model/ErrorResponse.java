package com.sixshop.payment.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sixshop.payment.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.slf4j.MDC;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private Integer status;
    private String error;
    private String message;
    private LocalDateTime timeStamp;

    public ErrorResponse(int status, String message) {
        MDC.put(Constants.STATUS_CODE, String.valueOf(status));
        this.status = status;
        this.message = message;
    }
}
