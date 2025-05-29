package com.example.firstproject.repository.file;

import com.example.firstproject.domain.jdbc.File;
import com.example.firstproject.repository.FileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class JdbcFileRepositoryV2 implements FileRepository {
    private final DataSource dataSource;
    private final SQLExceptionTranslator exTranslator;

    public JdbcFileRepositoryV2(DataSource dataSource) {
        this.dataSource = dataSource;
        this.exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
    }

    @Override
    public void save(File file) {
        String sql = "INSERT INTO FILE (post_id, filename, filepath, uploaded_At) VALUES (?, ?, ?, ?)";
        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, file.getPostId());
            pstmt.setString(2, file.getFileName());
            pstmt.setString(3, file.getFilePath());
            pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("save", sql, e), "save file failed");
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public boolean checkFile(Long postId, String fileName) {
        String sql = "SELECT COUNT(*) FROM FILE WHERE post_id = ? AND fileName = ?";
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setLong(1, postId);
            pstmt.setString(2, fileName);

            rs = pstmt.executeQuery();

            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("check", sql, e));
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public List<File> findByPostId(Long postId) {
        String sql = "SELECT * FROM FILE WHERE post_id = ?";
        ArrayList<File> files = new ArrayList<>();

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setLong(1, postId);

            rs = pstmt.executeQuery();

            while (rs.next()) {
                files.add(File.builder()
                        .id(rs.getLong("id"))
                        .postId(rs.getLong("post_id"))
                        .fileName(rs.getString("filename"))
                        .filePath(rs.getString("filepath"))
                        .uploadedAt(Optional.ofNullable(rs.getTimestamp("uploaded_at")).map(Timestamp::toLocalDateTime).orElse(null))
                        .build());
            }

            return files;
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findByPostId", sql, e), "findByPostId file failed");
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM FILE WHERE id = ?";
        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("deleteById", sql, e), "delete file failed");
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void deleteByPostIdAndFileName(Long postId, String fileName) {
        String sql = "DELETE FROM FILE WHERE post_id = ? AND fileName = ?";
        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, postId);
            pstmt.setString(2, fileName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("deleteByPostIdAndFileName", sql, e), "delete file failed");
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void deleteByPostId(Long postId) {
        String sql = "DELETE FROM FILE WHERE post_id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, postId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("deleteByPostId", sql, e), "delete file failed");
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        String sql = "DELETE FROM FILE f " +
                "WHERE f.post_id " +
                "IN (SELECT post_id FROM POST p WHERE p.member_id = ?) ";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, memberId);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("deleteByMemberId", sql, e), "delete file failed");
        } finally {
            close(con, pstmt, null);
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
