package com.repup.web;

import com.repup.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds values every page layout needs, such as the canonical URL, to the model.
 * It also registers {@link AppProperties}, so controller tests ({@code @WebMvcTest}) get it too.
 */
@ControllerAdvice
@EnableConfigurationProperties(AppProperties.class)
public class SeoModelAdvice {

    private final AppProperties appProperties;

    public SeoModelAdvice(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @ModelAttribute("baseUrl")
    public String baseUrl() {
        return appProperties.baseUrl();
    }

    /** Base URL plus the request path; query parameters are dropped on purpose. */
    @ModelAttribute("canonicalUrl")
    public String canonicalUrl(HttpServletRequest request) {
        return appProperties.baseUrl() + request.getRequestURI();
    }
}
