package com.example.firstproject.domain.jpa;

import com.example.firstproject.domain.dto.post.PostDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID")
    private MemberEntity member;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<CommentEntity> comments;

    private String title;

    private String content;

    private LocalDateTime createdAt;

    @Builder.Default
    private LocalDateTime updatedAt = null;
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FileEntity> files = new ArrayList<>();

    @Builder.Default
    private Long viewCnt = 0L;

    public void update(PostDto postDto) {
        this.title = postDto.getTitle();
        this.content = postDto.getContent();
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementViewCnt() {
        this.viewCnt++;
    }

    public void attachFiles(List<FileEntity> files) {
        this.files.addAll(files);
    }
}
