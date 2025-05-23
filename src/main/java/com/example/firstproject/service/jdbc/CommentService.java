package com.example.firstproject.service.jdbc;

import com.example.firstproject.domain.dto.comment.ResponseCommentDto;
import com.example.firstproject.domain.dto.comment.RequestCommentDto;
import com.example.firstproject.domain.dto.comment.UpdateCommentDto;
import com.example.firstproject.domain.jdbc.Comment;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;

    public void saveComment(Member loginMember, Long postId, RequestCommentDto requestCommentDto) {
        Comment comment = Comment.builder()
                .postId(postId)
                .memberId(loginMember.getId())
                .content(requestCommentDto.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        commentRepository.save(comment);
    }

    public void modifyComment(Long commentId, UpdateCommentDto updateCommentDto) {
        commentRepository.updateById(commentId, updateCommentDto);
    }

    public void saveReply(Member loginMember, Long postId, Long parentCommentId, String replyContent) {
        Comment comment = Comment.builder()
                .postId(postId)
                .memberId(loginMember.getId())
                .parentCommentId(parentCommentId)
                .content(replyContent)
                .createdAt(LocalDateTime.now())
                .build();

        commentRepository.save(comment);
    }

    public void deleteCommentByCommentId(Long commentId) {
        commentRepository.deleteById(commentId);

    }

    public List<ResponseCommentDto> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    public List<ResponseCommentDto> getReplies(Long postId) {
        return commentRepository.getReplies(postId);
    }

}
