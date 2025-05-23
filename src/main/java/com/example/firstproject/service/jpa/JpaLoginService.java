package com.example.firstproject.service.jpa;

import com.example.firstproject.common.constant.SessionConst;
import com.example.firstproject.domain.Role;
import com.example.firstproject.domain.dto.member.LoginMember;
import com.example.firstproject.domain.jpa.MemberEntity;
import com.example.firstproject.repository.member.JpaMemberRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class JpaLoginService {
    private final JpaMemberRepository memberRepository;

    /**
     * 로그인 아이디로 회원을 조회하는 메서드
     */
    public Optional<MemberEntity> findLoginMemberById(LoginMember loginMember) {
        return memberRepository.findByLoginId(loginMember.getId());
    }

    /**
     * 로그인 처리하는 메서드
     */
    public String processLogin(LoginMember loginMember, BindingResult bindingResult,
                             HttpServletRequest request, HttpServletResponse response) {
        Optional<MemberEntity> optionalFindMember = findLoginMemberById(loginMember);

        if(optionalFindMember.isEmpty() || !loginMember.getPwd().equals(optionalFindMember.get().getPassword())) {
            bindingResult.reject("loginFail");
            return null;
        }

        processRememberIdCookie(loginMember, response);
        createSession(loginMember, request);

        return  optionalFindMember.get().getRole() == Role.ADMIN ? "admin" : "";
    }

    /**
     * 로그아웃 처리하는 메서드
     */
    public void processLogout(HttpServletRequest request) {
        /*
         * getSession(boolean create)
         * create 파라미터의 기본값은 true
         * 참인 경우 세션이 있으면 기존 세션을 반환하고 반대로 세션이 없으면 새로운 세션을 생성해서 반환
         * 거짓인 경우 세션이 없으면 기존 세션을 반환하고 반대로 세션이 없으면 새로운 세션을 생성하지 않고 널값을 반환
         */
        HttpSession session = request.getSession(false);

        log.info("session={}", session.getAttribute(SessionConst.LOGIN_MEMBER));

        session.invalidate();

        log.info("LOGOUT SUCCESS");
    }


    /**
     * 로그인 아이디 기억 기능을 처리하는 메서드
     */
    public LoginMember processRememberIdLoginMember(String memberId) {
        if (memberId == null) {
            return new LoginMember();
        } else {
            LoginMember loginMember = new LoginMember(memberId);
            loginMember.setRememberId(true);
            return loginMember;
        }
    }

    /**
     * 로그인 성공시 세션을 생성하는 메서드
     */
    public void createSession(LoginMember loginMember, HttpServletRequest request) {
        Optional<MemberEntity> optionalFindMember = findLoginMemberById(loginMember);

        // 세션 생성
        HttpSession session = request.getSession();

        // 세션에 정보 저장하기
        session.setAttribute(SessionConst.LOGIN_MEMBER, optionalFindMember.orElseThrow(RuntimeException::new));
        session.setAttribute("status", true); // 로그인 여부 확인하기 위한 작업

        log.info("create session={}", session.getAttribute(SessionConst.LOGIN_MEMBER));
    }

    /**
     * 로그인 아디 기억 기능을 처리하는 메서드(쿠키 사용)
     */
    public void processRememberIdCookie(LoginMember loginMember, HttpServletResponse response) {
        if(loginMember.isRememberId()) {
            creatCookie(response, loginMember);
        } else {
            deleteCookie(response, loginMember);
        }
    }

    /**
     * 쿠키를 생성하는 메서드
     */
    private void creatCookie(HttpServletResponse response, LoginMember loginMember) {
        Cookie cookie = new Cookie("memberId", loginMember.getId());
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    /**
     * 쿠키를 삭제하는 메서드
     */
    private void deleteCookie(HttpServletResponse response, LoginMember loginMember) {
        Cookie cookie = new Cookie("memberId", loginMember.getId());

        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }
}
