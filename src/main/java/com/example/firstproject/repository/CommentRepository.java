package com.example.firstproject.repository;

import com.example.firstproject.domain.dto.comment.ResponseCommentDto;
import com.example.firstproject.domain.dto.comment.UpdateCommentDto;
import com.example.firstproject.domain.jdbc.Comment;

import java.util.List;

public interface CommentRepository {
    void save(Comment comment);

    void updateById(Long id, UpdateCommentDto updateCommentDto);

    void deleteByMemberId(Long memberId);

    void deleteByPostId(Long postId);

    void deleteById(Long id);

    List<ResponseCommentDto> findByPostId(Long postId);

    List<ResponseCommentDto> getReplies(Long postId);
}
