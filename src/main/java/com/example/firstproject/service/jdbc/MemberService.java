package com.example.firstproject.service.jdbc;

import com.example.firstproject.common.constant.SessionConst;
import com.example.firstproject.domain.Role;
import com.example.firstproject.domain.dto.PageHandler;
import com.example.firstproject.domain.dto.SearchCondition;
import com.example.firstproject.domain.dto.member.MemberDetails;
import com.example.firstproject.domain.dto.member.MemberDto;
import com.example.firstproject.domain.dto.member.UpdateMemberDto;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.domain.jdbc.Post;
import com.example.firstproject.repository.CommentRepository;
import com.example.firstproject.repository.FileRepository;
import com.example.firstproject.repository.MemberRepository;
import com.example.firstproject.repository.PostRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final FileRepository fileRepository;
    private final CommentRepository commentRepository;

    public Integer checkPage(Integer page) {
        if(memberRepository.getCountAll() - 1 < page * 10 - 9)
            return page == 1 ? page : page - 1;

        return page;
    }

    public PageHandler getPageHandler(SearchCondition sc, Integer page, Integer pageSize) {
        return new PageHandler(memberRepository.getCountBySearchCondition(sc), page, pageSize);
    }

    public Map<String, Integer> getPageInfo(Integer page, Integer pageSize) {
        Map<String, Integer> map = new HashMap<>();

        map.put("offset", (page - 1) * pageSize);
        map.put("pageSize", pageSize);

        return map;
    }

    public List<MemberDetails> getPagedMembers(Map<String, Integer> pageInfo, SearchCondition sc) {
        return memberRepository.getPagedMembersBySearchCondition(pageInfo, sc);
    }


    /**
     * 회원 정보를 조회하는 메서드, 수정 화면에 기존의 정보를 제공하기 위한 기능
     */
    public UpdateMemberDto findMember(Member member) {
        Member findMember = memberRepository
                .findById(member.getId())
                .orElseThrow(() -> new NoSuchElementException("가입되지 않은 회원입니다."));

        return UpdateMemberDto.from(findMember);
    }

    /**
     * 회원을 추가하는 메서드, 즉 회원가입 기능을 하는 메서드
     */
    public void saveMember(MemberDto member) {
        memberRepository.save(Member.from(member));
    }

    /**
     * 회원 권한을 변경하는 메서드
     */
    public void changeMemberRole(Member member, Long memberId, String newRole, HttpServletRequest request) {
        Member findMember = memberRepository.findById(memberId).get();

        Role role = "ADMIN".equals(newRole) ? Role.ADMIN : Role.USER;

        if(!Objects.equals(member.getUsername(), "admin") && "ADMIN".equals(findMember.getRole().name()) && "USER".equals(newRole)) {
            memberRepository.updateRoleById(memberId, role);
            terminateSession(request);
            return;
        }

        memberRepository.updateRoleById(memberId, role);
    }

    /**
     * 회원을 수정하는 메서드
     * - 세션에 회원 정보가 저장되어 있으므로 회원 수정시 세션 처리해줘야 함
     * - 로그인 아이디 기억 기능은 로그인 아이디의 값을 가지고 있는 쿠키로 구현하였는데 회원 수정시 쿠키도 처리해줘야 함
     */
    public void editMember(Long id, UpdateMemberDto updateMemberDto, BindingResult bindingResult,
                           HttpServletRequest request,HttpServletResponse response) {
        if(memberRepository.isLoginIdExists(updateMemberDto.getLoginId())) {
            bindingResult.rejectValue("loginId", "duplicate", "이미 사용 중인 아이디입니다.");
            return;
        }

        if(memberRepository.isNickNameExists(updateMemberDto.getNickname())) {
            bindingResult.rejectValue("nickname", "duplicate", "이미 사용 중인 닉네임입니다.");
            return;
        }

        memberRepository.updateById(id, updateMemberDto);
        Optional<Member> optionalFindMember = memberRepository.findById(id);

        // 세션 처리
        HttpSession session = request.getSession(false);

        if(session != null) {
            session.setAttribute(SessionConst.LOGIN_MEMBER, optionalFindMember.orElseThrow(RuntimeException::new));
        }

        // 쿠키 처리
        if(request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if("memberId".equals(cookie.getName())) {
                    deleteCookie(response);
                    creatCookie(response, updateMemberDto.getLoginId());

                    break;
                }
            }
        }
    }

    /**
     * 회원 탈퇴 기능을 하는 메서드
     * 단, 회원이 작성한 게시글 정보를 먼저 삭제하도록 구현
     */
    public void deleteMember(Member member, HttpServletRequest request) {
        deleteMemberCommon(member.getId());
        terminateSession(request);
    }

    /**
     * 관리자에 의해 회원 탈퇴 처리를 하는 메서드
     */
    public void deleteMemberByAdmin(Long memberId) {
        deleteMemberCommon(memberId);
    }

    private void deleteMemberCommon(Long memberId) {
        fileRepository.deleteByMemberId(memberId); // 회원이 업로드한 파일 삭제
        commentRepository.deleteByMemberId(memberId); // 회원이 작성한 댓글 삭제

        // 회원이 작성한 게시글에 존재하는 댓글 삭제
        List<Post> posts = postRepository.findByMemberId(memberId);
        for (Post post : posts) {
            commentRepository.deleteByPostId(post.getId());
        }

        postRepository.deleteByMemberId(memberId); // 회원이 작성한 게시글 삭제
        memberRepository.deleteById(memberId); // 회원 삭제
    }

    /**
     * 회원탈퇴 시, 회원탈퇴 전에 로그인으로 인해서 생성된 세션을 종료시키는 메서드
     */
    public void terminateSession(HttpServletRequest request) {
        request.getSession().invalidate();
    }

    public void checkMember(MemberDto member, BindingResult bindingResult) {
        if(memberRepository.isLoginIdExists(member.getLoginId())) {
            bindingResult.rejectValue("loginId", "duplicate", "이미 사용 중인 아이디입니다.");
        }

        if(memberRepository.isNickNameExists(member.getNickname())) {
            bindingResult.rejectValue("nickname", "duplicate", "이미 사용 중인 닉네임입니다.");
        }
    }

    /**
     * 쿠키를 생성하는 메서드
     */
    private void creatCookie(HttpServletResponse response, String id) {
        Cookie cookie = new Cookie("memberId", id);

        cookie.setPath("/");

        response.addCookie(cookie);
    }

    /**
     * 쿠키를 삭제하는 메서드
     */
    private void deleteCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("memberId", null);

        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }
}
