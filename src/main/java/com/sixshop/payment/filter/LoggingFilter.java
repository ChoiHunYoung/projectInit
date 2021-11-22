package com.sixshop.payment.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class LoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        Map<String, String> requestMap = this.getTypesafeRequestMap(requestWrapper);

        long startTime = System.currentTimeMillis();

        filterChain.doFilter(requestWrapper, responseWrapper);

        long elapsed = System.currentTimeMillis() - startTime;
        MDC.put("ELAPSED", String.valueOf(elapsed));

        if (requestMap.get("password") != null) {
            requestMap.put("password", "********");
        }

        final StringBuilder logMessage = new StringBuilder("[")
                .append(elapsed).append("ms] ");

        MDC.put("STATUS_CODE", String.valueOf(responseWrapper.getStatus()));

        if (StringUtils.hasLength(requestWrapper.getMethod())) {
            MDC.put("HTTP_METHOD", requestWrapper.getMethod());
            logMessage.append("[HTTP METHOD:")
                    .append(requestWrapper.getMethod())
                    .append("] ");
        }

        if (StringUtils.hasLength(requestWrapper.getRequestURI())) {
            MDC.put("REQUEST_URI", requestWrapper.getRequestURI());
            logMessage.append("[REQUEST URI:")
                    .append(requestWrapper.getRequestURI())
                    .append("] ");
        }

        if (requestMap.size() > 0) {
            MDC.put("REQUEST_PARAM", requestMap.toString());
            logMessage.append("[REQUEST PARAMETERS:")
                    .append(requestMap)
                    .append("] ");
        }

        if (StringUtils.hasLength(new String(requestWrapper.getContentAsByteArray(), request.getCharacterEncoding()))
                && !(requestWrapper.getHeader("content-type") != null && requestWrapper.getHeader("content-type").contains("multipart"))) {

            String reqBody = new String(requestWrapper.getContentAsByteArray(), request.getCharacterEncoding());
            try {
                if (reqBody.contains("\"password\"")) {
                    String preB = reqBody.substring(0, reqBody.indexOf("\"password\":") + 11);
                    reqBody = reqBody.substring(reqBody.indexOf("\"password\":") + 11);

                    String middleB = reqBody.substring(0, reqBody.indexOf("\"") + 1);
                    reqBody = reqBody.substring(middleB.length());

                    String postB = reqBody.substring(reqBody.indexOf("\""));
                    reqBody = preB + middleB + "****" + postB;
                } else if (reqBody.contains("password")) {
                    String preB = reqBody.substring(0, reqBody.indexOf("password") + 9);
                    reqBody = reqBody.substring(reqBody.indexOf("password") + 9);

                    String postB = reqBody.substring(reqBody.indexOf("&"));
                    reqBody = preB + "****" + postB;
                }
                MDC.put("REQUEST_BODY", reqBody);
                logMessage.append("[REQUEST BODY:")
                        .append(reqBody)
                        .append("] ");
            } catch (Exception e) {
                log.error("password hidden fail");
            }
        }

        if (StringUtils.hasLength(new String(responseWrapper.getContentAsByteArray(), response.getCharacterEncoding()))) {
            String responseBody = new String(responseWrapper.getContentAsByteArray(), response.getCharacterEncoding());
            MDC.put("RESPONSE_BODY", responseBody);
            logMessage.append("[RESPONSE:")
                    .append(responseBody)
                    .append("]");
        }

        log.info(logMessage.toString());

        responseWrapper.copyBodyToResponse();

        MDC.clear();
    }

    private Map<String, String> getTypesafeRequestMap(HttpServletRequest request) {
        Map<String, String> typesafeRequestMap = new HashMap<String, String>();
        Enumeration<?> requestParamNames = request.getParameterNames();
        while (requestParamNames.hasMoreElements()) {
            String requestParamName = (String) requestParamNames.nextElement();
            String requestParamValue = request.getParameter(requestParamName);
            typesafeRequestMap.put(requestParamName, requestParamValue);
        }
        return typesafeRequestMap;
    }

    @Override
    public void destroy() {

    }
}
