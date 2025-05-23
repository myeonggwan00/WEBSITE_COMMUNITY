package com.example.firstproject.repository.member;

import com.example.firstproject.domain.Role;
import com.example.firstproject.domain.dto.SearchCondition;
import com.example.firstproject.domain.dto.member.MemberDetails;
import com.example.firstproject.domain.dto.member.UpdateMemberDto;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 스프링이 제공하는 데이터 접근 예외 추상화와 SQL 예외 변환기 적용
 */
@Slf4j
@Repository
public class JdbcMemberRepositoryV4 implements MemberRepository {
    private final NamedParameterJdbcTemplate template;

    public JdbcMemberRepositoryV4(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public void save(Member member) {
        String sql = "insert into member(login_id, password, username, nickname, role, created_at) values (:loginId, :password, :username, :nickname, :role, :createdAt)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("loginId", member.getLoginId())
                .addValue("password", member.getPassword())
                .addValue("username", member.getUsername())
                .addValue("nickname", member.getNickname())
                .addValue("role", member.getRole().toString())
                .addValue("createdAt", member.getCreatedAt());

        template.update(sql, params);
    }

    @Override
    public void updateById(Long id, UpdateMemberDto updateMemberDto) {
        String sql = "update member set login_id = :loginId, password = :password, username = :username, nickname = :nickname, updated_at = :updatedAt where id = :id";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("loginId", updateMemberDto.getLoginId())
                .addValue("password", updateMemberDto.getPassword())
                .addValue("username", updateMemberDto.getUsername())
                .addValue("nickname", updateMemberDto.getNickname())
                .addValue("updatedAt", Timestamp.valueOf(LocalDateTime.now()))
                .addValue("id", id);

        template.update(sql, params);
    }

    @Override
    public void updateRoleById(Long id, Role role) {
        String sql = "update member set role = :role, updated_at = :updatedAt where id = :id";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("role", role.toString())
                .addValue("updatedAt", Timestamp.valueOf(LocalDateTime.now()))
                .addValue("id", id);

        template.update(sql, params);
    }

    @Override
    public Optional<Member> findById(Long id) {
        String sql = "select * from member where id = :id";

        try {
            MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);

            Member member = template.queryForObject(sql, params, memberRowMapper());

            return Optional.ofNullable(member);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Member> findByLoginId(String loginId) {
        String sql = "select * from member where login_id = :loginId";
        try {
            MapSqlParameterSource params = new MapSqlParameterSource().addValue("loginId", loginId);

            Member member = template.queryForObject(sql, params, memberRowMapper());

            return Optional.ofNullable(member);
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
        String sql = "delete from member where id = :id";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);

        template.update(sql, params);
    }

    @Override
    public void deleteAll() {
        String sql = "delete from member";

        template.update(sql, new MapSqlParameterSource());
    }

    @Override
    public boolean isLoginIdExists(String loginId) {
        String sql = "SELECT COUNT(*) FROM MEMBER WHERE login_id = :loginId";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("loginId", loginId);

        int count = template.queryForObject(sql, params, Integer.class);

        return count > 0;
    }

    @Override
    public boolean isNickNameExists(String nickname) {
        String sql = "SELECT COUNT(*) FROM MEMBER WHERE nickname = :nickname";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("nickname", nickname);

        int count = template.queryForObject(sql, params, Integer.class);

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
                "where m.login_id like :keyword limit :limit offset :offset";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("keyword", "%" + keyword + "%")
                .addValue("limit", limit)
                .addValue("offset", offset);

        return template.query(sql, params, memberDetailsRowMapper());
    }

    @Override
    public List<MemberDetails> findByUsername(Integer offset, Integer limit, String keyword) {
        String sql = "select m.id, m.login_id, m.username, m.nickname, m.created_at, m.updated_at, m.role " +
                "from Member m " +
                "where m.username like :keyword limit :limit offset :offset";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("keyword", "%" + keyword + "%")
                .addValue("limit", limit)
                .addValue("offset", offset);

        return template.query(sql, params, memberDetailsRowMapper());
    }

    @Override
    public List<MemberDetails> findByNickname(Integer offset, Integer limit, String keyword) {
        String sql = "select m.id, m.login_id, m.username, m.nickname, m.created_at, m.updated_at, m.role " +
                "from Member m " +
                "where m.nickname like :keyword limit :limit offset :offset";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("keyword", "%" + keyword + "%")
                .addValue("limit", limit)
                .addValue("offset", offset);

        return template.query(sql, params, memberDetailsRowMapper());
    }

    @Override
    public List<MemberDetails> findAll(Integer offset, Integer limit) {
        String sql = "select m.id, m.login_id, m.username, m.nickname, m.created_at, m.updated_at, m.role " +
                "from Member m limit :limit offset :offset";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("offset", offset)
                .addValue("limit", limit);

        return template.query(sql, params, memberDetailsRowMapper());
    }

    /**
     * 검색 조건에 해당하는 회원수 반환
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
        String sql = "select count(*) from Member m where m.login_id like :keyword";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("keyword", "%" + keyword + "%");

        return Objects.requireNonNull(template.queryForObject(sql, params, Integer.class));
    }

    @Override
    public int getCountByUsername(String keyword) {
        String sql = "select count(*) from Member m where m.username like :keyword";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("keyword", "%" + keyword + "%");

        return Objects.requireNonNull(template.queryForObject(sql, params, Integer.class));
    }

    @Override
    public int getCountByNickname(String keyword) {
        String sql = "select count(*) from Member m where m.nickname like :keyword";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("keyword", "%" + keyword + "%");

        return Objects.requireNonNull(template.queryForObject(sql, params, Integer.class));
    }

    @Override
    public int getCountAll() {
        String sql = "select count(*) from Member";

        Integer count = template.queryForObject(sql, new MapSqlParameterSource(), Integer.class);

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
            Member member = new Member();

            member.setId(rs.getLong("id"));
            member.setLoginId(rs.getString("login_id"));
            member.setPassword(rs.getString("password"));
            member.setUsername(rs.getString("username"));
            member.setNickname(rs.getString("nickname"));
            member.setRole(Enum.valueOf(Role.class, rs.getString("role")));
            member.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

            return member;
        };
    }
}
