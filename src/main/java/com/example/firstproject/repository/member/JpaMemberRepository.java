package com.example.firstproject.repository.member;

import com.example.firstproject.domain.dto.SearchCondition;
import com.example.firstproject.domain.dto.member.MemberDetails;
import com.example.firstproject.domain.dto.member.UpdateMemberDto;
import com.example.firstproject.domain.jpa.MemberEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMemberRepository {
    private final EntityManager em;

    public void save(MemberEntity member) {
        em.persist(member);
    }

    public void updateById(Long id, UpdateMemberDto updateMemberDto) {
        MemberEntity member = em.find(MemberEntity.class, id);
        member.update(updateMemberDto);
    }

    public Optional<MemberEntity> findById(Long id) {
        MemberEntity member = em.find(MemberEntity.class, id);
        return Optional.of(member);
    }

    public Optional<MemberEntity> findByLoginId(String loginId) {
        try {
            MemberEntity member = em.createQuery("select m from MemberEntity m where m.loginId = :loginId", MemberEntity.class).setParameter("loginId", loginId).getSingleResult();
            return Optional.of(member);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<MemberEntity> findAll() {
        String sql = "select m from MemberEntity m";
        return em.createQuery(sql, MemberEntity.class).getResultList();
    }

    public void deleteById(Long id) {
        em.remove(em.find(MemberEntity.class, id));
    }

    public boolean isLoginIdExists(String loginId) {
        String sql = "select count(m) from MemberEntity m where m.loginId = :loginId";
        Long count = em.createQuery(sql, Long.class).setParameter("loginId", loginId).getSingleResult();
        return count > 0;
    }

    public boolean isNickNameExists(String nickname) {
        String sql = "select count(m) from MemberEntity m where m.nickname = :nickname";
        Long count = em.createQuery(sql, Long.class).setParameter("nickname", nickname).getSingleResult();
        return count > 0;
    }

    /**
     * 검색 조건에 해당하는 게시글 목록에 페이징 처리를 하여 반환
     */
    public List<MemberDetails> getPagedMembersBySearchCondition(Map<String, Integer> map, SearchCondition sc) {
        Integer offset = map.get("offset");
        Integer pageSize = map.get("pageSize");

        return switch (sc.getOption()) {
            case "I" -> findByLoginId(offset, pageSize, sc.getKeyword());
            case "U" -> findByUsername(offset, pageSize, sc.getKeyword());
            case "N" -> findByNickname(offset, pageSize, sc.getKeyword());
            default -> findAll(offset, pageSize);
        };
    }

    public List<MemberDetails> findByLoginId(Integer offset, Integer limit, String keyword) {
        String sql = "select new com.example.firstproject.domain.dto.member.MemberDetails(m.id, m.loginId, m.username, m.nickname, m.createdAt, m.updatedAt, m.role) " +
                "from MemberEntity m " +
                "where m.loginId like :keyword";
        return em.createQuery(sql, MemberDetails.class).setParameter("keyword", "%" + keyword + "%").setFirstResult(offset).setMaxResults(limit).getResultList();
    }


    public List<MemberDetails> findByUsername(Integer offset, Integer limit, String keyword) {
        String sql = "select new com.example.firstproject.domain.dto.member.MemberDetails(m.id, m.loginId, m.username, m.nickname, m.createdAt, m.updatedAt, m.role) " +
                "from MemberEntity m " +
                "where m.username like :keyword";
        return em.createQuery(sql, MemberDetails.class).setParameter("keyword", "%" + keyword + "%").setFirstResult(offset).setMaxResults(limit).getResultList();
    }


    public List<MemberDetails> findByNickname(Integer offset, Integer limit, String keyword) {
        String sql = "select new com.example.firstproject.domain.dto.member.MemberDetails(m.id, m.loginId, m.username, m.nickname, m.createdAt, m.updatedAt, m.role) " +
                "from MemberEntity m " +
                "where m.nickname like :keyword";
        return em.createQuery(sql, MemberDetails.class).setParameter("keyword", "%" + keyword + "%").setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    public List<MemberDetails> findAll(Integer offset, Integer limit) {
        String sql = "select new com.example.firstproject.domain.dto.member.MemberDetails(m.id, m.loginId, m.username, m.nickname, m.createdAt, m.updatedAt, m.role) " +
                "from MemberEntity m";
        return em.createQuery(sql, MemberDetails.class).setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    /**
     * 검색 조건에 해당하는 게시글 개수 반환
     */
    public int getCountBySearchCondition(SearchCondition sc) {
        return switch (sc.getOption()) {
            case "I" -> getCountByLoginId(sc.getKeyword());
            case "U" -> getCountByUsername(sc.getKeyword());
            case "N" -> getCountByNickname(sc.getKeyword());
            default -> getCountAll();
        };
    }

    public int getCountByLoginId(String keyword) {
        String sql = "select count(*) from MemberEntity m where m.loginId like :keyword";
        return em.createQuery(sql, Long.class).setParameter("keyword", "%" + keyword + "%").getSingleResult().intValue();
    }

    public int getCountByUsername(String keyword) {
        String sql = "select count(*) from MemberEntity m where m.username like :keyword";
        return em.createQuery(sql, Long.class).setParameter("keyword", "%" + keyword + "%").getSingleResult().intValue();
    }

    public int getCountByNickname(String keyword) {
        String sql = "select count(*) from MemberEntity m where m.nickname like :keyword";
        return em.createQuery(sql, Long.class).setParameter("keyword", "%" + keyword + "%").getSingleResult().intValue();
    }

    public int getCountAll() {
        String sql = "select count(*) from MemberEntity";
        return em.createQuery(sql, Long.class).getSingleResult().intValue();
    }
}
