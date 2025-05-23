package com.example.firstproject.domain.jpa;

import com.example.firstproject.domain.dto.comment.UpdateCommentDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // cno

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POST_ID")
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID")
    private MemberEntity member;

    private Long parentCommentId; // pcno

    private String content;

    private LocalDateTime createdAt;

    @Builder.Default
    private LocalDateTime updatedAt = null;

    public void updateComment(UpdateCommentDto updateCommentDto) {
        this.content = updateCommentDto.getContent();
        this.updatedAt = LocalDateTime.now();
    }
}
