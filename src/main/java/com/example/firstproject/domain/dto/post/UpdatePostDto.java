package com.example.firstproject.domain.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@ToString
public class UpdatePostDto {
    @NotBlank
    private String title;

    @NotEmpty
    private String content;

    private String username;

    private LocalDateTime updatedAt;

    private List<MultipartFile> files;

    private List<String> fileNames;

    public void initializeCreatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
