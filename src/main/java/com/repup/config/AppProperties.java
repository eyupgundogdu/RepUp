package com.repup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under the {@code app.*} prefix in application.yml.
 *
 * @param baseUrl public address of the site without a trailing slash, used for canonical URLs
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String baseUrl) {
}
