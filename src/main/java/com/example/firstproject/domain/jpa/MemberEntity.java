package com.example.firstproject.domain.jpa;

import com.example.firstproject.domain.Role;
import com.example.firstproject.domain.dto.member.MemberDto;
import com.example.firstproject.domain.dto.member.UpdateMemberDto;
import com.example.firstproject.domain.jdbc.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String loginId;

    private String password;

    private String username;

    private String nickname;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    private LocalDateTime createdAt;

    @Builder.Default
    private LocalDateTime updatedAt = null;

    public void changeRole(String newRole) {
        this.role = "ADMIN".equals(newRole) ? Role.ADMIN : Role.USER;
        this.updatedAt = LocalDateTime.now();
    }

    public void update(UpdateMemberDto updateMemberDto) {
        this.loginId = updateMemberDto.getLoginId();
        this.password = updateMemberDto.getPassword();
        this.username = updateMemberDto.getUsername();
        this.nickname = updateMemberDto.getNickname();
        this.updatedAt = LocalDateTime.now();
    }

    public static MemberEntity from(MemberDto memberDto) {
        return MemberEntity.builder()
                .loginId(memberDto.getLoginId())
                .password(memberDto.getPassword())
                .username(memberDto.getUsername())
                .nickname(memberDto.getNickname())
                .role(memberDto.getRole())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
