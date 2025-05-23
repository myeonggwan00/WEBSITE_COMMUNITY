package com.example.firstproject.repository;

import com.example.firstproject.domain.dto.post.PostDetails;
import com.example.firstproject.domain.dto.post.PostDto;
import com.example.firstproject.domain.jdbc.File;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.domain.jdbc.Post;
import com.example.firstproject.domain.dto.SearchCondition;

import java.util.List;
import java.util.Map;

public interface PostRepository {
    void save(Post post);

    void savePostWithFiles(Post post, List<File> files);

    void updateById(Long id, PostDto updatePost);

    void updateViewCnt(Long id);

    Post findById(Long id);

    List<Post> findByMemberId(Long memberId);

    void deleteById(Long id);

    void deleteByMemberId(Long memberId);

    List<PostDetails> getPagedMyPostsBySearchCondition(Map<String, Integer> map, SearchCondition sc, Member member);

    List<PostDetails> findByTitle(Integer offset, Integer limit, String keyword, Long memberId);

    List<PostDetails> findByContent(Integer offset, Integer limit, String keyword, Long memberId);

    List<PostDetails> findAll(Integer offset, Integer limit, Long memberId);

    List<PostDetails> getPagedPostsBySearchCondition(Map<String, Integer> map, SearchCondition sc);

    List<PostDetails> findByTitle(Integer offset, Integer limit, String keyword);

    List<PostDetails> findByContent(Integer offset, Integer limit, String keyword);

    List<PostDetails> findByWriter(Integer offset, Integer limit, String keyword);

    List<PostDetails> findAll(Integer offset, Integer limit);

    int getMyCountBySearchCondition(SearchCondition sc, Member member);

    int getCountByContent(String keyword, Long memberId);

    int getCountByTitle(String keyword, Long memberId);

    int getCountAll(Long memberId);

    int getCountBySearchCondition(SearchCondition sc);

    int getCountByTitle(String keyword);

    int getCountByContent(String keyword);

    int getCountByWriter(String keyword);

    int getCountAll();
}
