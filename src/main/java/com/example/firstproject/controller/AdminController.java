package com.example.firstproject.controller;

import com.example.firstproject.common.constant.SessionConst;
import com.example.firstproject.domain.dto.PageHandler;
import com.example.firstproject.domain.dto.SearchCondition;
import com.example.firstproject.domain.dto.SearchOption;
import com.example.firstproject.domain.dto.member.MemberDetails;
import com.example.firstproject.domain.dto.post.PostDetails;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.domain.jpa.MemberEntity;
import com.example.firstproject.service.jdbc.MemberService;
import com.example.firstproject.service.jdbc.PostService;
import com.example.firstproject.service.jpa.JpaMemberService;
import com.example.firstproject.service.jpa.JpaPostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AdminController {
    /* JDBC */
//    private final PostService postService;
//    private final MemberService memberService;

    /* JPA */
    private final JpaPostService postService;
    private final JpaMemberService memberService;

    /**
     * 관리자 페이지 제공
     */
    @GetMapping("/admin")
    public String admin() {
        return "admin/admin";
    }

    /**
     * 관리자 페이지 - 게시글 목록 페이지를 제공
     */
    @GetMapping("/admin/posts")
    public String posts(Model model,
                        @RequestParam(defaultValue = "1") Integer page,
                        @RequestParam(defaultValue = "10") Integer pageSize,
                        SearchCondition sc,
                        @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) MemberEntity loginMember) {
        List<SearchOption> searchOptions = new ArrayList<>();

        searchOptions.add(new SearchOption("C", "내용"));
        searchOptions.add(new SearchOption("T", "제목"));
        searchOptions.add(new SearchOption("W", "작성자"));

        Map<String, Integer> map = postService.getPageInfo(page, pageSize);
        List<PostDetails> posts = postService.getPagedPosts(map, sc);
        PageHandler pageHandler = postService.getPageHandler(sc, page, pageSize);

        model.addAttribute("posts", posts);
        model.addAttribute("pageHandler", pageHandler);
        model.addAttribute("searchOptions", searchOptions);
        model.addAttribute("searchCondition", sc);
        model.addAttribute("page", page);
        model.addAttribute("pageSize", pageSize);

        return "admin/adminPosts";
    }

    /**
     * 관리자 페이지 - 회원 목록 페이지를 제공
     */
    @GetMapping("/admin/members")
    public String members(Model model,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          SearchCondition sc) {
        List<SearchOption> searchOptions = new ArrayList<>();

        searchOptions.add(new SearchOption("I", "아이디"));
        searchOptions.add(new SearchOption("U", "이름"));
        searchOptions.add(new SearchOption("N", "닉네임"));

        Map<String, Integer> map = memberService.getPageInfo(page, pageSize);
        List<MemberDetails> members = memberService.getPagedMembers(map, sc);
        PageHandler pageHandler = memberService.getPageHandler(sc, page, pageSize);

        model.addAttribute("members", members);
        model.addAttribute("pageHandler", pageHandler);
        model.addAttribute("searchOptions", searchOptions);
        model.addAttribute("searchCondition", sc);
        model.addAttribute("page", page);
        model.addAttribute("pageSize", pageSize);

        return "admin/adminMembers";
    }

    /**
     * 관리자 페이지 - 회원 목록 페이지에서 회원 권한을 수정하는 요청 처리
     */
    @PostMapping("/admin/members/{memberId}/edit")
    @Transactional
    public String editMember(@PathVariable("memberId") Long memberId,
                             @RequestParam String newRole, HttpServletRequest request,
                             @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) MemberEntity loginMember) {
        memberService.changeMemberRole(loginMember, memberId, newRole, request);

        return "redirect:/admin/members";
    }

    /**
     * 관리자 페이지 - 회원 목록 페이지에서 회원을 삭제하는 요청 처리
     */
    @PostMapping("/admin/members/{memberId}/delete")
    @Transactional
    public String deleteMember(@PathVariable Long memberId,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer checkPage = memberService.checkPage(page);
        memberService.deleteMemberByAdmin(memberId);

        return "redirect:/admin/members?page=" + checkPage + "&pageSize=" + pageSize;
    }

    /**
     * 관리자 페이지 - 게시글 목록 페이지에서 게시글을 삭제하는 요청 처리
     */
    @PostMapping("/admin/posts/{postId}/delete")
    @Transactional
    public String deletePost(@PathVariable Long postId,
                             @RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer checkPage = postService.checkPage(page);
        postService.deletePost(postId);

        return "redirect:/admin/posts?page=" + checkPage + "&pageSize=" + pageSize;
    }
}
