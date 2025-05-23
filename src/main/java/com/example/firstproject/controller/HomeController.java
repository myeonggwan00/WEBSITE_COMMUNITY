package com.example.firstproject.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@Slf4j
@RequiredArgsConstructor
public class HomeController {
    /**
     * 홈페이지 제공
     */
    @GetMapping("/")
    public String home() {
        return "home";
    }

}
