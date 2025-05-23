package com.example.firstproject.domain.dto.post;

import com.example.firstproject.domain.jpa.FileEntity;
import com.example.firstproject.domain.jpa.PostEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@ToString
public class PostDto {
    private Long id;

    private Long memberId;

    @NotBlank
    private String title;

    @NotEmpty
    private String content;

    @DateTimeFormat(pattern = "yy-MM-dd HH:mm")
    private LocalDateTime createdAt;

    @Builder.Default
    @DateTimeFormat(pattern = "yy-MM-dd HH:mm")
    private LocalDateTime updatedAt = null;

    private List<MultipartFile> files;

    private List<String> fileNames;


//    public static PostDto from(Post post) {
//        return PostDto.builder()
//                .memberId(post.getMemberId())
//                .title(post.getTitle())
//                .createdAt(post.getCreatedAt())
//                .updatedAt(post.getUpdatedAt())
//                .build();
//    }

    public static PostDto from(PostEntity postEntity) {
        return PostDto.builder()
                .id(postEntity.getId())
                .memberId(postEntity.getMember().getId())
                .title(postEntity.getTitle())
                .content(postEntity.getContent())
                .createdAt(postEntity.getCreatedAt())
                .updatedAt(postEntity.getUpdatedAt())
                .fileNames(postEntity.getFiles().stream().map(FileEntity::getFileName).collect(Collectors.toList()))
                .build();
    }

    public void updateLastUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
