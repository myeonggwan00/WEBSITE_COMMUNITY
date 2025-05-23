package com.example.firstproject.repository.comment;

import com.example.firstproject.domain.dto.comment.ResponseCommentDto;
import com.example.firstproject.domain.dto.comment.UpdateCommentDto;
import com.example.firstproject.domain.jdbc.Comment;
import com.example.firstproject.repository.CommentRepository;
import com.example.firstproject.common.exception.DbException;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.example.firstproject.common.db.DBConnectionUtils.getConnection;

@Slf4j
public class JdbcCommentRepositoryV1 implements CommentRepository {

    @Override
    public void save(Comment comment) {
        String sql = "insert into " +
                "comment(post_id, member_id, parent_comment_id, content, created_at) " +
                "values(?, ?, ?, ?, ?)";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            con.setAutoCommit(false); // 트랜잭션 시작

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, comment.getPostId());
            pstmt.setLong(2, comment.getMemberId());
            pstmt.setObject(3, comment.getParentCommentId(), Types.BIGINT);
            pstmt.setString(4, comment.getContent());
            pstmt.setTimestamp(5, Timestamp.valueOf(comment.getCreatedAt()));

            pstmt.executeUpdate();
            con.commit(); // 커밋
        } catch (SQLException e) {
            attemptRollback(con); // 롤백 시도
            throw new DbException(e);
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
            con.setAutoCommit(false); // 트랜잭션 시작

            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, updateCommentDto.getContent());
            pstmt.setTimestamp(2, Timestamp.valueOf(updateCommentDto.getUpdatedAt()));
            pstmt.setLong(3, id);
            pstmt.executeUpdate();

            con.commit(); // 커밋
        } catch (SQLException e) {
            attemptRollback(con); // 롤백 시도
            throw new DbException(e);
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
            con.setAutoCommit(false); // 트랜잭션 시작

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, memberId);
            pstmt.executeUpdate();

            con.commit(); // 커밋
        } catch (SQLException e) {
            attemptRollback(con); // 롤백 시도
            throw new DbException(e);
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
            con.setAutoCommit(false);

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, postId);
            pstmt.executeUpdate();

            con.commit();
        } catch (SQLException e) {
            attemptRollback(con); // 롤백 시도
            throw new DbException(e);
        } finally {
            close(con, pstmt, null);
        }
    }

    /**
     * 댓글 삭제시 부모 댓글에 자식 댓글이 있는 경우 자식 댓글까지 삭제하도록 처리
     */
    @Override
    public void deleteById(Long id) {
        String deleteChildSql = "DELETE FROM comment WHERE parent_comment_id = ?";
        String deleteParentSql = "DELETE FROM comment WHERE id = ?";

        Connection con = null;
        PreparedStatement deleteChildPstmt = null;
        PreparedStatement deleteParentPstmt = null;

        try {
            con = getConnection();
            con.setAutoCommit(false);

            deleteChildPstmt = con.prepareStatement(deleteChildSql);
            deleteChildPstmt.setLong(1, id);
            deleteChildPstmt.executeUpdate();

            deleteParentPstmt = con.prepareStatement(deleteParentSql);
            deleteParentPstmt.setLong(1, id);
            deleteParentPstmt.executeUpdate();

            con.commit();
        } catch (SQLException e) {
            attemptRollback(con); // 롤백 시도
            throw new DbException(e);
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
            throw new DbException(e);
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
            throw new DbException(e);
        } finally {
            close(con, pstmt, rs);
        }
    }

    private void attemptRollback(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException rollbackEx) {
                log.warn("Rollback failed: {}", rollbackEx.getMessage());
            }
        }
    }

    private void close(Connection con, Statement stmt, ResultSet rs) {
        if(rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.info("error", e);
            }
        }

        if(stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.info("error", e);
            }
        }

        if(con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                log.info("error", e);
            }
        }
    }
}
