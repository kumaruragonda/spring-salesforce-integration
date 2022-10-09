package com.spring.springsalesforceintegration.impl;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalesforceService {

    private String token;
    private static final String REFRESH_TOKEN = "http://localhost:8080/refreshtoken";

    private final RestTemplate restTemplate;

    private final SalesforceConfiguration salesforceConfiguration;

    private final KeyRetriever keyRetriever;

    public String createLead() {
        SalesforceToken token = getToken();

        var lead = new Lead("CompanyName", "Surname");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());

        var resp = restTemplate.postForObject(token.getInstanceUrl() + "/services/data/v39.0/sobjects/Lead", new HttpEntity<>(lead, headers), String.class);
        return resp;
    }

    public SalesforceToken getToken() {
        String jwt = createJwtToken();
        return sentRequest(jwt);
    }

    private String createJwtToken() {
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(salesforceConfiguration.getExpirationInMinutes());
        return Jwts.builder()
                .setIssuer(salesforceConfiguration.getClientId())
                .setAudience(salesforceConfiguration.getAudience())
                .setSubject(salesforceConfiguration.getSubject())
                .setExpiration(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(keyRetriever.getKey())
                .compact();
    }

    public String getRefreshToken() {
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(salesforceConfiguration.getRefreshTokenInMinutes());
        return Jwts.builder()
                .setIssuer(salesforceConfiguration.getClientId())
                .setAudience(salesforceConfiguration.getAudience())
                .setSubject(salesforceConfiguration.getSubject())
                .setExpiration(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(keyRetriever.getKey())
                .compact();
    }

    SalesforceToken sentRequest(String jwt) {
        String url = UriComponentsBuilder.fromUriString(salesforceConfiguration.getAudience()).path("/services/oauth2/token")
                .queryParam("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                .queryParam("assertion", jwt)
                .toUriString();

        HttpHeaders headers = getHeaders();
        try {
          return restTemplate.postForObject(url, new HttpEntity<>(null, headers), SalesforceToken.class);
        } catch (Exception ex) {
            // check if exception is due to ExpiredJwtException
            if (ex.getMessage().contains("io.jsonwebtoken.ExpiredJwtException")) {
                // Refresh Token
                refreshToken();
                // try again with refresh token
                return restTemplate.postForObject(url, new HttpEntity<>(null, headers), SalesforceToken.class);

            } else {
                System.out.println(ex);
            }

        }
        return  null;
    }

    private void refreshToken() {
        String token  = "Bearer " + getRefreshToken();
        HttpHeaders headers = getHeaders();
        headers.set("Authorization", token);
        headers.set("isRefreshToken", "true");

    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

}
