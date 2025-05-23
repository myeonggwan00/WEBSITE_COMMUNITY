package com.example.firstproject.repository.member;

import com.example.firstproject.domain.Role;
import com.example.firstproject.domain.dto.SearchCondition;
import com.example.firstproject.domain.dto.member.MemberDetails;
import com.example.firstproject.domain.dto.member.UpdateMemberDto;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 스프링이 제공하는 데이터 접근 예외 추상화와 SQL 예외 변환기 적용
 */
@Slf4j
public class JdbcMemberRepositoryV3 implements MemberRepository {
    private final JdbcTemplate template;

    public JdbcMemberRepositoryV3(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    @Override
    public void save(Member member) {
        String sql = "insert into member(login_id, password, username, nickname, role, created_at) values (?, ?, ?, ?, ?, ?)";

        template.update(sql, member.getLoginId(), member.getPassword(), member.getUsername(), member.getNickname(), member.getRole().toString(), member.getCreatedAt());
    }

    @Override
    public void updateById(Long id, UpdateMemberDto updateMemberDto) {
        String sql = "update member set login_id = ?, password = ?, username = ?, nickname = ?, updated_at = ? where id = ?";

        template.update(sql, updateMemberDto.getLoginId(), updateMemberDto.getPassword(), updateMemberDto.getUsername(),
                updateMemberDto.getNickname(), Timestamp.valueOf(LocalDateTime.now()), id);
    }

    @Override
    public void updateRoleById(Long id, Role role) {
        String sql = "update member set role = ?, updated_at = ? where id = ?";

        template.update(sql, role.toString(), Timestamp.valueOf(LocalDateTime.now()), id);
    }

    @Override
    public Optional<Member> findById(Long id) {
        String sql = "select * from member where id = ?";

        try {
            Member member = template.queryForObject(sql, memberRowMapper(), id);

            return Optional.of(member);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }

    }

    @Override
    public Optional<Member> findByLoginId(String loginId) {
        String sql = "select * from member where login_id = ?";
        try {
            Member member = template.queryForObject(sql, memberRowMapper(), loginId);

            return Optional.of(member);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Member> findAll() {
        String sql = "select * from member";

        return template.query(sql, memberRowMapper());
    }

    @Override
    public void deleteById(Long id) {
        String sql = "delete from member where id = ?";

        template.update(sql, id);
    }

    @Override
    public void deleteAll() {
        String sql = "delete from member";

        template.update(sql);
    }

    @Override
    public boolean isLoginIdExists(String loginId) {
        String sql = "SELECT COUNT(*) FROM MEMBER WHERE login_id = ?";
        int count = template.queryForObject(sql, Integer.class, loginId);
        return count > 0;
    }

    @Override
    public boolean isNickNameExists(String nickName) {
        String sql = "SELECT COUNT(*) FROM MEMBER WHERE nickname = ?";
        int count = template.queryForObject(sql, Integer.class, nickName);
        return count > 0;
    }

    /**
     * 검색 조건에 해당하는 회원 목록에 페이징 처리를 하여 반환
     */
    @Override
    public List<MemberDetails> getPagedMembersBySearchCondition(Map<String, Integer> map, SearchCondition sc) {
        Integer offset = map.get("offset");
        Integer pageSize = map.get("pageSize");

        return switch (sc.getOption()) {
            case "I" -> findByLoginId(offset, pageSize, sc.getKeyword());
            case "U" -> findByUsername(offset, pageSize, sc.getKeyword());
            case "N" -> findByNickname(offset, pageSize, sc.getKeyword());
            default -> findAll(offset, pageSize);
        };
    }

    @Override
    public List<MemberDetails> findByLoginId(Integer offset, Integer limit, String keyword) {
        String sql = "select m.id, m.login_id, m.username, m.nickname, m.created_at, m.updated_at, m.role " +
                "from Member m " +
                "where m.login_id like ? limit ? offset ?";

        return template.query(sql, memberDetailsRowMapper(), "%" + keyword + "%", limit, offset);
    }

    @Override
    public List<MemberDetails> findByUsername(Integer offset, Integer limit, String keyword) {
        String sql = "select m.id, m.login_id, m.username, m.nickname, m.created_at, m.updated_at, m.role " +
                "from Member m " +
                "where m.username like ? limit ? offset ?";

        return template.query(sql, memberDetailsRowMapper(), "%" + keyword + "%", limit, offset);
    }

    @Override
    public List<MemberDetails> findByNickname(Integer offset, Integer limit, String keyword) {
        String sql = "select m.id, m.login_id, m.username, m.nickname, m.created_at, m.updated_at, m.role " +
                "from Member m " +
                "where m.nickname like ? limit ? offset ?";

        return template.query(sql, memberDetailsRowMapper(), "%" + keyword + "%", limit, offset);
    }

    @Override
    public List<MemberDetails> findAll(Integer offset, Integer limit) {
        String sql = "select m.id, m.login_id, m.username, m.nickname, m.created_at, m.updated_at, m.role " +
                "from Member m limit ? offset ?";

        return template.query(sql, memberDetailsRowMapper(), limit, offset);
    }

    /**
     * 검색 조건에 해당하는 회원수 구하기
     */
    @Override
    public int getCountBySearchCondition(SearchCondition sc) {
        return switch (sc.getOption()) {
            case "I" -> getCountByLoginId(sc.getKeyword());
            case "U" -> getCountByUsername(sc.getKeyword());
            case "N" -> getCountByNickname(sc.getKeyword());
            default -> getCountAll();
        };
    }

    @Override
    public int getCountByLoginId(String keyword) {
        String sql = "select count(*) from Member m where m.login_id like ?";

        return template.queryForObject(sql, Integer.class, "%" + keyword + "%");
    }

    @Override
    public int getCountByUsername(String keyword) {
        String sql = "select count(*) from Member m where m.username like ?";

        return template.queryForObject(sql, Integer.class, "%" + keyword + "%");
    }

    @Override
    public int getCountByNickname(String keyword) {
        String sql = "select count(*) from Member m where m.nickname like ?";

        return template.queryForObject(sql, Integer.class, "%" + keyword + "%");
    }

    @Override
    public int getCountAll() {
        String sql = "select count(*) from Member";

        Integer count = template.queryForObject(sql, Integer.class);

        return count != null ? count : 0;
    }

    private RowMapper<MemberDetails> memberDetailsRowMapper() {
        return (rs, rowNum) -> {
            return MemberDetails.builder()
                    .id(rs.getLong("id"))
                    .loginId(rs.getString("login_id"))
                    .username(rs.getString("username"))
                    .nickname(rs.getString("nickname"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                    .role(Role.valueOf(rs.getString("role")))
                    .build();
        };
    }

    private RowMapper<Member> memberRowMapper() {
        return (rs, rowNum) -> {
            return Member.builder()
                    .id(rs.getLong("id"))
                    .loginId(rs.getString("login_id"))
                    .password(rs.getString("password"))
                    .username(rs.getString("username"))
                    .nickname(rs.getString("nickname"))
                    .role(Role.valueOf(rs.getString("role")))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .build();
        };
    }
}
