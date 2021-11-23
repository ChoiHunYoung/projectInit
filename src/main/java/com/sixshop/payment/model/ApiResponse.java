package com.sixshop.payment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.MDC;

@Data
@SuppressWarnings("unchecked")
@AllArgsConstructor
public class ApiResponse<T> {
    private MetaResponse meta;
    private T data;
    private ErrorResponse error;
    private String traceId;
    private String globalTraceId;

    public ApiResponse() {
        this.meta = new MetaResponse();
        this.data = (T) EmptyObject.OBJECT;
        this.error = new ErrorResponse();
        setTraceId();
    }

    public ApiResponse(MetaResponse meta) {
        this.meta = meta;
        this.data = (T) EmptyObject.LIST;
        this.error = new ErrorResponse();
        setTraceId();
    }

    public ApiResponse(T data) {
        this.meta = new MetaResponse();

        if (data == null)  this.data = (T) EmptyObject.OBJECT;
        else this.data = data;

        this.error = new ErrorResponse();
        setTraceId();
    }

    public ApiResponse(ErrorResponse error) {
        this.meta = new MetaResponse();
        this.data = (T) EmptyObject.OBJECT;
        this.error = error;
        setTraceId();
    }

    public ApiResponse(Long totalCount, T data) {
        this.meta = new MetaResponse(totalCount);

        if (data == null) {
            this.data = (T) EmptyObject.LIST;
        } else {
            this.data = data;
        }

        this.error = new ErrorResponse();
        setTraceId();
    }

    private void setTraceId() {
        try {
            this.traceId = MDC.get("LOCAL_TRACE_ID").replaceAll("\\[", "").replaceAll("]", "").trim();
            this.globalTraceId = MDC.get("GLOBAL_TRACE_ID").replaceAll("\\[", "").replaceAll("]", "").trim();
        } catch (NullPointerException e) {
            this.traceId = "";
        }
    }
}
