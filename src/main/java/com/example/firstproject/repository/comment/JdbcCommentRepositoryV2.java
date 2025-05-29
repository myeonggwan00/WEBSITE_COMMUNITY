package com.example.firstproject.repository.comment;

import com.example.firstproject.domain.dto.comment.ResponseCommentDto;
import com.example.firstproject.domain.dto.comment.UpdateCommentDto;
import com.example.firstproject.domain.jdbc.Comment;
import com.example.firstproject.repository.CommentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 스프링의 SQLExceptionTranslator를 사용해서 SQL 예외를 Spring 데이터 접근 예외로 변환
 */
@Slf4j
public class JdbcCommentRepositoryV2 implements CommentRepository {
    private final DataSource dataSource;
    private final SQLExceptionTranslator exTranslator;

    public JdbcCommentRepositoryV2(DataSource dataSource) {
        this.dataSource = dataSource;
        this.exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
    }

    @Override
    public void save(Comment comment) {
        String sql = "insert into " +
                "comment(post_id, member_id, parent_comment_id, content, created_at) " +
                "values(?, ?, ?, ?, ?)";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, comment.getPostId());
            pstmt.setLong(2, comment.getMemberId());
            pstmt.setObject(3, comment.getParentCommentId(), Types.BIGINT);
            pstmt.setString(4, comment.getContent());
            pstmt.setTimestamp(5, Timestamp.valueOf(comment.getCreatedAt()));

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("save", sql, e), "add comment failed");
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void updateById(Long id, UpdateCommentDto updateCommentDto) {
        String sql = "UPDATE comment SET content = ?, updated_at = ? WHERE id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, updateCommentDto.getContent());
            pstmt.setTimestamp(2, Timestamp.valueOf(updateCommentDto.getUpdatedAt()));
            pstmt.setLong(3, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("updateById", sql, e), "update comment failed");
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        String sql = "DELETE FROM comment c WHERE c.member_id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, memberId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("deleteByMemberId", sql, e));
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void deleteByPostId(Long postId) {
        String sql = "DELETE FROM comment c WHERE c.post_id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, postId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("deleteByPostId", sql, e));
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void deleteById(Long id) {
        String deleteChildSql = "DELETE FROM comment WHERE parent_comment_id = ?";
        String deleteParentSql = "DELETE FROM comment WHERE id = ?";

        Connection con = null;
        PreparedStatement deleteChildPstmt = null;
        PreparedStatement deleteParentPstmt = null;

        try {
            con = getConnection();

            deleteChildPstmt = con.prepareStatement(deleteChildSql);
            deleteChildPstmt.setLong(1, id);
            deleteChildPstmt.executeUpdate();

            deleteParentPstmt = con.prepareStatement(deleteParentSql);
            deleteParentPstmt.setLong(1, id);
            deleteParentPstmt.executeUpdate();
        } catch (SQLException e) {
            throw exTranslator.translate("deleteById", deleteParentSql, e);
        } finally {
            close(con, deleteChildPstmt, null);
            close(con, deleteParentPstmt, null);
        }
    }

    @Override
    public List<ResponseCommentDto> findByPostId(Long postId) {
        String sql = "SELECT c.id, c.post_id, c.member_id, c.parent_comment_id, c.content, c.created_at, m.nickname " +
                "FROM comment c JOIN member m ON c.member_id = m.id " +
                "WHERE c.post_id = ? AND c.parent_comment_id is NULL";

        List<ResponseCommentDto> responseCommentDtoList = new ArrayList<>();

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setLong(1, postId);

            rs = pstmt.executeQuery();

            if(rs != null) {
                while (rs.next()) {
                    responseCommentDtoList.add(
                            ResponseCommentDto.builder()
                                    .id(rs.getLong("id"))
                                    .postId(rs.getLong("post_id"))
                                    .memberId(rs.getLong("member_id"))
                                    .parentCommentId(rs.getLong("parent_comment_id"))
                                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                                    .content(rs.getString("content"))
                                    .nickname(rs.getString("nickname"))
                                    .build()
                    );
                }
                return responseCommentDtoList;
            }

            return responseCommentDtoList;
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findByPostId", sql, e), "find comment failed");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public List<ResponseCommentDto> getReplies(Long postId) {
        String sql = "SELECT c.id, c.post_id, c.member_id, c.parent_comment_id, c.content, c.created_at, m.nickname " +
                "FROM comment c JOIN member m ON c.member_id = m.id " +
                "WHERE c.post_id = ? AND c.parent_comment_id IS NOT NULL";

        List<ResponseCommentDto> responseCommentDtoList = new ArrayList<>();

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setLong(1, postId);

            rs = pstmt.executeQuery();

            if(rs != null) {
                while (rs.next()) {
                    responseCommentDtoList.add(
                            ResponseCommentDto.builder()
                                    .id(rs.getLong("id"))
                                    .postId(rs.getLong("post_id"))
                                    .memberId(rs.getLong("member_id"))
                                    .parentCommentId(rs.getLong("parent_comment_id"))
                                    .content(rs.getString("content"))
                                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                                    .nickname(rs.getString("nickname"))
                                    .build()
                    );
                }
                return responseCommentDtoList;
            }

            return responseCommentDtoList;
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("getReplies", sql, e), "get comment failed");
        } finally {
            close(con, pstmt, rs);
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void close(Connection con, Statement stmt, ResultSet rs) {
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        JdbcUtils.closeConnection(con);
    }

}
