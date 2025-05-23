package com.example.firstproject.repository.file;

import com.example.firstproject.domain.jpa.FileEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaFileRepository {
    private final EntityManager em;

    public void save(FileEntity file) {
        em.persist(file);
    }

    public List<FileEntity> findByPostId(Long postId) {
        String sql = "select f from FileEntity f where f.post.id = :postId";

        return em.createQuery(sql, FileEntity.class).setParameter("postId", postId).getResultList();
    }

    public void deleteByPostIdAndFileName(Long postId, String fileName) {
        String sql = "delete from FileEntity f where f.post.id = :postId and f.fileName = :fileName";

        em.createQuery(sql).setParameter("postId", postId).setParameter("fileName", fileName).executeUpdate();
    }

    public void deleteByMemberId(Long memberId) {
        String sql = "delete from FileEntity f where f.post.member.id = :memberId";

        em.createQuery(sql).setParameter("memberId", memberId).executeUpdate();
    }

    public boolean checkFile(Long postId, String fileName) {
        String sql = "select count(f) from FileEntity f where f.post.id = :postId and f.fileName = :fileName";
        Long count = em.createQuery(sql, Long.class).setParameter("postId", postId).setParameter("fileName", fileName).getSingleResult();

        return count > 0;
    }
}
