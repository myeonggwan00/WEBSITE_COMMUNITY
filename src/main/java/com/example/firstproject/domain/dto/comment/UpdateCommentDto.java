package com.example.firstproject.domain.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@AllArgsConstructor
public class UpdateCommentDto {
    private String content;
    private LocalDateTime updatedAt;
}
