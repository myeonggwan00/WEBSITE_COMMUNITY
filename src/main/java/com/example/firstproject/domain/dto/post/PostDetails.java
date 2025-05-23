package com.example.firstproject.domain.dto.post;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class PostDetails {
    private Long id;

    private String title;

    private String content;

    private String nickname;

    @DateTimeFormat(pattern = "yy-MM-dd HH:mm")
    private LocalDateTime createdAt;

    @Builder.Default
    @DateTimeFormat(pattern = "yy-MM-dd HH:mm")
    private LocalDateTime updatedAt = null;

    private Long viewCnt;
}
