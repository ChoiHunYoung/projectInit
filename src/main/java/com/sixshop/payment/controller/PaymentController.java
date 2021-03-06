package com.sixshop.payment.controller;

import com.sixshop.payment.model.dto.PaymentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PaymentController {
    @GetMapping(value = "/payments/request/{pgType}/{customerNo}/{orderNo}")
    public void paymentRequest(@RequestHeader(name = "customer-no") Long customerNo,
                               @RequestHeader(name = "site-link") String siteLink,
                               @RequestBody PaymentDto.Request request) {
    }
}
