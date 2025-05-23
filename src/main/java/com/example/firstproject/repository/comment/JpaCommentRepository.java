package com.example.firstproject.repository.comment;

import com.example.firstproject.domain.dto.comment.ResponseCommentDto;
import com.example.firstproject.domain.dto.comment.UpdateCommentDto;
import com.example.firstproject.domain.jpa.CommentEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class JpaCommentRepository {
    private final EntityManager em;

    public void save(CommentEntity comment) {
        em.persist(comment);
    }

    public void updateById(Long id, UpdateCommentDto updateCommentDto) {
        CommentEntity comment = em.find(CommentEntity.class, id);
        comment.updateComment(updateCommentDto);
    }

    public void deleteByMemberId(Long memberId) {
        String sql = "delete from CommentEntity c where c.member.id = :memberId";
        em.createQuery(sql).setParameter("memberId", memberId).executeUpdate();
    }

    public void deleteById(Long id) {
        List<CommentEntity> childComments = em.createQuery("select c from CommentEntity c where c.parentCommentId = :parentCommentId", CommentEntity.class)
                .setParameter("parentCommentId", id)
                .getResultList();

        for (CommentEntity childComment : childComments) {
            em.remove(childComment);
        }

        em.remove(em.find(CommentEntity.class, id));
    }

    public List<ResponseCommentDto> findByPostId(Long postId) {
        String sql = "SELECT new com.example.firstproject.domain.dto.comment.ResponseCommentDto(c.id, c.post.id, c.member.id, c.parentCommentId, m.nickname, c.content, c.createdAt, c.updatedAt) " +
                "FROM CommentEntity c JOIN c.member m " +
                "WHERE c.post.id = :postId";

        return em.createQuery(sql, ResponseCommentDto.class).setParameter("postId", postId).getResultList();
    }

    public List<ResponseCommentDto> getReplies(Long postId) {
        String sql = "SELECT new com.example.firstproject.domain.dto.comment.ResponseCommentDto(c.id, c.post.id, c.member.id, c.parentCommentId, m.nickname, c.content, c.createdAt, c.updatedAt) " +
                "FROM CommentEntity c JOIN c.member m " +
                "WHERE c.post.id = :postId AND c.parentCommentId IS NOT NULL";

        return em.createQuery(sql, ResponseCommentDto.class).setParameter("postId", postId).getResultList();
    }
}
