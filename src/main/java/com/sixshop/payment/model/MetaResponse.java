package com.sixshop.payment.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetaResponse {
    private Long totalCount;

    public MetaResponse() {
        this.totalCount = 0L;
    }

    public MetaResponse(Long count) {
        this.totalCount = count;
    }
}
