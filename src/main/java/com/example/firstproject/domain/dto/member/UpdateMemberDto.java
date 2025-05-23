package com.example.firstproject.domain.dto.member;

import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.domain.Role;
import com.example.firstproject.domain.jpa.MemberEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@ToString
public class UpdateMemberDto {
    @NotBlank
    private String loginId;

    @NotBlank
    private String password;

    @NotBlank
    private String username;

    private String nickname;

    private Role role;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static UpdateMemberDto from(Member member) {
        return UpdateMemberDto.builder()
                .loginId(member.getLoginId())
                .password(member.getPassword())
                .username(member.getUsername())
                .nickname(member.getNickname())
                .role(member.getRole())
                .createdAt(member.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static UpdateMemberDto from(MemberEntity member) {
        return UpdateMemberDto.builder()
                .loginId(member.getLoginId())
                .password(member.getPassword())
                .username(member.getUsername())
                .nickname(member.getNickname())
                .role(member.getRole())
                .createdAt(member.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
