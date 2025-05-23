package com.example.firstproject.domain.dto.member;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginMember {
    // 회원 식별 아이디
    private long memberId;

    @NotBlank
    private String id;

    @NotBlank
    private String pwd;

    private boolean rememberId;

    public LoginMember() {}

    public LoginMember(String id) {
        this.id = id;
    }
}
