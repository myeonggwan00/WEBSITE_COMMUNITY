package com.example.firstproject.repository;


import com.example.firstproject.domain.Role;
import com.example.firstproject.domain.dto.SearchCondition;
import com.example.firstproject.domain.dto.member.MemberDetails;
import com.example.firstproject.domain.dto.member.UpdateMemberDto;
import com.example.firstproject.domain.jdbc.Member;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MemberRepository {
    void save(Member member);

    void updateById(Long id, UpdateMemberDto updateMemberDto);

    void updateRoleById(Long id, Role role);

    Optional<Member> findById(Long id);

    Optional<Member> findByLoginId(String loginId);

    List<Member> findAll();

    void deleteById(Long id);

    void deleteAll();

    boolean isLoginIdExists(String loginId);

    boolean isNickNameExists(String nickName);

    List<MemberDetails> getPagedMembersBySearchCondition(Map<String, Integer> map, SearchCondition sc);

    List<MemberDetails> findByLoginId(Integer offset, Integer limit, String keyword);

    List<MemberDetails> findByUsername(Integer offset, Integer limit, String keyword);

    List<MemberDetails> findByNickname(Integer offset, Integer limit, String keyword);

    List<MemberDetails> findAll(Integer offset, Integer limit);

    int getCountBySearchCondition(SearchCondition sc);

    int getCountByLoginId(String keyword);

    int getCountByUsername(String keyword);

    int getCountByNickname(String keyword);

    int getCountAll();
}
