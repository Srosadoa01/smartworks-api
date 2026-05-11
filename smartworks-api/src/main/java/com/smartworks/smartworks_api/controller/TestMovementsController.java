package com.smartworks.smartworks_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestMovementsController {

    @GetMapping("/movements-test")
    public String test() {
        return "OK MOVEMENTS";
    }
}
