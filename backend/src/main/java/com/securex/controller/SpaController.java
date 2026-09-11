package com.securex.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {
        "/",
        "/login",
        "/register",
        "/dashboard",
        "/files",
        "/shares",
        "/admin",
        "/share/**"
    })
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
