package com.platform.order.config;
import jakarta.servlet.*; import jakarta.servlet.http.*; import java.io.IOException; import java.util.*; import org.slf4j.MDC; import org.springframework.stereotype.Component; import org.springframework.web.filter.OncePerRequestFilter;
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    private static final String HEADER="X-Correlation-Id";
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String id= Optional.ofNullable(request.getHeader(HEADER)).orElse(UUID.randomUUID().toString());
        MDC.put("correlationId", id); response.setHeader(HEADER, id);
        try { filterChain.doFilter(request,response);} finally { MDC.clear(); }
    }
}

