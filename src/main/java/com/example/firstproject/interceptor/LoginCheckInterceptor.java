package com.example.firstproject.interceptor;

import com.example.firstproject.common.constant.SessionConst;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // redirectURL 검증
        if (requestURI == null || !requestURI.matches("^/([a-zA-Z0-9_\\-]+(/)?)*$")) {
            requestURI = "/";
        }

        if(requestURI.startsWith("/admin")){
            requestURI = "/";
        }


        // GET /posts/{숫자} 형식만 예외 처리
        // ^은 문자열을 시작을 의미, $은 문자열 끝을 의미, \d+은 숫자 하나 이상을 의미(자바에서는 \\가 \로 인식)
        if ("GET".equalsIgnoreCase(method) && requestURI.matches("^/posts/\\d+$")) {
            return true;
        }

        HttpSession session = request.getSession(false);

        /*
         * 로그인 여부를 확인
         * 만약 로그인이 되어있지 않으면 게시글을 작성하면 안되므로 로그인 창을 보여주도록 설정
         * 반대로 로그인이 되어있으면 게시글을 작성할 수 있으므로 게시글 작성 화면을 보여주도록 설정
         */
        if (session == null || session.getAttribute(SessionConst.LOGIN_MEMBER) == null) {
            log.info("미인증 사용자 요청");

            // 로그인 화면으로 redirect
            response.sendRedirect("/login?redirectURL=" + requestURI);

            return false;
        }

        return true;
    }
}
