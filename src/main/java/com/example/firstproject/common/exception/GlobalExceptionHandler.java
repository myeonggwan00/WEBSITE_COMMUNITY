package com.example.firstproject.common.exception;

import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public String handleMethodNotSupported(HttpRequestMethodNotSupportedException e, Model model) {
        model.addAttribute("message", "잘못된 요청입니다. 요청 방식이 지원되지 않습니다.");
        return "errorpage/customError";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleBadRequest(IllegalArgumentException e, Model model) {
        model.addAttribute("message", "잘못된 요청입니다. 요청 방식이 지원되지 않습니다.");
        return "errorpage/customError";
    }
}
