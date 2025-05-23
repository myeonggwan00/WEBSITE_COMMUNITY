package com.example.firstproject.repository.file;

import com.example.firstproject.domain.jdbc.File;
import com.example.firstproject.repository.FileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Optional;

@Slf4j
public class JdbcFileRepositoryV3 implements FileRepository {
    private final JdbcTemplate template;

    public JdbcFileRepositoryV3(DataSource dataSource) {
        template = new JdbcTemplate(dataSource);
    }

    @Override
    public void save(File file) {
        String sql = "INSERT INTO FILE (post_id, filename, filepath, uploaded_At) VALUES (?, ?, ?, ?)";

        template.update(sql, file.getPostId(), file.getFileName(), file.getFilePath(), file.getUploadedAt());
    }

    @Override
    public boolean checkFile(Long postId, String fileName) {
        String sql = "SELECT COUNT(*) FROM FILE WHERE post_id = ? AND fileName = ?";

        Integer count = template.queryForObject(sql, Integer.class, postId, fileName);

        return count > 0;
    }

    @Override
    public List<File> findByPostId(Long postId) {
        String sql = "SELECT * FROM FILE WHERE post_id = ?";

        return  template.query(sql, fileRowMapper(), postId);
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM FILE WHERE id = ?";

        template.update(sql, id);
    }

    @Override
    public void deleteByPostIdAndFileName(Long postId, String fileName) {
        String sql = "DELETE FROM FILE WHERE post_id = ? AND fileName = ?";

        template.update(sql, postId, fileName);
    }

    @Override
    public void deleteByPostId(Long postId) {
        String sql = "DELETE FROM FILE WHERE post_id = ?";

        template.update(sql, postId);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        String sql = "DELETE FROM FILE f " +
                "WHERE f.post_id " +
                "IN (SELECT post_id FROM POST p WHERE p.member_id = ?) ";

        template.update(sql, memberId);
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
