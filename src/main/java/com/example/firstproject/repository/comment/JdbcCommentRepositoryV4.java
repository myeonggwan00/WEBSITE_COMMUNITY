package com.example.firstproject.repository.comment;

import com.example.firstproject.domain.dto.comment.ResponseCommentDto;
import com.example.firstproject.domain.dto.comment.UpdateCommentDto;
import com.example.firstproject.domain.jdbc.Comment;
import com.example.firstproject.repository.CommentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;


@Slf4j
@Repository
public class JdbcCommentRepositoryV4 implements CommentRepository {
    private final NamedParameterJdbcTemplate template;

    public JdbcCommentRepositoryV4(DataSource dataSource) {
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public void save(Comment comment) {
        String sql = "insert into " +
                "comment(post_id, member_id, parent_comment_id, content, created_at) " +
                "values(:postId, :memberId, :parentCommentId, :content, :createdAt)";

        SqlParameterSource params = new BeanPropertySqlParameterSource(comment);
        template.update(sql, params);
    }

    @Override
    public void updateById(Long id, UpdateCommentDto updateCommentDto) {
        String sql = "UPDATE comment SET content = :content, updated_at =:updatedAt WHERE id = :id";

        MapSqlParameterSource finalParam = new MapSqlParameterSource().addValue("id", id);

        BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(updateCommentDto);

        for (String paramName : params.getReadablePropertyNames()) {
            finalParam.addValue(paramName, params.getValue(paramName));
        }

        template.update(sql, finalParam);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        String sql = "DELETE FROM comment c WHERE c.member_id = :memberId";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("memberId", memberId);

        template.update(sql, params);
    }

    @Override
    public void deleteByPostId(Long postId) {
        String sql = "DELETE FROM comment c WHERE c.post_id = :postId";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("postId", postId);

        template.update(sql, params);
    }

    @Override
    public void deleteById(Long id) {
        String deleteChildSql = "DELETE FROM comment WHERE parent_comment_id = :parentCommentId";
        MapSqlParameterSource param1 = new MapSqlParameterSource().addValue("parentCommentId", id);
        template.update(deleteChildSql, param1);

        String deleteParentSql = "DELETE FROM comment WHERE id = :commentId";
        MapSqlParameterSource param2 = new MapSqlParameterSource().addValue("commentId", id);
        template.update(deleteParentSql, param2);
    }

    @Override
    public List<ResponseCommentDto> findByPostId(Long postId) {
        String sql = "SELECT c.id, c.post_id, c.member_id, c.parent_comment_id, c.content, c.created_at, c.updated_at, m.nickname " +
                "FROM comment c JOIN member m ON c.member_id = m.id " +
                "WHERE c.post_id = :postId AND c.parent_comment_id IS NULL";

        MapSqlParameterSource param = new MapSqlParameterSource().addValue("postId", postId);

        return template.query(sql, param, commentRowMapper());
    }

    @Override
    public List<ResponseCommentDto> getReplies(Long postId) {
        String sql = "SELECT c.id, c.post_id, c.member_id, c.parent_comment_id, c.content, c.created_at, c.updated_at, m.nickname " +
                "FROM comment c JOIN member m ON c.member_id = m.id " +
                "WHERE c.post_id = :postId AND c.parent_comment_id IS NOT NULL";

        MapSqlParameterSource param = new MapSqlParameterSource().addValue("postId", postId);

        return template.query(sql, param, commentRowMapper());
    }

    public RowMapper<ResponseCommentDto> commentRowMapper() {
        return (rs, rowNum) ->
                ResponseCommentDto.builder()
                        .id(rs.getLong("id"))
                        .postId(rs.getLong("post_id"))
                        .memberId(rs.getLong("member_id"))
                        .parentCommentId(rs.getLong("parent_comment_id"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                        .content(rs.getString("content"))
                        .nickname(rs.getString("nickname"))
                        .build();
    }
}
