package com.sixshop.payment.exception;

public enum ExceptionType {
    /**
     *  BadRequest (1000 ~ 1999)
     *  1000 ~ 1009 : 계정 권한
     *  1010 ~ 1019 : DB 관련
     *  1900 ~ 1999 : Custom
     */
    USER_ID_NOT_FOUND(400, 1000, "회원정보를 찾을 수 없습니다.(Token)"),

    /**
     *  Unauthorized (2000 ~ 2999)
     *  2000 ~ 2999 : Custom
     */

    /**
     *  Not Found (3000 ~ 3999)
     *  3000 ~ 3999 : Custom
     */

    /**
     *  Server (9000 ~ 9999)
     */
    UNKNOWN(500, 9000, "알 수 없는 에러가 발생 했습니다.")
    ;

    private Integer httpStatusCode;
    private Integer code;
    private String value;

    ExceptionType(Integer httpStatusCode, Integer code, String value) {
        this.httpStatusCode = httpStatusCode;
        this.code = code;
        this.value = value;
    }

    public String getKey() {
        return name();
    }

    public String getValue() {
        return this.value;
    }

    public Integer getCode() {
        return this.code;
    }

    public Integer getHttpStatusCode() {
        return this.httpStatusCode;
    }
}
