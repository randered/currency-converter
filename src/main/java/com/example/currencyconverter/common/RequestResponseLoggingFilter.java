package com.example.currencyconverter.common;

import com.example.currencyconverter.util.Constants;
import com.example.currencyconverter.util.LogMessages;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        if (query != null) {
            path = path + "?" + query;
        }
        String clientId = resolveClientId(request);

        log.info(LogMessages.REQUEST_ENTRY, method, path, clientId);
        try {
            chain.doFilter(request, response);
        } finally {
            log.info(LogMessages.REQUEST_EXIT, method, path, response.getStatus(),
                    System.currentTimeMillis() - start);
        }
    }

    private String resolveClientId(HttpServletRequest request) {
        String fromHeader = request.getHeader(Constants.HEADER_CLIENT_ID);
        if (fromHeader != null) {
            return fromHeader;
        }
        String[] segments = request.getRequestURI().split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if ("clients".equals(segments[i])) {
                return segments[i + 1];
            }
        }
        return request.getParameter("clientId");
    }
}
