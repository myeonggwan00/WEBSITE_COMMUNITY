package com.example.firstproject.repository.file;

import com.example.firstproject.domain.jdbc.File;
import com.example.firstproject.repository.FileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class JdbcFileRepositoryV4 implements FileRepository {
    private final NamedParameterJdbcTemplate template;

    public JdbcFileRepositoryV4(DataSource dataSource) {
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public void save(File file) {
        System.out.println("file.toString() = " + file.toString());
        String sql = "INSERT INTO FILE (post_id, filename, filepath, uploaded_At) VALUES (:postId, :fileName, :filePath, :uploadedAt)";

        BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(file);

        template.update(sql, params);
    }

    @Override
    public boolean checkFile(Long postId, String fileName) {
        String sql = "SELECT COUNT(*) FROM FILE WHERE post_id = :postId AND fileName = :fileName";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("fileName", fileName);

        Integer count = template.queryForObject(sql, params, Integer.class);

        return count > 0;
    }

    @Override
    public List<File> findByPostId(Long postId) {
        String sql = "SELECT * FROM FILE WHERE post_id = :postId";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("postId", postId);

        return  template.query(sql, params, fileRowMapper());
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM FILE WHERE id = :id";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);

        template.update(sql, params);
    }

    @Override
    public void deleteByPostIdAndFileName(Long postId, String fileName) {
        String sql = "DELETE FROM FILE WHERE post_id = :postId AND fileName = :fileName";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("fileName", fileName);


        template.update(sql, params);
    }

    @Override
    public void deleteByPostId(Long postId) {
        String sql = "DELETE FROM FILE WHERE post_id = :postId";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("postId", postId);

        template.update(sql, params);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        String sql = "DELETE FROM FILE f " +
                "WHERE f.post_id " +
                "IN (SELECT post_id FROM POST p WHERE p.member_id = :memberId) ";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("memberId", memberId);

        template.update(sql, params);
    }

    private RowMapper<File> fileRowMapper() {
        return (rs, rowNum) ->
                File.builder()
                        .id(rs.getLong("id"))
                        .postId(rs.getLong("post_id"))
                        .fileName(rs.getString("filename"))
                        .filePath(rs.getString("filepath"))
                        .uploadedAt(Optional.ofNullable(rs.getTimestamp("uploaded_at")).map(Timestamp::toLocalDateTime).orElse(null))
                        .build();
    }
}
