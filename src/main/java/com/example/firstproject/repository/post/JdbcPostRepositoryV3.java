package com.example.firstproject.repository.post;

import com.example.firstproject.domain.dto.post.PostDetails;
import com.example.firstproject.domain.dto.post.PostDto;
import com.example.firstproject.domain.jdbc.File;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.domain.jdbc.Post;
import com.example.firstproject.domain.dto.SearchCondition;
import com.example.firstproject.repository.PostRepository;
import com.example.firstproject.common.exception.DbException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
public class JdbcPostRepositoryV3 implements PostRepository {
    private final JdbcTemplate template;

    public JdbcPostRepositoryV3(DataSource dataSource) {
        template = new JdbcTemplate(dataSource);
    }

    @Override
    public void save(Post post) {
        String sql = "INSERT INTO Post (member_id, title, content, created_at, view_cnt) " +
                "VALUES (?, ?, ?, ?, ?)";

        template.update(sql, post.getMemberId(), post.getTitle(), post.getContent(), Timestamp.valueOf(post.getCreatedAt()), post.getViewCnt());
    }

    @Override
    public void savePostWithFiles(Post post, List<File> files) {
        String postSql = "insert into Post (member_id, title, content, created_at, view_cnt) values (?, ?, ?, ?, ?)";
        String fileSql = "insert into File (post_id, filename, filepath, uploaded_at) values (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        template.update(con -> {
            PreparedStatement postStmt = con.prepareStatement(postSql, Statement.RETURN_GENERATED_KEYS);

            // 게시글 저장
            postStmt.setLong(1, post.getMemberId());
            postStmt.setString(2, post.getTitle());
            postStmt.setString(3, post.getContent());
            postStmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            postStmt.setLong(5, post.getViewCnt());

            return postStmt;
        }, keyHolder);

        // 생성된 게시글 ID 가져오기
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new DbException("게시글 저장 실패: ID를 가져올 수 없음");
        }

        long postId = key.longValue();
        System.out.println("postId = " + postId);

        for (File file : files) {
            template.update(fileSql, postId, file.getFileName(), file.getFilePath(), Timestamp.valueOf(LocalDateTime.now()));
        }
    }

    @Override
    public void updateById(Long id, PostDto updatePost) {
        String sql = "update Post set title = ?, content = ?, updated_at = ? where id = ?";

        template.update(sql, updatePost.getTitle(), updatePost.getContent(), updatePost.getUpdatedAt(), id);
    }

    @Override
    public void updateViewCnt(Long id) {
        String sql = "update Post set view_cnt = view_cnt + 1 where id = ?";

        template.update(sql, id);
    }

    @Override
    public Post findById(Long id) {
        String sql = "select * from Post where id = ?";

        return template.queryForObject(sql, postRowMapper(), id);
    }

    @Override
    public List<Post> findByMemberId(Long memberId) {
        String sql = "select * from Post p where p.member_id = ?";

        return template.query(sql, postRowMapper(), memberId);
    }

    @Override
    public void deleteById(Long id) {
        String sql = "delete from Post where id = ?";

        template.update(sql, id);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        String sql = "delete from Post p where p.member_id = ?";

        template.update(sql, memberId);
    }

    /**
     * 검색 조건에 해당하는 나의 게시글 목록에 페이징 처리를 하여 반환
     */
    @Override
    public List<PostDetails> getPagedMyPostsBySearchCondition(Map<String, Integer> map, SearchCondition sc, Member member) {
        Integer offset = map.get("offset");
        Integer pageSize = map.get("pageSize");

        return switch (sc.getOption()) {
            case "C" -> findByContent(offset, pageSize, sc.getKeyword(), member.getId());
            case "T" -> findByTitle(offset, pageSize, sc.getKeyword(), member.getId());
            default -> findAll(offset, pageSize, member.getId());
        };
    }

    @Override
    public List<PostDetails> findByContent(Integer offset, Integer limit, String keyword, Long memberId) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join Member m on p.member_id = m.id " +
                "where p.content like ? and m.id = ? limit ? offset ?";

        return template.query(sql, postDetailsRowMapper() ,"%" + keyword + "%", memberId, limit, offset);
    }

    @Override
    public List<PostDetails> findByTitle(Integer offset, Integer limit, String keyword, Long memberId) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join Member m on p.member_id = m.id " +
                "where p.title like ? and m.id = ? limit ? offset ?";

        return template.query(sql, postDetailsRowMapper() ,"%" + keyword + "%", memberId, limit, offset);
    }

    @Override
    public List<PostDetails> findAll(Integer offset, Integer limit, Long memberId) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join member m on p.member_id = m.id " +
                "where m.id = ? limit ? offset ?";

        return template.query(sql, postDetailsRowMapper(), memberId, limit, offset);
    }

    /**
     * 검색 조건에 해당하는 게시글 목록에 페이징 처리를 하여 반환
     */
    @Override
    public List<PostDetails> getPagedPostsBySearchCondition(Map<String, Integer> map, SearchCondition sc) {
        Integer offset = map.get("offset");
        Integer pageSize = map.get("pageSize");

        return switch (sc.getOption()) {
            case "C" -> findByContent(offset, pageSize, sc.getKeyword());
            case "T" -> findByTitle(offset, pageSize, sc.getKeyword());
            case "W" -> findByWriter(offset, pageSize, sc.getKeyword());
            default -> findAll(offset, pageSize);
        };
    }

    @Override
    public List<PostDetails> findByContent(Integer offset, Integer limit ,String keyword) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join member m on p.member_id = m.id " +
                "where p.content like ? limit ? offset ?";

        return template.query(sql, postDetailsRowMapper(), "%" + keyword + "%", limit, offset);
    }

    @Override
    public List<PostDetails> findByTitle(Integer offset, Integer limit, String keyword) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join member m on p.member_id = m.id " +
                "where p.title like ? limit ? offset ?";

        return template.query(sql, postDetailsRowMapper() ,"%" + keyword + "%", limit, offset);
    }

    @Override
    public List<PostDetails> findByWriter(Integer offset, Integer limit, String keyword) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join member m on p.member_id = m.id " +
                "where m.nickname like ? limit ? offset ?";

        return template.query(sql, postDetailsRowMapper() ,"%" + keyword + "%", limit, offset);
    }

    @Override
    public List<PostDetails> findAll(Integer offset, Integer limit) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join member m on p.member_id = m.id limit ? offset ?";

        return template.query(sql, postDetailsRowMapper(), limit, offset);
    }

    /**
     * 검색 조건에 해당하는 나의 게시글 수 구하기
     */
    @Override
    public int getMyCountBySearchCondition(SearchCondition sc, Member member) {
        return switch (sc.getOption()) {
            case "C" -> getCountByContent(sc.getKeyword(), member.getId());
            case "T" -> getCountByTitle(sc.getKeyword(), member.getId());
            default -> getCountAll(member.getId());
        };
    }

    @Override
    public int getCountByContent(String keyword, Long memberId) {
        String sql = "select count(*) from Post p where p.content like ? and p.member_id = ?";

        return template.queryForObject(sql, Integer.class, "%" + keyword + "%", memberId);
    }

    @Override
    public int getCountByTitle(String keyword, Long memberId) {
        String sql = "select count(*) from Post p where p.title like ? and p.member_id = ?";

        return template.queryForObject(sql, Integer.class, "%" + keyword + "%", memberId);
    }

    @Override
    public int getCountAll(Long memberId) {
        String sql = "select count(*) from Post p where p.member_id = ?";

        return template.queryForObject(sql, Integer.class, memberId);
    }

    /**
     * 검색 조건에 해당하는 게시글 수 구하기
     */
    @Override
    public int getCountBySearchCondition(SearchCondition sc) {
        return switch (sc.getOption()) {
            case "C" -> getCountByContent(sc.getKeyword());
            case "T" -> getCountByTitle(sc.getKeyword());
            case "W" -> getCountByWriter(sc.getKeyword());
            default -> getCountAll();
        };
    }

    @Override
    public int getCountByContent(String keyword) {
        String sql = "select count(*) from Post p where p.content like ?";

        return template.queryForObject(sql, Integer.class, "%" + keyword + "%");
    }

    @Override
    public int getCountByTitle(String keyword) {
        String sql = "select count(*) from Post p where p.title like ?";

        return template.queryForObject(sql, Integer.class, "%" + keyword + "%");
    }

    @Override
    public int getCountByWriter(String keyword) {
        String sql = "select count(*) from Post p join Member m on p.member_id = m.id " +
                "where m.nickname like ?";

        return template.queryForObject(sql, Integer.class, "%" + keyword + "%");
    }

    @Override
    public int getCountAll() {
        String sql = "select count(*) from Post";

        return template.queryForObject(sql, Integer.class);
    }

    private RowMapper<PostDetails> postDetailsRowMapper() {
        return (rs, rowNum) -> {
            return PostDetails.builder()
                    .id(rs.getLong("id"))
                    .title(rs.getString("title"))
                    .nickname(rs.getString("nickname"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                    .viewCnt(rs.getLong("view_cnt"))
                    .build();
        };
    }

    private RowMapper<Post> postRowMapper() {
        return (rs, rowNum) -> {
            return Post.builder()
                    .id(rs.getLong("id"))
                    .memberId(rs.getLong("member_id"))
                    .title(rs.getString("title"))
                    .content(rs.getString("content"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                    .viewCnt(rs.getLong("view_cnt"))
                    .build();
        };
    }

}
