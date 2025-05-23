package com.example.firstproject.repository.comment;

import com.example.firstproject.domain.dto.comment.ResponseCommentDto;
import com.example.firstproject.domain.dto.comment.UpdateCommentDto;
import com.example.firstproject.domain.jdbc.Comment;
import com.example.firstproject.repository.CommentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.util.List;

@Slf4j
public class JdbcCommentRepositoryV3 implements CommentRepository {
    private final JdbcTemplate template;

    public JdbcCommentRepositoryV3(DataSource dataSource) {
        template = new JdbcTemplate(dataSource);
    }

    @Override
    public void save(Comment comment) {
        String sql = "insert into " +
                "comment(post_id, member_id, parent_comment_id, content, created_at) " +
                "values(?, ?, ?, ?, ?)";

        template.update(sql, comment.getPostId(), comment.getMemberId(), comment.getParentCommentId(), comment.getContent(), comment.getCreatedAt());
    }

    @Override
    public void updateById(Long id, UpdateCommentDto updateCommentDto) {
        String sql = "UPDATE comment SET content = ?, updated_at = ? WHERE id = ?";

        template.update(sql, updateCommentDto.getContent(), updateCommentDto.getUpdatedAt(), id);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        String sql = "DELETE FROM comment c WHERE c.member_id = ?";

        template.update(sql, memberId);
    }

    @Override
    public void deleteByPostId(Long postId) {
        String sql = "DELETE FROM comment c WHERE c.post_id = ?";

        template.update(sql, postId);
    }

    @Override
    public void deleteById(Long id) {
        String deleteChildSql = "DELETE FROM comment WHERE parent_comment_id = ?";
        template.update(deleteChildSql, id);

        String deleteParentSql = "DELETE FROM comment WHERE id = ?";
        template.update(deleteParentSql, id);
    }

    @Override
    public List<ResponseCommentDto> findByPostId(Long postId) {
        String sql = "SELECT c.id, c.post_id, c.member_id, c.parent_comment_id, c.content, c.created_at, m.nickname " +
                "FROM comment c JOIN member m ON c.member_id = m.id " +
                "WHERE c.post_id = ? AND c.parent_comment_id IS NULL";

        return template.query(sql, commentRowMapper(), postId);
    }

    @Override
    public List<ResponseCommentDto> getReplies(Long postId) {
        String sql = "SELECT c.id, c.post_id, c.member_id, c.parent_comment_id, c.content, c.created_at, m.nickname " +
                "FROM comment c JOIN member m ON c.member_id = m.id " +
                "WHERE c.post_id = ? AND c.parent_comment_id IS NOT NULL";
        return template.query(sql, commentRowMapper(), postId);
    }

    public RowMapper<ResponseCommentDto> commentRowMapper() {
        return (rs, rowNum) ->
                ResponseCommentDto.builder()
                        .id(rs.getLong("id"))
                        .postId(rs.getLong("post_id"))
                        .memberId(rs.getLong("member_id"))
                        .parentCommentId(rs.getLong("parent_comment_id"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .content(rs.getString("content"))
                        .nickname(rs.getString("nickname"))
                        .build();
    }

}
