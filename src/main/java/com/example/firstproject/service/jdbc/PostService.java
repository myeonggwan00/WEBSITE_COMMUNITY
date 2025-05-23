package com.example.firstproject.service.jdbc;

import com.example.firstproject.domain.dto.*;
import com.example.firstproject.domain.dto.post.PostDetails;
import com.example.firstproject.domain.dto.post.PostDto;
import com.example.firstproject.domain.jdbc.File;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.domain.jdbc.Post;
import com.example.firstproject.repository.CommentRepository;
import com.example.firstproject.repository.FileRepository;
import com.example.firstproject.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final FileRepository fileRepository;

    /**
     * 게시글에서 수정된 내용이 있는지 확인
     */
    public void checkPost(Long postId, PostDto postDto, BindingResult bindingResult) {
        Post post = postRepository.findById(postId);
        List<File> files = fileRepository.findByPostId(postId);
        List<String> submittedFileNames = postDto.getFileNames();
        List<String> fileNames = new ArrayList<>();
        List<MultipartFile> newFiles = postDto.getFiles();

        if (submittedFileNames == null) {
            submittedFileNames = new ArrayList<>();
        }

        for (String submittedFileName : submittedFileNames) {
            if(submittedFileName != null) {
                fileNames.add(submittedFileName);
            }
        }

        boolean isTitleUnchanged = post.getTitle().equals(postDto.getTitle());

        boolean isContentUnchanged = post.getContent().equals(postDto.getContent());

        List<String> originalFileNames = files.stream().map(File::getFileName).collect(Collectors.toList());

        Collections.sort(fileNames);
        Collections.sort(originalFileNames);

        boolean isFileListUnchanged = originalFileNames.equals(submittedFileNames);

        boolean hasNoFileUpload = true;

        for (MultipartFile file : newFiles) {
            if (file != null && !file.isEmpty() && file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()) {
                hasNoFileUpload = false;
                break;
            }
        }

        if(isTitleUnchanged && isContentUnchanged && isFileListUnchanged && hasNoFileUpload) {
            bindingResult.reject("noChange", "변경된 사항이 없습니다.");
        }
    }

    /**
     * 게시판 페이징 처리를 위해서 PageHandler 생성 및 반환하는 메서드
     */
    public PageHandler getPageHandler(SearchCondition sc, Integer page, Integer pageSize, Member member) {
        return new PageHandler(postRepository.getMyCountBySearchCondition(sc, member), page, pageSize);
    }

    /**
     * 게시판 페이징 처리를 위해서 PageHandler 생성 및 반환하는 메서드
     */
    public PageHandler getPageHandler(SearchCondition sc, Integer page, Integer pageSize) {
        return new PageHandler(postRepository.getCountBySearchCondition(sc), page, pageSize);
    }

    /**
     * 게시판 페이징 처리하는데 필요한 페이지 정보를 반환하는 메서드
     * - 페이지 정보란 예를 들어서 MySQL의 OFFSET, LIMIT에 대한 정보를 의미
     */
    public Map<String, Integer> getPageInfo(Integer page, Integer pageSize) {
        Map<String, Integer> map = new HashMap<>();

        map.put("offset", (page - 1) * pageSize);
        map.put("pageSize", pageSize);

        return map;
    }

    /**
     * 검색 조건에 해당하는 나의 게시글 목록에 페이징 처리하여 반환
     */
    public List<PostDetails> getPagedMyPosts(Map<String, Integer> pageInfo, SearchCondition sc, Member member) {
        return postRepository.getPagedMyPostsBySearchCondition(pageInfo, sc, member);
    }

    /**
     * 검색 조건으로 얻은 게시글 목록과 페이지 정보를 이용해서 페이징 처리된, 즉 해당 페이지에 존재하는 게시글 리스트를 반환하는 메서드
     */
    public List<PostDetails> getPagedPosts(Map<String, Integer> pageInfo, SearchCondition sc) {
        return postRepository.getPagedPostsBySearchCondition(pageInfo, sc);
    }

    /**
     * 게시글을 저장하는 메서드
     */
    public Post savePostFromDto(PostDto postDto, Member loginMember) {
        return Post.builder()
                .title(postDto.getTitle())
                .content(postDto.getContent())
                .memberId(loginMember.getId())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void savePostWithFiles(Post post, List<File> files) {
        postRepository.savePostWithFiles(post, files);
    }

    /**
     * 게시글에 대한 정보를 얻어오는 메서드
     */
    public PostDto getPostInfo(Long postId) {
        Post post = postRepository.findById(postId);
        List<File> byPostId = fileRepository.findByPostId(postId);

        return PostDto.builder()
                .id(postId)
                .memberId(post.getMemberId())
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .fileNames(byPostId.stream().map(File::getFileName).toList())
                .build();
    }

    /**
     * 게시글의 조회수를 증가시키는 메서드
     */
    public void increaseViewCnt(Long postId) {
        postRepository.updateViewCnt(postId);
    }

    /**
     * 게시글을 수정하는 메서드
     */
    public void editPost(Long postId, PostDto updatedPost) {
        updatedPost.updateLastUpdatedAt();
        postRepository.updateById(postId, updatedPost);
    }

    /**
     * 게시글을 삭제하는 메서드
     */
    public void deletePost(Long postId) {
        commentRepository.deleteByPostId(postId);
        fileRepository.deleteByPostId(postId);
        postRepository.deleteById(postId);
    }

    /**
     * 페이지를 검사하는 메서드
     * 예를 들어서 16페이지에 게시글이 하나 존재한다고 가정해보면?
     * 해당 게시글을 삭제하면 15페이지로 가야지 사용자가 더 편리하고 프로그램상 맞다.
     * 즉, 해당 게시글을 삭제하면 16페이지에는 아무 게시글이 없는데 해당 페이지를 보여줄 필요가 없다는 말이다.
     * 이러한 점을 방지하기 위해 해당 메서드를 도입했다.
     */
    public Integer checkPage(Integer page) {
        if(postRepository.getCountAll() - 1 < page * 10 - 9)
            return page == 1 ? page : page - 1;

        return page;
    }

    /**
     * 페이지를 구하는 메서드
     * 게시글 작성할 때 마다 맨 첫 번쩨 페이지로 가는 것이 불편해서 해당 메서드 도입
     * 해당 메서드를 도입함으로써 게시글을 작성했을 때 작성한 게시글이 존재하는 게시판 페이지로 이동이 가능해졌다.
     * 이전에는 게시글 작성시 무조건 첫 번째 게시판 페이지로 이동하였다.
     */
    public Integer findPage(String path, Member member) {
        int totalCnt = postRepository.getCountAll();

        if("/my/posts".equals(path)) {
            totalCnt = postRepository.getCountByWriter(member.getNickname());
        }

        if(totalCnt != 0) {
            if(totalCnt % 10 == 0) {
                return totalCnt / 10;
            } else {
                return totalCnt / 10 + 1;
            }
        } else {
            return 1;
        }
    }
}
