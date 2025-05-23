package com.example.firstproject.service.jpa;

import com.example.firstproject.domain.dto.comment.ResponseCommentDto;
import com.example.firstproject.domain.dto.comment.RequestCommentDto;
import com.example.firstproject.domain.dto.comment.UpdateCommentDto;
import com.example.firstproject.domain.jpa.CommentEntity;
import com.example.firstproject.domain.jpa.MemberEntity;
import com.example.firstproject.repository.comment.JpaCommentRepository;
import com.example.firstproject.repository.post.JpaPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class JpaCommentService {
    private final JpaCommentRepository commentRepository;
    private final JpaPostRepository postRepository;

    /**
     * 게시글에 댓글을 작성하는 메서드
     */
    public void saveComment(MemberEntity loginMember, Long postId, RequestCommentDto requestCommentDto) {
        CommentEntity comment = CommentEntity.builder()
                .post(postRepository.findById(postId))
                .member(loginMember)
                .content(requestCommentDto.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        commentRepository.save(comment);
    }

    /**
     * 게시글에 작성된 댓글을 수정하는 메서드
     */
    public void modifyComment(Long commentId, UpdateCommentDto updateCommentDto) {
        commentRepository.updateById(commentId, updateCommentDto);
    }

    /**
     * 게시글에 작성된 댓글에 답글 작성하는 메서드
     */
    public void saveReply(MemberEntity loginMember, Long postId, Long parentCommentId, String replyContent) {
        CommentEntity comment = CommentEntity.builder()
                .post(postRepository.findById(postId))
                .member(loginMember)
                .content(replyContent)
                .parentCommentId(parentCommentId)
                .createdAt(LocalDateTime.now())
                .build();

        commentRepository.save(comment);
    }

    /**
     * 게시글에 작성된 댓글 또는 답글을 삭제하는 메서드
     */
    public void deleteCommentByCommentId(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    /**
     * 게시글에 작성된 댓글을 조회하는 메서드
     */
    public List<ResponseCommentDto> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    /**
     * 게시글에 작성된 답글을 조회하는 메서드
     */
    public List<ResponseCommentDto> getReplies(Long postId) {
        return commentRepository.getReplies(postId);
    }

}
