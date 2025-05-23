package com.example.firstproject.controller;

import com.example.firstproject.common.constant.SessionConst;
import com.example.firstproject.domain.dto.member.MemberDto;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.domain.jpa.MemberEntity;
import com.example.firstproject.service.jdbc.MemberService;
import com.example.firstproject.service.jpa.JpaMemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RegisterController {
    /* JDBC */
//    private final MemberService memberService;

    /* JPA */
    private final JpaMemberService memberService;

    /**
     * 회원가입 페이지 제공
     * 사용자가 회원가입할 때 입력한 정보를 처리하기 위해서 비어있는 회원 객체를 저장소(Model)에 저장해서 폼에 넘겨준다.
     */
    @GetMapping("/members/new")
    public String register(Model model) {

        model.addAttribute("member", new MemberDto());

        return "member/registerForm";
    }

    /**
     * 회원가입 처리
     * 1. 회원가입 화면에서 사용자가 입력한 정보를 얻기 (@ModelAttribute 사용)
     */
    @PostMapping("/members/register")
    public String register(@Validated @ModelAttribute(name = "member") MemberDto member, BindingResult bindingResult) {
        memberService.checkMember(member, bindingResult);

        if(bindingResult.hasErrors()) {
            return "member/registerForm";
        }

        memberService.saveMember(member);

        return "home";
    }

    /**
     * 회원탈퇴 처리
     */
    @GetMapping("/members/delete")
    public String deleteAccount(HttpServletRequest request,
                                @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) MemberEntity member) {
        memberService.deleteMember(member, request);

        return "redirect:/";
    }
}
