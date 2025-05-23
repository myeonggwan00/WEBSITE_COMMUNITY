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
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
public class JdbcPostRepositoryV2 implements PostRepository {
    private final DataSource dataSource;
    private final SQLExceptionTranslator exTranslator;

    public JdbcPostRepositoryV2(DataSource dataSource) {
        this.dataSource = dataSource;
        this.exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
    }

    @Override
    public void save(Post post) {
        String sql = "insert into Post (member_id, title, content, created_at, view_cnt) " +
                "values (?, ?, ?, ?, ?)";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            con.setAutoCommit(false);

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, post.getMemberId());
            pstmt.setString(2, post.getTitle());
            pstmt.setString(3, post.getContent());
            pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setLong(5, post.getViewCnt());
            pstmt.executeUpdate();

            con.commit();
        } catch (SQLException e) {
            attemptRollback(con);
            throw Objects.requireNonNull(exTranslator.translate("save", sql, e), "save post failed");
        } finally {
            close(con, pstmt, null);
        }

    }

    @Override
    public void savePostWithFiles(Post post, List<File> files) {
        String postSql = "insert into Post (member_id, title, content, created_at, view_cnt) values (?, ?, ?, ?, ?)";
        String fileSql = "insert into File (post_id, filename, filepath, uploaded_at) values (?, ?, ?, ?)";

        Connection con = null;
        PreparedStatement postStmt = null;
        PreparedStatement fileStmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            con.setAutoCommit(false);

            postStmt = con.prepareStatement(postSql, Statement.RETURN_GENERATED_KEYS);

            // 게시글 저장
            postStmt.setLong(1, post.getMemberId());
            postStmt.setString(2, post.getTitle());
            postStmt.setString(3, post.getContent());
            postStmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            postStmt.setLong(5, post.getViewCnt());
            postStmt.executeUpdate();

            // 생성된 게시글 ID 가져오기
            rs = postStmt.getGeneratedKeys();

            if (!rs.next()) {
                throw new SQLException("게시글 저장 실패: ID를 가져올 수 없음");
            }

            long postId = rs.getLong(1);  // 생성된 post_id 가져오기

            // 파일 저장
            fileStmt = con.prepareStatement(fileSql);

            for (File file : files) {
                fileStmt.setLong(1, postId);
                fileStmt.setString(2, file.getFileName());
                fileStmt.setString(3, file.getFilePath());
                fileStmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                fileStmt.executeUpdate();
            }

            con.commit();
        } catch (SQLException e) {
            attemptRollback(con);
            throw new DbException(e);
        } finally {
            JdbcUtils.closeResultSet(rs);
            JdbcUtils.closeStatement(postStmt);
            JdbcUtils.closeStatement(fileStmt);
            JdbcUtils.closeConnection(con);
        }
    }

    @Override
    public void updateById(Long id, PostDto updatePost) {
        String sql = "update Post set title = ?, content = ?, updated_at = ? where id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            con.setAutoCommit(false);

            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, updatePost.getTitle());
            pstmt.setString(2, updatePost.getContent());
            pstmt.setTimestamp(3, Timestamp.valueOf(updatePost.getUpdatedAt()));
            pstmt.setLong(4, id);
            pstmt.executeUpdate();

            con.commit();
        } catch (SQLException e) {
            attemptRollback(con);
            throw Objects.requireNonNull(exTranslator.translate("update", sql, e), "update post failed");
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void updateViewCnt(Long id) {
        String sql = "update Post set view_cnt = view_cnt + 1 where id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            con.setAutoCommit(false);

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, id);
            pstmt.executeUpdate();

            con.commit();
        } catch (SQLException e) {
            attemptRollback(con);
            throw Objects.requireNonNull(exTranslator.translate("updateViewCnt", sql, e), "update ViewCnt failed");
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public Post findById(Long id) {
        String sql = "select * from Post where id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setLong(1, id);

            rs = pstmt.executeQuery();

            if(rs.next()) {
                return Post.builder()
                        .id(rs.getLong("id"))
                        .memberId(rs.getLong("member_id"))
                        .title(rs.getString("title"))
                        .content(rs.getString("content"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                        .viewCnt(rs.getLong("view_cnt"))
                        .build();
            } else {
                throw new NoSuchElementException("member not found id = " + id);
            }
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findById", sql, e), "find Post failed");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public List<Post> findByMemberId(Long memberId) {
        String sql = "select * from Post p where p.member_id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Post> posts = new ArrayList<>();

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setLong(1, memberId);

            rs = pstmt.executeQuery();

            if(rs != null) {
                while(rs.next()) {
                    posts.add(Post.builder()
                            .id(rs.getLong("id"))
                            .memberId(rs.getLong("member_id"))
                            .title(rs.getString("title"))
                            .content(rs.getString("content"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                            .viewCnt(rs.getLong("view_cnt"))
                            .build());
                }

                return posts;
            } else {
                throw new NoSuchElementException("postList not found memberId = " + memberId);
            }
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findByMemberId", sql, e), "find Post failed");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "delete from Post where id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            con.setAutoCommit(false);

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, id);
            pstmt.executeUpdate();

            con.commit();
        } catch (SQLException e) {
            attemptRollback(con);
            throw Objects.requireNonNull(exTranslator.translate("deleteById", sql, e), "delete Post failed");
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        String sql = "delete from post p where p.member_id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            con.setAutoCommit(false);

            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, memberId);
            pstmt.executeUpdate();

            con.commit();
        } catch(SQLException e) {
            attemptRollback(con);
            throw Objects.requireNonNull(exTranslator.translate("deleteByMemberId", sql, e), "delete Post failed");
        } finally {
            close(con, pstmt, null);
        }
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

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<PostDetails> postList = new ArrayList<>();

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setLong(2, memberId);
            pstmt.setInt(3, limit);
            pstmt.setInt(4, offset);

            rs = pstmt.executeQuery();

            if(rs != null) {
                while(rs.next()) {
                    postList.add(PostDetails.builder()
                            .id(rs.getLong("id"))
                            .title(rs.getString("title"))
                            .nickname(rs.getString("nickname"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                            .viewCnt(rs.getLong("view_cnt"))
                            .build());
                }

                return postList;
            } else {
                throw new NoSuchElementException("postList is not existed.");
            }
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findByContent", sql, e), "find postList failed");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public List<PostDetails> findByTitle(Integer offset, Integer limit, String keyword, Long memberId) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join Member m on p.member_id = m.id " +
                "where p.title like ? and m.id = ? limit ? offset ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<PostDetails> postList = new ArrayList<>();

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setLong(2, memberId);
            pstmt.setInt(3, limit);
            pstmt.setInt(4, offset);

            rs = pstmt.executeQuery();

            if(rs != null) {
                while(rs.next()) {
                    postList.add(PostDetails.builder()
                            .id(rs.getLong("id"))
                            .title(rs.getString("title"))
                            .nickname(rs.getString("nickname"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                            .viewCnt(rs.getLong("view_cnt"))
                            .build());
                }

                return postList;
            } else {
                throw new NoSuchElementException("postList is not existed.");
            }
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findByTitle", sql, e), "find postList failed");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public List<PostDetails> findAll(Integer offset, Integer limit, Long memberId) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from post p join Member m on p.member_id = m.id " +
                "where m.id = ? limit ? offset ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<PostDetails> postList = new ArrayList<>();

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setLong(1, memberId);
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);

            rs = pstmt.executeQuery();

            if(rs != null) {
                while(rs.next()) {
                    postList.add(PostDetails.builder()
                            .id(rs.getLong("id"))
                            .title(rs.getString("title"))
                            .nickname(rs.getString("nickname"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                            .viewCnt(rs.getLong("view_cnt"))
                            .build());
                }

                return postList;
            } else {
                throw new NoSuchElementException("postList is not existed.");
            }
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findAll", sql, e), "find postList failed");
        } finally {
            close(con, pstmt, rs);
        }
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
                "from Post p join Member m on p.member_id = m.id " +
                "where p.content like ? limit ? offset ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<PostDetails> postList = new ArrayList<>();


        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);

            rs = pstmt.executeQuery();

            if(rs != null) {
                while(rs.next()) {
                    postList.add(PostDetails.builder()
                            .id(rs.getLong("id"))
                            .title(rs.getString("title"))
                            .nickname(rs.getString("nickname"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                            .viewCnt(rs.getLong("view_cnt"))
                            .build());
                }

                return postList;
            } else {
                throw new NoSuchElementException("postList is not existed.");
            }
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findByContent", sql, e), "find postList failed");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public List<PostDetails> findByTitle(Integer offset, Integer limit, String keyword) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join Member m on p.member_id = m.id " +
                "where p.title like ? limit ? offset ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<PostDetails> postList = new ArrayList<>();

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, "%"+keyword+"%");
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);

            rs = pstmt.executeQuery();

            if(rs != null) {
                while(rs.next()) {
                    postList.add(PostDetails.builder()
                            .id(rs.getLong("id"))
                            .title(rs.getString("title"))
                            .nickname(rs.getString("nickname"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                            .viewCnt(rs.getLong("view_cnt"))
                            .build());
                }

                return postList;
            } else {
                throw new NoSuchElementException("postList is not existed.");
            }
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findByTitle", sql, e), "find postList failed");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public List<PostDetails> findByWriter(Integer offset, Integer limit, String keyword) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join Member m on p.member_id = m.id " +
                "where m.nickname like ? limit ? offset ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<PostDetails> postList = new ArrayList<>();

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, "%"+keyword+"%");
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);

            rs = pstmt.executeQuery();

            if(rs != null) {
                while(rs.next()) {
                    postList.add(PostDetails.builder()
                            .id(rs.getLong("id"))
                            .title(rs.getString("title"))
                            .nickname(rs.getString("nickname"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                            .viewCnt(rs.getLong("view_cnt"))
                            .build());
                }

                return postList;
            } else {
                throw new NoSuchElementException("postList is not existed.");
            }
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findByWriter", sql, e), "find postList failed");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public List<PostDetails> findAll(Integer offset, Integer limit) {
        String sql = "select p.id, p.title, p.content, m.nickname, p.created_at, p.updated_at, p.view_cnt " +
                "from Post p join Member m on p.member_id = m.id limit ? offset ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<PostDetails> postList = new ArrayList<>();

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);

            rs = pstmt.executeQuery();

            if(rs != null) {
                while(rs.next()) {
                    postList.add(PostDetails.builder()
                            .id(rs.getLong("id"))
                            .title(rs.getString("title"))
                            .nickname(rs.getString("nickname"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toLocalDateTime).orElse(null))
                            .viewCnt(rs.getLong("view_cnt"))
                            .build());
                }
                return postList;
            } else {
                throw new NoSuchElementException("postList is not existed.");
            }
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("findAll", sql, e), "find postList failed");
        } finally {
            close(con, pstmt, rs);
        }
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

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setLong(2, memberId);

            rs = pstmt.executeQuery();

            if(rs.next()) {
                return rs.getInt(1);
            } else {
                throw new NoSuchElementException("post not found");
            }
        } catch(SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("getCountByContent", sql, e), "post not found");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public int getCountByTitle(String keyword, Long memberId) {
        String sql = "select count(*) from Post p where p.title like ? and p.member_id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setLong(2, memberId);

            rs = pstmt.executeQuery();

            if(rs.next()) {
                return rs.getInt(1);
            } else {
                throw new NoSuchElementException("post not found");
            }
        } catch(SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("getCountByTitle", sql, e), "post not found");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public int getCountAll(Long memberId) {
        String sql = "select count(*) from Post p where p.member_id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setLong(1, memberId);

            rs = pstmt.executeQuery();

            if(rs.next()) {
                return rs.getInt(1);
            } else {
                throw new NoSuchElementException("post not found");
            }
        } catch(SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("getCountAll", sql, e), "post not found");
        } finally {
            close(con, pstmt, rs);
        }
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

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, "%"+keyword+"%");
            rs = pstmt.executeQuery();

            if(rs.next()) {
                return rs.getInt(1);
            } else {
                throw new NoSuchElementException("post not found");
            }
        } catch(SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("getCountByContent", sql, e), "post not found");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public int getCountByTitle(String keyword) {
        String sql = "select count(*) from Post p where p.title like ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, "%"+keyword+"%");
            rs = pstmt.executeQuery();

            if(rs.next()) {
                return rs.getInt(1);
            } else {
                throw new NoSuchElementException("post not found");
            }
        } catch(SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("getCountByTitle", sql, e), "post not found");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public int getCountByWriter(String keyword) {
        String sql = "select count(*) from Post p join Member m on p.member_id = m.id " +
                "where m.nickname like ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, "%"+keyword+"%");
            rs = pstmt.executeQuery();

            if(rs.next()) {
                return rs.getInt(1);
            } else {
                throw new NoSuchElementException("post not found");
            }
        } catch(SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("getCountByWriter", sql, e), "post not found");
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public int getCountAll() {
        String sql = "select count(*) from post";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();

            if(rs.next()) {
                int count = rs.getInt(1);
                return count;
            } else {
                throw new NoSuchElementException("post not found");
            }
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("getCountAll", sql, e), "post not found");
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

    private Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw Objects.requireNonNull(exTranslator.translate("getConnection", null, e), "get Connection failed");
        }
    }

    private void close(Connection con, Statement stmt, ResultSet rs) {
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        JdbcUtils.closeConnection(con);
    }
}
