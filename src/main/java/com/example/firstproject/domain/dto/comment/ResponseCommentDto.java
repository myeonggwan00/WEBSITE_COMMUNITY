package com.example.firstproject.domain.dto.comment;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@Getter @Setter
@ToString
public class ResponseCommentDto {
    private Long id;
    private Long postId;
    private Long memberId;
    private Long parentCommentId; // pcno
    private String nickname; // userId
    private String content;
    @DateTimeFormat(pattern = "yy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public ResponseCommentDto(Long id, Long postId, Long memberId, Long parentCommentId, String nickname, String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.postId = postId;
        this.memberId = memberId;
        this.parentCommentId = parentCommentId;
        this.nickname = nickname;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
