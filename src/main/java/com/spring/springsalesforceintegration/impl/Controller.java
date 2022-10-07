package com.spring.springsalesforceintegration.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class Controller {

    private final SalesforceService salesforceService;

    @GetMapping("test")
    String createLead() {
        return salesforceService.createLead();
    }
}
