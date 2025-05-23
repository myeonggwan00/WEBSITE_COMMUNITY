package com.example.firstproject.domain.jdbc;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.NumberFormat;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Post {
    private Long id;

    private Long memberId;

    private String title;

    private String content;

    @DateTimeFormat(pattern = "yy-MM-dd HH:mm")
    private LocalDateTime createdAt;

    @Builder.Default
    @DateTimeFormat(pattern = "yy-MM-dd HH:mm")
    private LocalDateTime updatedAt = null;

    @NumberFormat(pattern = "###,###")
    @Builder.Default
    private Long viewCnt = 0L;

    public Post() {
    }

    public Post(String title, String content, String username) {
        this.title = title;
        this.content = content;
    }

    public void increaseViewCnt() {
        viewCnt++;
    }
}
