package com.example.firstproject.controller;

import com.example.firstproject.domain.dto.member.LoginMember;
import com.example.firstproject.service.jdbc.LoginService;
import com.example.firstproject.service.jpa.JpaLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Controller
@Slf4j
@RequiredArgsConstructor
public class LoginController {
    /* JDBC */
//    private final LoginService loginService;

    /* JPA */
    private final JpaLoginService loginService;

    /**
     * 로그인 페이지 제공
     * 추후 로그인 정보를 주고 받기 위해서 비어있는 회원 객체를 저장소(Model)에 저장해서 폼으로 넘겨준다.
     */
    @GetMapping("/login")
    public String login(Model model,
                        @CookieValue(name = "memberId", required = false) String memberId,
                        @RequestParam(defaultValue = "/") String redirectURL) {
        LoginMember loginMember = loginService.processRememberIdLoginMember(memberId);

        model.addAttribute("loginMember", loginMember);
        model.addAttribute("redirectURL", redirectURL);

        return "member/loginForm";
    }

    /**
     * 로그인 처리
     * 1. 로그인 화면에서 사용자가 입력한 정보를 가져오기 (@ModelAttribute 사용)
     * 2. 모든 회원 정보가 담겨있는 저장소에 사용자가 입력한 정보가 저장되어 있는지 확인하기
     * 3. 저장소에 저장이 되어있지 않은 회원인 경우 로그인 화면을 다시 보여주기
     * 4. 저장소에 저장이 되어있다면 로그인 처리(세션 생성, 홈 화면으로 이동)하기
     */
    @PostMapping("/login")
    public String login(@Validated @ModelAttribute("loginMember") LoginMember loginMember, BindingResult bindingResult,
                        @RequestParam(defaultValue = "/") String redirectURL, // 로그인이 필요한 경로로 요청시 해당 경로 저장
                        HttpServletRequest request, HttpServletResponse response) {
        String path = loginService.processLogin(loginMember, bindingResult, request, response);

        if(bindingResult.hasErrors() || path == null) {
            loginMember.setId("");
            loginMember.setRememberId(false);

            return "member/loginForm";
        }

        if("/admin".equals(path)) {
            return "redirect:" + path;
        }

        return "redirect:" + redirectURL;
    }

    /**
     * 로그아웃 요청을 처리
     * 1. HTTP Session 가져오기
     * 2. 가져온 세션이 널인지 아닌지 확인
     * 3. 널이 아니면 세션의 invalidate 메서드를 사용하여 세션을 종료
     */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        loginService.processLogout(request);

        return "redirect:/";
    }
}