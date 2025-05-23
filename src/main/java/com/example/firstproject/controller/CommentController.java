package com.example.firstproject.controller;

import com.example.firstproject.common.constant.SessionConst;
import com.example.firstproject.domain.dto.comment.RequestCommentDto;
import com.example.firstproject.domain.dto.comment.UpdateCommentDto;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.domain.jpa.MemberEntity;
import com.example.firstproject.service.jdbc.CommentService;
import com.example.firstproject.service.jpa.JpaCommentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@Slf4j
@RequiredArgsConstructor
public class CommentController {
    /* JDBC */
//    private final CommentService commentService;

    /* JPA */
    private final JpaCommentService commentService;

    /**
     * 게시글에 댓글 작성하는 요청을 처리
     */
    @PostMapping("/posts/{postId}/comment")
    public String comment(@PathVariable Long postId, RequestCommentDto comment,
                          @RequestParam String prevUri, RedirectAttributes redirectAttributes,
                          @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) MemberEntity loginMember) {
        redirectAttributes.addAttribute("postId", postId);
        redirectAttributes.addAttribute("prevUri", prevUri);

        commentService.saveComment(loginMember, postId, comment);

        return "redirect:/posts/{postId}";
    }

    /**
     * 게시글에 작성된 댓글을 삭제하는 요청을 처리
     */
    @PostMapping("/posts/{postId}/comment/{commentId}/delete")
    public String deleteComment(@PathVariable Long postId, @PathVariable Long commentId,
                                @RequestParam String prevUri, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("postId", postId);
        redirectAttributes.addAttribute("prevUri", prevUri);

        commentService.deleteCommentByCommentId(commentId);

        return "redirect:/posts/{postId}";
    }

    /**
     * 게시글에 작성된 댓글을 수정하는 요청을 처리
     */
    @PostMapping("/posts/{postId}/comment/{commentId}/edit")
    public String modifyComment(@PathVariable Long postId, @PathVariable Long commentId,
                                @RequestParam String modifyContent, @RequestParam String prevUri,
                                RedirectAttributes redirectAttributes) {
        UpdateCommentDto updateCommentDto = new UpdateCommentDto(modifyContent, LocalDateTime.now());

        redirectAttributes.addAttribute("postId", postId);
        redirectAttributes.addAttribute("prevUri", prevUri);

        commentService.modifyComment(commentId, updateCommentDto);

        return "redirect:/posts/{postId}";
    }

    /**
     * 게시글에 작성된 댓글에 답글을 작성하는 요청을 처리
     */
    @PostMapping("/posts/{postId}/comment/{parentCommentId}/reply")
    public String addReply(@PathVariable Long postId, @PathVariable Long parentCommentId, @RequestParam String replyContent,
                           @RequestParam String prevUri, RedirectAttributes redirectAttributes,
                           @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) MemberEntity loginMember) {
        redirectAttributes.addAttribute("postId", postId);
        redirectAttributes.addAttribute("prevUri", prevUri);

        commentService.saveReply(loginMember, postId, parentCommentId, replyContent);

        return "redirect:/posts/{postId}";
    }
}
