package com.example.firstproject.domain.jdbc;

import com.example.firstproject.domain.dto.comment.CommentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class Comment {
    private Long id; // cno
    private Long postId; // postBno
    private Long memberId;
    private Long parentCommentId; // pcno
    private String content;
    private LocalDateTime createdAt;
    @Builder.Default
    private LocalDateTime updatedAt = null;

    public Comment() {}

    public Comment(Long id, Long parentCommentId, String comment, LocalDateTime createdAt) {
        this.id = id;
        this.parentCommentId = parentCommentId;
        this.content = comment;
        this.createdAt = createdAt;
    }

    public static Comment fromDto(CommentDto dto) {
        return Comment
                .builder()
                .memberId(dto.getMemberId())
                .postId(dto.getPostId())
                .parentCommentId(dto.getParentCommentId())
                .content(dto.getContent())
                .createdAt(dto.getCreatedAt())
                .build();
    }
}
