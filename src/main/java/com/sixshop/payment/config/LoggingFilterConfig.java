package com.sixshop.payment.config;

import com.sixshop.payment.filter.LoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingFilterConfig {
    @Bean
    public FilterRegistrationBean<LoggingFilter> loggingFilter() {
        FilterRegistrationBean<LoggingFilter> registrationBean = new FilterRegistrationBean<>();

        LoggingFilter loggingFilter = new LoggingFilter();
        registrationBean.setFilter(loggingFilter);
        return registrationBean;
    }
}
