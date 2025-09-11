package com.ddtt.configs;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.MutableHttpResponse;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Filter("/api/**")
public class Logging implements HttpServerFilter{
    private static final Logger LOG = LoggerFactory.getLogger(Logging.class);

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        LOG.info("API called: {} {}", request.getMethod(), request.getUri());
        return chain.proceed(request);
    }
}
