package com.jettch.sisgev.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessExpirationMinutes = 30;
    private long refreshExpirationDays = 7;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getAccessExpirationMinutes() { return accessExpirationMinutes; }
    public void setAccessExpirationMinutes(long accessExpirationMinutes) { this.accessExpirationMinutes = accessExpirationMinutes; }

    public long getRefreshExpirationDays() { return refreshExpirationDays; }
    public void setRefreshExpirationDays(long refreshExpirationDays) { this.refreshExpirationDays = refreshExpirationDays; }
}
