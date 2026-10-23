package com.example.reliability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionController {

    @Value("${app.version:1.0.0}")
    private String version;

    @GetMapping("/version")
    public String version() {
        return version;
    }
}