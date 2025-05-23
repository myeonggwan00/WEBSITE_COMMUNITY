package com.example.firstproject.domain.dto.comment;

import com.example.firstproject.domain.jdbc.Comment;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {
    private Long postId; // postBno
    private Long memberId;
    @Builder.Default
    private Long parentCommentId = null; // pcno
    private String content;
    @DateTimeFormat(pattern = "yy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    static CommentDto from(Comment comment) {
        return CommentDto
                .builder()
                .postId(comment.getPostId())
                .content(comment.getContent()).build();
    }
}
