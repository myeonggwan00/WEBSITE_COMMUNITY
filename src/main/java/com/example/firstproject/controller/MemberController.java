package com.example.firstproject.controller;

import com.example.firstproject.common.constant.SessionConst;
import com.example.firstproject.domain.dto.member.UpdateMemberDto;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.domain.jpa.MemberEntity;
import com.example.firstproject.service.jdbc.MemberService;
import com.example.firstproject.service.jpa.JpaMemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
@RequiredArgsConstructor
public class MemberController {
    /* JDBC */
//    private final MemberService memberService;

    /* JPA */
    private final JpaMemberService memberService;

    /**
     * 회원 수정 페이지 제공
     */
    @GetMapping("/members/edit")
    public String edit(Model model, @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) MemberEntity loginMember) {
        // 회원 수정 화면에 기존 회원 정보를 보여주기
        model.addAttribute("member", memberService.findMember(loginMember));

        return "member/editProfile";
    }

    /**
     * 회원 수정 요청을 처리
     */
    @PostMapping("/members/edit")
    public String member(@ModelAttribute("member") UpdateMemberDto member, BindingResult bindingResult,
                         @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) MemberEntity loginMember,
                         HttpServletRequest request, HttpServletResponse response) {

        memberService.editMember(loginMember.getId(), member, bindingResult, request, response);

        if(bindingResult.hasErrors()) {
            return "member/editProfile";
        }

        return "redirect:/";
    }
}
