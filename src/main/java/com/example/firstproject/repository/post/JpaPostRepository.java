package com.example.firstproject.repository.post;

import com.example.firstproject.domain.dto.SearchCondition;
import com.example.firstproject.domain.dto.post.PostDetails;
import com.example.firstproject.domain.dto.post.PostDto;
import com.example.firstproject.domain.jpa.MemberEntity;
import com.example.firstproject.domain.jpa.PostEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class JpaPostRepository {
    private final EntityManager em;

    public void save(PostEntity post) {
        em.persist(post);
    }

    public void updateById(Long id, PostDto postDto) {
        em.find(PostEntity.class, id).update(postDto);
    }

    public void updateViewCnt(Long id) {
        em.find(PostEntity.class, id).incrementViewCnt();
    }

    public PostEntity findById(Long id) {
        return em.find(PostEntity.class, id);
    }

    public List<PostEntity> findAll() {
        return em.createQuery("select p from PostEntity p", PostEntity.class).getResultList();
    }

    public List<PostEntity> findByMemberId(Long memberId) {
        return em.createQuery("select p from PostEntity p where p.member.id = :memberId", PostEntity.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }

    public void deleteById(Long id) {
        em.remove(em.find(PostEntity.class, id));
    }

    public void deleteByMemberId(Long memberId) {
        em.createQuery("delete from PostEntity p where p.member.id = :memberId")
                .setParameter("memberId", memberId)
                .executeUpdate();
    }

    /**
     * 검색 조건에 해당하는 나의 게시글 목록에 페이징 처리를 하여 반환
     */
    public List<PostDetails> getPagedMyPostsBySearchCondition(Map<String, Integer> map, SearchCondition sc, MemberEntity member) {
        Integer offset = map.get("offset");
        Integer pageSize = map.get("pageSize");

        return switch (sc.getOption()) {
            case "C" -> findByContent(offset, pageSize, sc.getKeyword(), member.getId());
            case "T" -> findByTitle(offset, pageSize, sc.getKeyword(), member.getId());
            default -> findAll(offset, pageSize, member.getId());
        };
    }


    public List<PostDetails> findByTitle(Integer offset, Integer limit, String keyword, Long memberId) {
        String sql = "select new com.example.firstproject.domain.dto.post.PostDetails(p.id, p.title, p.content, m.nickname, p.createdAt, p.updatedAt, p.viewCnt) " +
                "from PostEntity p join p.member m " +
                "where p.title like :keyword and m.id = :memberId";

        return em.createQuery(sql, PostDetails.class)
                .setParameter("keyword", "%" + keyword + "%")
                .setParameter("memberId", memberId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }


    public List<PostDetails> findByContent(Integer offset, Integer limit, String keyword, Long memberId) {
        String sql = "select new com.example.firstproject.domain.dto.post.PostDetails(p.id, p.title, p.content, m.nickname, p.createdAt, p.updatedAt, p.viewCnt) " +
                "from PostEntity p join p.member m " +
                "where p.content like :keyword and m.id = :memberId";

        return em.createQuery(sql, PostDetails.class)
                .setParameter("keyword", "%" + keyword + "%")
                .setParameter("memberId", memberId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<PostDetails> findAll(Integer offset, Integer limit, Long memberId) {
        String sql = "select new com.example.firstproject.domain.dto.post.PostDetails(p.id, p.title, p.content, m.nickname, p.createdAt, p.updatedAt, p.viewCnt) " +
                "from PostEntity p join p.member m where m.id = :memberId";

        return em.createQuery(sql, PostDetails.class)
                .setParameter("memberId", memberId)
                .setFirstResult(offset).setMaxResults(limit)
                .getResultList();
    }

    /**
     * 검색 조건에 해당하는 게시글 목록에 페이징 처리를 하여 반환
     */
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

    public List<PostDetails> findAll(Integer offset, Integer limit) {
        String sql = "select new com.example.firstproject.domain.dto.post.PostDetails(p.id, p.title, p.content, m.nickname, p.createdAt, p.updatedAt, p.viewCnt) " +
                "from PostEntity p join p.member m";
        return em.createQuery(sql, PostDetails.class).setFirstResult(offset).setMaxResults(limit).getResultList();
    }


    public List<PostDetails> findByTitle(Integer offset, Integer limit, String keyword) {
        String sql = "select new com.example.firstproject.domain.dto.post.PostDetails(p.id, p.title, p.content, m.nickname, p.createdAt, p.updatedAt, p.viewCnt) " +
                "from PostEntity p join p.member m " +
                "where p.title like :keyword";
        return em.createQuery(sql, PostDetails.class).setParameter("keyword", "%" + keyword + "%").setFirstResult(offset).setMaxResults(limit).getResultList();
    }


    public List<PostDetails> findByContent(Integer offset, Integer limit, String keyword) {
        String sql = "select new com.example.firstproject.domain.dto.post.PostDetails(p.id, p.title, p.content, m.nickname, p.createdAt, p.updatedAt, p.viewCnt) " +
                "from PostEntity p join p.member m " +
                "where p.content like :keyword";
        return em.createQuery(sql, PostDetails.class).setParameter("keyword", "%" + keyword + "%").setFirstResult(offset).setMaxResults(limit).getResultList();
    }


    public List<PostDetails> findByWriter(Integer offset, Integer limit, String keyword) {
        String sql = "select new com.example.firstproject.domain.dto.post.PostDetails(p.id, p.title, p.content, m.nickname, p.createdAt, p.updatedAt, p.viewCnt) " +
                "from PostEntity p join p.member m " +
                "where m.nickname like :keyword";
        return em.createQuery(sql, PostDetails.class).setParameter("keyword", "%" + keyword + "%").setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    /**
     * 추가
     */
    public int getMyCountBySearchCondition(SearchCondition sc, MemberEntity member) {
        return switch (sc.getOption()) {
            case "C" -> getCountByContent(sc.getKeyword(), member.getId());
            case "T" -> getCountByTitle(sc.getKeyword(), member.getId());
            default -> getCountAll(member.getId());
        };
    }

    public int getCountByTitle(String keyword, Long memberId) {
        String sql = "select count(*) from PostEntity p where p.title like :keyword and p.member.id = :memberId";
        return em.createQuery(sql, Long.class)
                .setParameter("keyword", "%" + keyword + "%")
                .setParameter("memberId", memberId)
                .getSingleResult().intValue();
    }

    public int getCountByContent(String keyword, Long memberId) {
        String sql = "select count(*) from PostEntity p where p.content like :keyword and p.member.id = :memberId";
        return em.createQuery(sql, Long.class)
                .setParameter("keyword", "%" + keyword + "%")
                .setParameter("memberId", memberId)
                .getSingleResult().intValue();
    }

    public int getCountAll(Long memberId) {
        String sql = "select count(*) from PostEntity p where p.member.id = :memberId";
        return em.createQuery(sql, Long.class)
                .setParameter("memberId", memberId)
                .getSingleResult()
                .intValue();
    }

    /**
     * 검색 조건에 해당하는 게시글 개수 반환
     */
    public int getCountBySearchCondition(SearchCondition sc) {
        return switch (sc.getOption()) {
            case "C" -> getCountByContent(sc.getKeyword());
            case "T" -> getCountByTitle(sc.getKeyword());
            case "W" -> getCountByWriter(sc.getKeyword());
            default -> getCountAll();
        };
    }

    public int getCountByTitle(String keyword) {
        String sql = "select count(*) from PostEntity p where p.title like :keyword";
        return em.createQuery(sql, Long.class).setParameter("keyword", "%" + keyword + "%").getSingleResult().intValue();
    }

    public int getCountByContent(String keyword) {
        String sql = "select count(*) from PostEntity p where p.content like :keyword";
        return em.createQuery(sql, Long.class).setParameter("keyword", "%" + keyword + "%").getSingleResult().intValue();
    }

    public int getCountByWriter(String keyword) {
        String sql = "select count(*) from PostEntity p where p.member.nickname like :keyword";
        return em.createQuery(sql, Long.class).setParameter("keyword", "%" + keyword + "%").getSingleResult().intValue();
    }

    public int getCountAll() {
        String sql = "select count(*) from PostEntity";
        return em.createQuery(sql, Long.class).getSingleResult().intValue();
    }
}
