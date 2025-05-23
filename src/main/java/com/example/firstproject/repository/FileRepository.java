package com.example.firstproject.repository;

import com.example.firstproject.domain.jdbc.File;

import java.util.List;

public interface FileRepository {
    void save(File file);

    boolean checkFile(Long postId, String fileName);

    List<File> findByPostId(Long postId);

    void deleteById(Long id);

    void deleteByPostIdAndFileName(Long postId, String fileName);

    void deleteByPostId(Long postId);

    void deleteByMemberId(Long memberId);
}
