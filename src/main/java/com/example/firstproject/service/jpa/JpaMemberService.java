package com.example.firstproject.service.jpa;

import com.example.firstproject.common.constant.SessionConst;
import com.example.firstproject.domain.dto.PageHandler;
import com.example.firstproject.domain.dto.SearchCondition;
import com.example.firstproject.domain.dto.member.MemberDetails;
import com.example.firstproject.domain.dto.member.MemberDto;
import com.example.firstproject.domain.dto.member.UpdateMemberDto;
import com.example.firstproject.domain.jpa.MemberEntity;
import com.example.firstproject.domain.jpa.PostEntity;
import com.example.firstproject.repository.comment.JpaCommentRepository;
import com.example.firstproject.repository.file.JpaFileRepository;
import com.example.firstproject.repository.member.JpaMemberRepository;
import com.example.firstproject.repository.post.JpaPostRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class JpaMemberService {
    private final JpaMemberRepository memberRepository;
    private final JpaPostRepository postRepository;
    private final JpaFileRepository fileRepository;
    private final JpaCommentRepository commentRepository;

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
    public UpdateMemberDto findMember(MemberEntity member) {
        MemberEntity findMember = memberRepository
                                    .findById(member.getId())
                                    .orElseThrow(() -> new NoSuchElementException("가입되지 않은 회원입니다."));

        return UpdateMemberDto.from(findMember);
    }

    /**
     * 회원을 추가하는 메서드, 즉 회원가입 기능을 하는 메서드
     */
    public void saveMember(MemberDto member) {
        memberRepository.save(MemberEntity.from(member));
    }

    /**
     * 회원 권한을 변경하는 메서드
     */
    public void changeMemberRole(MemberEntity member, Long memberId, String newRole, HttpServletRequest request) {
        MemberEntity findMember = memberRepository.findById(memberId).get();

        if(!Objects.equals(member.getUsername(), "admin") && "ADMIN".equals(findMember.getRole().name()) && "USER".equals(newRole)) {
            findMember.changeRole(newRole);
            terminateSession(request);
            return;
        }

        findMember.changeRole(newRole);
    }

    /**
     * 회원을 수정하는 메서드
     */
    public void editMember(Long id, UpdateMemberDto updateMemberDto, BindingResult bindingResult,
                           HttpServletRequest request, HttpServletResponse response) {
        if(memberRepository.isLoginIdExists(updateMemberDto.getLoginId())) {
            bindingResult.rejectValue("loginId", "duplicate", "이미 사용 중인 아이디입니다.");
            return;
        }

        if(memberRepository.isNickNameExists(updateMemberDto.getNickname())) {
            bindingResult.rejectValue("nickname", "duplicate", "이미 사용 중인 닉네임입니다.");
            return;
        }


        memberRepository.updateById(id, updateMemberDto);

        // 세션 처리
        HttpSession session = request.getSession(false);

        Optional<MemberEntity> optionalFindMember = memberRepository.findById(id);

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
     * 회원탈퇴 기능을 하는 메서드
     * 단, 회원이 작성한 게시글 정보를 먼저 삭제하도록 구현
     */
    public void deleteMember(MemberEntity member, HttpServletRequest request) {
        deleteMemberCommon(member.getId());
        terminateSession(request); // 세션 종료
    }

    /**
     * 관리자에 의해 회원 탈퇴 처리를 하는 메서드
     */
    public void deleteMemberByAdmin(Long memberId) {
        deleteMemberCommon(memberId);
    }

    /**
     * 회원 탈퇴 처리 로직
     */
    private void deleteMemberCommon(Long memberId) {
        fileRepository.deleteByMemberId(memberId); // 회원이 업로드한 파일 삭제
        commentRepository.deleteByMemberId(memberId);

        // 회원 탈퇴하기 전에 회원 식별자로 게시글을 삭제하려고 했다.
        // 어차피 엔티티 설계시 게시글과 댓글을 CASCADE 설정을 해둬서 게시글에 다른 사람이 작성한 댓글도 삭제하는 것을 기대했다.
        // 하지만 예상과는 다르게 CASCADE 적용이 되지 않았다....
        // CASCADE는 em.remove() 같은 JPA remove 동작에서만 작동한다.
        // deleteByMemberId() 내부 코드를 보면 JPA remove가 아닌 JPQL을 직접 작성해서 처리해서 예상과 다르게 작동되었다.
        List<PostEntity> posts = postRepository.findByMemberId(memberId);

        for (PostEntity post : posts) {
            postRepository.deleteById(post.getId());
        }

        memberRepository.deleteById(memberId); // 회원 삭제
    }

    /**
     * 회원탈퇴 시, 회원탈퇴 전에 로그인으로 인해서 생성된 세션을 종료시키는 메서드
     */
    public void terminateSession(HttpServletRequest request) {
        request.getSession().invalidate();
    }

    /**
     * 회원가입시 입력한 정보 확인하는 메서드
     */
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
