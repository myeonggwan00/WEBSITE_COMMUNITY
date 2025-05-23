package com.example.firstproject.domain.jdbc;

import lombok.*;

import java.time.LocalDateTime;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class File {
    private Long id;
    private Long postId;
    private String fileName;
    private String filePath;
    private LocalDateTime uploadedAt;

    public void uploadFile(Long postId) {
        this.postId = postId;
        this.uploadedAt = LocalDateTime.now();
    }
}
