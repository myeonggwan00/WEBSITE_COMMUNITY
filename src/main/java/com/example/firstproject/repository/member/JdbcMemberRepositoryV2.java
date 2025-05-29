package com.example.firstproject.repository.member;

import com.example.firstproject.domain.Role;
import com.example.firstproject.domain.dto.SearchCondition;
import com.example.firstproject.domain.dto.member.MemberDetails;
import com.example.firstproject.domain.dto.member.UpdateMemberDto;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.common.db.DBConnectionUtils;
import com.example.firstproject.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 스프링이 제공하는 데이터 접근 예외 추상화와 SQL 예외 변환기 적용
 */
@Slf4j
public class JdbcMemberRepositoryV2 implements MemberRepository {
    private final DataSource dataSource;
    private final SQLExceptionTranslator exTranslator; // SQL 예외 변환기

    public JdbcMemberRepositoryV2(DataSource dataSource) {
        this.dataSource = dataSource;
        this.exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
    }

    @Override
    public void save(Member member) {
        String sql = "insert into member(login_id, password, username, nickname, role, created_at) values (?, ?, ?, ?, ?, ?)";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, member.getLoginId());
            pstmt.setString(2, member.getPassword());
            pstmt.setString(3, member.getUsername());
            pstmt.setString(4, member.getNickname());
            pstmt.setString(5, member.getRole().toString());
            pstmt.setTimestamp(6, Timestamp.valueOf(member.getCreatedAt()));
            pstmt.executeUpdate();
        } catch(SQLException e) {
            throw exTranslator.translate("save", sql, e);
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void updateById(Long id, UpdateMemberDto updateMemberDto) {
        String sql = "update member set login_id = ?, password = ?, username = ?, nickname = ?, updated_at = ? where id = ?";
        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = DBConnectionUtils.getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, updateMemberDto.getLoginId());
            pstmt.setString(2, updateMemberDto.getPassword());
            pstmt.setString(3, updateMemberDto.getUsername());
            pstmt.setString(4, updateMemberDto.getNickname());
            pstmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setLong(6, id);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            throw exTranslator.translate("update", sql, e);
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void updateRoleById(Long id, Role role) {
        String sql = "update member set role = ?, updated_at = ? where id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = DBConnectionUtils.getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, role.toString());
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setLong(3, id);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            throw exTranslator.translate("update", sql, e);
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public Optional<Member> findById(Long id) {
        String sql = "select * from member where id = ?";
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, id);

            rs = pstmt.executeQuery();

            if(rs.next()) {
                Member member = Member.builder()
                        .id(rs.getLong("id"))
                        .loginId(rs.getString("login_id"))
                        .password(rs.getString("password"))
                        .username(rs.getString("username"))
                        .nickname(rs.getString("nickname"))
                        .role(Role.valueOf(rs.getString("role")))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .build();

                return Optional.of(member);
            } else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw exTranslator.translate("findById", sql, e);
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public Optional<Member> findByLoginId(String loginId) {
        String sql = "select * from member where login_id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, loginId);

            rs = pstmt.executeQuery();

            if(rs.next()) {
                Member member = Member.builder()
                        .id(rs.getLong("id"))
                        .loginId(rs.getString("login_id"))
                        .password(rs.getString("password"))
                        .username(rs.getString("username"))
                        .nickname(rs.getString("nickname"))
                        .role(Role.valueOf(rs.getString("role")))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .build();

                return Optional.of(member);
            } else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw exTranslator.translate("findByLoginId", sql, e);
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public List<Member> findAll() {
        String sql = "select * from member";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Member> memberList = new ArrayList<>();

        try {
            con = getConnection();

            pstmt = con.prepareStatement(sql);

            rs = pstmt.executeQuery();


            if(rs != null) {
                while (rs.next()) {
                    Member member = new Member();

                    member.setId(rs.getLong("id"));
                    member.setLoginId(rs.getString("login_id"));
                    member.setPassword(rs.getString("password"));
                    member.setUsername(rs.getString("username"));

                    memberList.add(member);
                }

                return memberList;
            } else {
                throw new NoSuchElementException("memberList is not existed.");
            }
        } catch(SQLException e) {
            throw exTranslator.translate("findAll", sql, e);
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "delete from member where id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw exTranslator.translate("deleteById", sql, e);
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void deleteAll() {
        String sql = "delete from member";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw exTranslator.translate("deleteAll", sql, e);
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public boolean isLoginIdExists(String loginId) {
        String sql = "SELECT COUNT(*) FROM MEMBER WHERE login_id = ?";
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, loginId);

            rs = pstmt.executeQuery();

            return rs.next() ? rs.getInt(1) > 0 : false;
        } catch (SQLException e) {
            throw exTranslator.translate("isLoginIdExists", sql, e);
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public boolean isNickNameExists(String nickName) {
        String sql = "SELECT COUNT(*) FROM MEMBER WHERE nickname = ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, nickName);

            rs = pstmt.executeQuery();

            return rs.next() ? rs.getInt(1) > 0 : false;
        } catch (SQLException e) {
            throw exTranslator.translate("isNickNameExists", sql, e);
        } finally {
            close(con, pstmt, rs);
        }
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

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<MemberDetails> memberList = new ArrayList<>();

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);

            rs = pstmt.executeQuery();

            if(rs != null) {
                while (rs.next()) {
                    memberList.add(MemberDetails.builder()
                            .id(rs.getLong("id"))
                            .loginId(rs.getString("login_id"))
                            .username(rs.getString("username"))
                            .nickname(rs.getString("nickname"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                            .role(Role.valueOf(rs.getString("role")))
                            .build());
                }

                return memberList;
            } else {
                throw new NoSuchElementException("memberList is not existed.");
            }
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findByLoginId", sql, e), "find memberList failed");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public List<MemberDetails> findByUsername(Integer offset, Integer limit, String keyword) {
        String sql = "select m.id, m.login_id, m.username, m.nickname, m.created_at, m.updated_at, m.role " +
                "from Member m " +
                "where m.username like ? limit ? offset ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<MemberDetails> memberList = new ArrayList<>();

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);

            rs = pstmt.executeQuery();

            if(rs != null) {
                while (rs.next()) {
                    memberList.add(MemberDetails.builder()
                            .id(rs.getLong("id"))
                            .loginId(rs.getString("login_id"))
                            .username(rs.getString("username"))
                            .nickname(rs.getString("nickname"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                            .role(Role.valueOf(rs.getString("role")))
                            .build());
                }

                return memberList;
            } else {
                throw new NoSuchElementException("memberList is not existed.");
            }
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findByUsername", sql, e), "find memberList failed");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public List<MemberDetails> findByNickname(Integer offset, Integer limit, String keyword) {
        String sql = "select m.id, m.login_id, m.username, m.nickname, m.created_at, m.updated_at, m.role " +
                "from Member m " +
                "where m.nickname like ? limit ? offset ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<MemberDetails> memberList = new ArrayList<>();

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);

            rs = pstmt.executeQuery();

            if(rs != null) {
                while (rs.next()) {
                    memberList.add(MemberDetails.builder()
                            .id(rs.getLong("id"))
                            .loginId(rs.getString("login_id"))
                            .username(rs.getString("username"))
                            .nickname(rs.getString("nickname"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                            .role(Role.valueOf(rs.getString("role")))
                            .build());
                }

                return memberList;
            } else {
                throw new NoSuchElementException("memberList is not existed.");
            }
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findByNickname", sql, e), "find memberList failed");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public List<MemberDetails> findAll(Integer offset, Integer limit) {
        String sql = "select m.id, m.login_id, m.username, m.nickname, m.created_at, m.updated_at, m.role " +
                "from Member m limit ? offset ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<MemberDetails> memberList = new ArrayList<>();

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);

            rs = pstmt.executeQuery();

            if(rs != null) {
                while (rs.next()) {
                    memberList.add(MemberDetails.builder()
                            .id(rs.getLong("id"))
                            .loginId(rs.getString("login_id"))
                            .username(rs.getString("username"))
                            .nickname(rs.getString("nickname"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                            .role(Role.valueOf(rs.getString("role")))
                            .build());
                }

                return memberList;
            } else {
                throw new NoSuchElementException("memberList is not existed.");
            }
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findAll", sql, e), "find memberList failed");
        } finally {
            close(con, pstmt, rs);
        }
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
        String sql = "select count(*) from Member m where m.login_id like ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, "%" + keyword + "%");

            rs = pstmt.executeQuery();

            if(rs.next()) {
                return rs.getInt(1);
            } else {
                throw new NoSuchElementException("member not found");
            }
        } catch(SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("getCountByLoginId", sql, e), "member not found");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public int getCountByUsername(String keyword) {
        String sql = "select count(*) from Member m where m.username like ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, "%" + keyword + "%");

            rs = pstmt.executeQuery();

            if(rs.next()) {
                return rs.getInt(1);
            } else {
                throw new NoSuchElementException("member not found");
            }
        } catch(SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("getCountByUsername", sql, e), "member not found");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public int getCountByNickname(String keyword) {
        String sql = "select count(*) from Member m where m.nickname like ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, "%" + keyword + "%");

            rs = pstmt.executeQuery();

            if(rs.next()) {
                return rs.getInt(1);
            } else {
                throw new NoSuchElementException("member not found");
            }
        } catch(SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("getCountByNickname", sql, e), "member not found");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public int getCountAll() {
        String sql = "select count(*) from Member";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            rs = pstmt.executeQuery();

            if(rs.next()) {
                return rs.getInt(1);
            } else {
                throw new NoSuchElementException("member not found");
            }
        } catch(SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("getCountAll", sql, e), "member not found");
        } finally {
            close(con, pstmt, rs);
        }
    }

    private Connection getConnection() throws SQLException {
        Connection con = dataSource.getConnection();

        log.info("get connection={}, class={}", con, con.getClass());

        return con;
    }

    private void close(Connection con, Statement stmt, ResultSet rs) {
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        JdbcUtils.closeConnection(con);
    }
}
