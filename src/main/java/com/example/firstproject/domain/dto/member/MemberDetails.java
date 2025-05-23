package com.example.firstproject.domain.dto.member;

import com.example.firstproject.domain.Role;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class MemberDetails {
    private Long id;

    private String loginId;

    private String username;

    private String nickname;

    @DateTimeFormat(pattern = "yy-MM-dd HH:mm")
    private LocalDateTime createdAt;

    @Builder.Default
    @DateTimeFormat(pattern = "yy-MM-dd HH:mm")
    private LocalDateTime updatedAt = null;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;
}
