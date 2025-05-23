package com.example.firstproject.domain.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestCommentDto {
    @NotBlank
    private String content;
}
