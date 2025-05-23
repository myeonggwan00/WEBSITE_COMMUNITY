package com.example.firstproject.repository.post;

import com.example.firstproject.domain.dto.post.PostDetails;
import com.example.firstproject.domain.dto.post.PostDto;
import com.example.firstproject.domain.dto.SearchCondition;
import com.example.firstproject.domain.jdbc.File;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.domain.jdbc.Post;
import com.example.firstproject.repository.PostRepository;
import com.example.firstproject.common.exception.DbException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Repository
public class JdbcPostRepositoryV4 implements PostRepository {
    private final NamedParameterJdbcTemplate template;

    public JdbcPostRepositoryV4(DataSource dataSource) {
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public void save(Post post) {
        String sql = "insert into " +
                "Post (member_id, title, content, created_at, view_cnt) " +
                "values (:memberId, :title, :content, :createdAt, :viewCnt)";

        BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(post);

        template.update(sql, params);
    }

    @Override
    public void savePostWithFiles(Post post, List<File> files) {
        String postSql = "insert into Post (member_id, title, content, created_at, view_cnt) values (:memberId, :title, :content, :createdAt, :viewCnt)";
        String fileSql = "insert into File (post_id, filename, filepath, uploaded_at) values (:postId, :fileName, :filePath, :uploadedAt)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        MapSqlParameterSource postParams = new MapSqlParameterSource()
                .addValue("memberId", post.getMemberId())
                .addValue("title", post.getTitle())
                .addValue("content", post.getContent())
                .addValue("createdAt", Timestamp.valueOf(LocalDateTime.now()))
                .addValue("viewCnt", post.getViewCnt());

        template.update(postSql, postParams, keyHolder, new String[] {"id"});

        // 생성된 게시글 ID 가져오기
        Number key = keyHolder.getKey();

        if (key == null) {
            throw new DbException("게시글 저장 실패: ID를 가져올 수 없음");
        }

        long postId = key.longValue();

        for (File file : files) {
            MapSqlParameterSource fileParams = new MapSqlParameterSource()
                    .addValue("postId", postId)
                    .addValue("fileName", file.getFileName())
                    .addValue("filePath", file.getFilePath())
                    .addValue("uploadedAt", Timestamp.valueOf(LocalDateTime.now()));
            template.update(fileSql, fileParams);
        }
    }

    @Override
    public void updateById(Long id, PostDto updatePost) {
        String sql = "update Post set title = :title, content = :content, updated_at = :updatedAt where id = :id";

        MapSqlParameterSource finalParams = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("title", updatePost.getTitle())
                .addValue("content", updatePost.getContent())
                .addValue("updatedAt", Timestamp.valueOf(LocalDateTime.now()));

        template.update(sql, finalParams);
    }

    @Override
    public void updateViewCnt(Long id) {
        String sql = "update Post set view_cnt = view_cnt + 1 where id = :id";

        Map<String, Object> params = Map.of("id", id);

        template.update(sql, params);
    }

    @Override
    public Post findById(Long id) {
        String sql = "select * from Post where id = :id";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);

        return template.queryForObject(sql, params, postRowMapper());
    }

    @Override
    public List<Post> findByMemberId(Long memberId) {
        String sql = "select * from Post p where p.member_id = :memberId";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("memberId", memberId);

        return template.query(sql, params, postRowMapper());
    }

    @Override
    public void deleteById(Long id) {
        String sql = "delete from Post where id = :id";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);

        template.update(sql, params);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        String sql = "delete from Post p where p.member_id = :memberId";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("memberId", memberId);

        template.update(sql, params);
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
                "where p.content like :keyword and m.id = :memberId limit :limit offset :offset";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("keyword", "%" + keyword + "%")
                .addValue("memberId", memberId)
                .addValue("limit", limit)
                .addValue("offset", offset);

        return template.query(sql, params, postDetailsRowMapper());
    }

    @Override
    public List<PostDetails> findByTitle(Integer offset, Integer limit, String keyword, Long memberId) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join Member m on p.member_id = m.id " +
                "where p.title like :keyword and m.id = :memberId limit :limit offset :offset";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("keyword", "%" + keyword + "%")
                .addValue("memberId", memberId)
                .addValue("limit", limit)
                .addValue("offset", offset);

        return template.query(sql, params, postDetailsRowMapper());
    }

    @Override
    public List<PostDetails> findAll(Integer offset, Integer limit, Long memberId) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join member m on p.member_id = m.id " +
                "where m.id = :memberId limit :limit offset :offset";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("memberId", memberId)
                .addValue("limit", limit)
                .addValue("offset", offset);

        return template.query(sql, params, postDetailsRowMapper());
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
                "from post p join member m on p.member_id = m.id " +
                "where p.content like :keyword limit :limit offset :offset";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("keyword", "%" + keyword + "%")
                .addValue("limit", limit)
                .addValue("offset", offset);

        return template.query(sql, params, postDetailsRowMapper());
    }

    @Override
    public List<PostDetails> findByTitle(Integer offset, Integer limit, String keyword) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join member m on p.member_id = m.id " +
                "where p.title like :keyword limit :limit offset :offset";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("keyword", "%" + keyword + "%")
                .addValue("limit", limit)
                .addValue("offset", offset);

        return template.query(sql, params, postDetailsRowMapper());
    }

    @Override
    public List<PostDetails> findByWriter(Integer offset, Integer limit, String keyword) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join member m on p.member_id = m.id " +
                "where m.nickname like :keyword limit :limit offset :offset";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("keyword", "%" + keyword + "%")
                .addValue("limit", limit)
                .addValue("offset", offset);

        return template.query(sql, params, postDetailsRowMapper());
    }

    @Override
    public List<PostDetails> findAll(Integer offset, Integer limit) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join member m on p.member_id = m.id limit :limit offset :offset";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("offset", offset)
                .addValue("limit", limit);

        return template.query(sql, params, postDetailsRowMapper());
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
        String sql = "select count(*) from Post p where p.content like :keyword and p.member_id = :memberId";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("keyword", "%" + keyword + "%")
                .addValue("memberId", memberId);

        return Objects.requireNonNull(template.queryForObject(sql, params, Integer.class));
    }

    @Override
    public int getCountByTitle(String keyword, Long memberId) {
        String sql = "select count(*) from Post p where p.title like :keyword and p.member_id = :memberId";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("keyword", "%" + keyword + "%")
                .addValue("memberId", memberId);

        return Objects.requireNonNull(template.queryForObject(sql, params, Integer.class));
    }

    @Override
    public int getCountAll(Long memberId) {
        String sql = "select count(*) from Post p where p.member_id = :memberId";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("memberId", memberId);

        return Objects.requireNonNull(template.queryForObject(sql, params, Integer.class));
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
        String sql = "select count(*) from Post p where p.content like :keyword";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("keyword", "%" + keyword + "%");

        return Objects.requireNonNull(template.queryForObject(sql, params, Integer.class));
    }

    @Override
    public int getCountByTitle(String keyword) {
        String sql = "select count(*) from Post p where p.title like :keyword";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("keyword", "%" + keyword + "%");

        return Objects.requireNonNull(template.queryForObject(sql, params, Integer.class));
    }

    @Override
    public int getCountByWriter(String keyword) {
        String sql = "select count(*) from Post p join Member m on p.member_id = m.id " +
                "where m.username like :keyword";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("keyword", "%" + keyword + "%");

        return Objects.requireNonNull(template.queryForObject(sql, params, Integer.class));

    }

    @Override
    public int getCountAll() {
        String sql = "select count(*) from Post";

        return template.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
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
