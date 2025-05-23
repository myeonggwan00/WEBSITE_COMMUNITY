package com.example.firstproject.domain.jdbc;

import com.example.firstproject.domain.Role;
import com.example.firstproject.domain.dto.member.MemberDto;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@ToString
public class Member {
    private Long id;

    private String loginId;

    private String password;

    private String username;

    private String nickname;

    private Role role;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt = null;

    public void changeRole(String newRole) {
        this.role = "ADMIN".equals(newRole) ? Role.ADMIN : Role.USER;
        this.updatedAt = LocalDateTime.now();
    }

    public Member(String loginId, String password, String username) {
        this.loginId = loginId;
        this.password = password;
        this.username = username;
    }

    public static Member from(MemberDto memberDto) {
        return Member.builder()
                .loginId(memberDto.getLoginId())
                .password(memberDto.getPassword())
                .username(memberDto.getUsername())
                .nickname(memberDto.getNickname())
                .role(memberDto.getRole())
                .createdAt(LocalDateTime.now())
                .build();

    }
}
