package com.example.firstproject.controller;

import com.example.firstproject.common.constant.SessionConst;
import com.example.firstproject.domain.dto.*;
import com.example.firstproject.domain.dto.comment.ResponseCommentDto;
import com.example.firstproject.domain.dto.comment.RequestCommentDto;
import com.example.firstproject.domain.dto.post.PostDetails;
import com.example.firstproject.domain.dto.post.PostDto;
import com.example.firstproject.domain.jdbc.File;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.domain.jdbc.Post;
import com.example.firstproject.domain.jpa.MemberEntity;
import com.example.firstproject.domain.jpa.PostEntity;
import com.example.firstproject.service.jdbc.CommentService;
import com.example.firstproject.service.jdbc.FileService;
import com.example.firstproject.service.jdbc.PostService;
import com.example.firstproject.service.jpa.JpaCommentService;
import com.example.firstproject.service.jpa.JpaFileService;
import com.example.firstproject.service.jpa.JpaPostService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PostController {
    /* JDBC */
//    private final PostService postService;
//    private final CommentService commentService;
//    private final FileService fileService;

    /* JPA */
    private final JpaPostService postService;
    private final JpaCommentService commentService;
    private final JpaFileService fileService;

    /**
     * ModelAttribute 어노테이션의 특별한 사용법
     * BoardController 내의 여러 메서드에서 SearchOption 객체를 생성해서 값을 설정하고 모델에 저장하는 코드가 사용된다.
     * 이러한 중복된 코드를 줄이기 위해서 중복된 코드를 별도의 메서드로 분리하고 @ModelAttribute 사용
     */
    @ModelAttribute("searchOptions")
    public List<SearchOption> searchOptions() {
        List<SearchOption> searchOptions = new ArrayList<>();

        searchOptions.add(new SearchOption("C", "내용"));
        searchOptions.add(new SearchOption("T", "제목"));
        searchOptions.add(new SearchOption("W", "작성자"));

        return searchOptions;
    }

    /**
     * 게시판 페이지 제공
     * 회원이 작성한 게시글 정보를 처리하기 위해서 게시글이 저장되어 있는 저장소에서 모든 정보를 얻고 저장소(Model)에 담아서 화면으로 전송한다.
     */
    @GetMapping("/posts")
    public String posts(@RequestParam(defaultValue = "1") Integer page,
                        @RequestParam(defaultValue = "10") Integer pageSize,
                        SearchCondition sc, Model model) {
        Map<String, Integer> map = postService.getPageInfo(page, pageSize);
        List<PostDetails> posts = postService.getPagedPosts(map, sc);
        PageHandler pageHandler = postService.getPageHandler(sc, page, pageSize);

        model.addAttribute("posts", posts);
        model.addAttribute("pageHandler", pageHandler);
        model.addAttribute("searchCondition", sc);
        model.addAttribute("page", page);
        model.addAttribute("pageSize", pageSize);

        return "post/posts";
    }

    /**
     * 내가 작성한 게시글 목록 제공
     */
    @GetMapping("/my/posts")
    @Transactional
    public String myPosts(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          SearchCondition sc, Model model,
                          @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) MemberEntity loginMember) {
        List<SearchOption> searchOptions = new ArrayList<>();

        searchOptions.add(new SearchOption("C", "내용"));
        searchOptions.add(new SearchOption("T", "제목"));

        Map<String, Integer> map = postService.getPageInfo(page, pageSize);
        List<PostDetails> posts = postService.getPagedMyPosts(map, sc, loginMember);
        PageHandler pageHandler = postService.getPageHandler(sc, page, pageSize, loginMember);

        model.addAttribute("searchOptions", searchOptions);
        model.addAttribute("posts", posts);
        model.addAttribute("pageHandler", pageHandler);
        model.addAttribute("searchCondition", sc);
        model.addAttribute("page", page);
        model.addAttribute("pageSize", pageSize);

        return "post/myPosts";
    }

    /**
     * 게시글을 작성할 수 있는 페이지 제공
     * 회원이 작성한 게시글 정보를 처리하기 위해서 비어있는 게시글 객체를 저장소(Model)에 저장해서 폼으로 넘겨준다.
     */
    @GetMapping("/posts/new")
    public String newPost(Model model, HttpServletRequest request) throws URISyntaxException {
        // 게시글 작성 화면에서 취소 버튼을 누르면 홈 화면이 아닌 이전 화면이 나오도록 하는 것이 사용자 입장에서 편하다.
        String referer = request.getHeader("Referer");

        URI uri = new URI(referer);

        String path = uri.getPath();  // 결과 예시: /board/my

        // 이전의 요청이 로그인인 경우 게시글 글쓰는 화면에 취소를 누르면 계속 이상하게 동작
        // 따라서 /login, /signup 경로로 요청시 /posts로 설정
        if (path.equals("/login") || path.equals("/signup")) {
            path = "/posts";
        }

        model.addAttribute("path", path);
        model.addAttribute("post", new PostDto());

        return "post/postForm";
    }

    /**
     * 회원이 작성한 게시글을 처리
     * 1. 회원이 작성한 게시글 정보 얻기 (@ModelAttribute 사용)
     * 2. 로그인되어 있는 회원 정보를 알아내기 위해서 세션에 저장되어 있는 정보를 얻어오기 (@SessionAttribute 사용)
     * 3. 로그인되어 있는 회원으로 게시판에 게시글 저장하기
     */
    @PostMapping("/posts/new")
    public String newPost(@RequestParam(defaultValue = "/posts") String prevUri,
                          @Validated @ModelAttribute("post") PostDto postDto, BindingResult bindingResult,
                          @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) MemberEntity loginMember) {

        if(bindingResult.hasErrors()) {
            return "post/postForm";
        }

        /* JDBC */
//        Post post = postService.savePostFromDto(postDto, loginMember);
//        List<File> files = fileService.uploadFile(postDto);
//        postService.savePostWithFiles(post, files);

        /* JPA */
        PostEntity post = fileService.uploadFile(postDto, loginMember);
        postService.savePost(post);

        return "redirect:" + prevUri + "?page=" + postService.findPage(prevUri, loginMember) + "&pageSize=10";
    }

    /**
     * 게시글에 업로드된 파일을 다운로드 처리
     */
    @GetMapping("/download/{filename}")
    // ResponseEntity<Resource>는 스프링에서 파일 다운로드 응답을 표준적으로 처리하는 방식
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) throws IOException {
        return fileService.downloadFile(filename);
    }

    /**
     * 게시글을 선택했을 때 해당 게시글 페이지 제공
     */
    @GetMapping("/posts/{postId}")
    public String selectPost(@PathVariable Long postId,
                             @RequestParam(required = false) String prevUri,
                             Model model, HttpServletRequest request, HttpServletResponse response,
                             @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) MemberEntity loginMember) {
        RequestCommentDto comment = new RequestCommentDto();

        PostDto post = postService.getPostInfo(postId);

        // 이미 조회한 게시글인지 확인
        Cookie[] cookies = request.getCookies();
        boolean isViewed = false;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("viewed_" + postId)) {
                    isViewed = true;
                    break;
                }
            }
        }

        // 처음 조회한 게시글일 떄만 조회수를 증가하도록 설정
        if (!isViewed) {
            postService.increaseViewCnt(postId);

            Cookie newCookie = new Cookie("viewed_" + postId, "true");
            newCookie.setMaxAge(60 * 10); // 5분간 유지
            response.addCookie(newCookie);
        }

        List<ResponseCommentDto> comments = commentService.getCommentsByPostId(postId);
        List<ResponseCommentDto> replies = commentService.getReplies(postId);

        model.addAttribute("prevUri", prevUri);
        model.addAttribute("post", post);
        model.addAttribute("comment", comment);
        model.addAttribute("comments", comments);
        model.addAttribute("replies", replies);
        model.addAttribute("loginMember", loginMember);

        return "post/post";
    }

    /**
     * 게시글 수정 페이지 제공
     */
    @GetMapping("/posts/{postId}/edit")
    public String editPost(@PathVariable Long postId, Model model, @RequestParam String prevUri) {
        PostDto post = postService.getPostInfo(postId);

        model.addAttribute("prevUri", prevUri);
        model.addAttribute("postId", postId);
        model.addAttribute("post", post);

        return "post/editPost";
    }

    /**
     * 게시글 수정 및 등록 작업을 처리
     */
    @PostMapping("/posts/{postId}/edit")
    public String editPost(@PathVariable Long postId,
                           @RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           @RequestParam(defaultValue = "/posts") String prevUri,
                           RedirectAttributes redirectAttributes, Model model,
                           @Validated @ModelAttribute("post") PostDto postDto, BindingResult bindingResult) {
        postService.checkPost(postId, postDto, bindingResult);
        fileService.checkFile(postDto, bindingResult);

        if(bindingResult.hasErrors()) {
            model.addAttribute("prevUri", prevUri);
            return "post/editPost";
        }

        redirectAttributes.addAttribute("postId", postId);
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("pageSize", pageSize);
        redirectAttributes.addAttribute("prevUri", prevUri);

        fileService.updateFile(postId, postDto);
        postService.editPost(postId, postDto);

        return "redirect:/posts/{postId}";
    }


    /**
     * 게시글을 삭제하는 작업을 처리
     */
    @PostMapping("/posts/{id}/delete")
    public String deleteBoard(@PathVariable Long id,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) MemberEntity loginMember) {
        Integer checkPage = postService.checkPage(page);

        postService.deletePost(id);

        String uri = "ADMIN".equals(loginMember.getRole().name()) ? "/admin/posts" : "/posts";

        return "redirect:" + uri + "?page="+ checkPage + "&pageSize=" + pageSize;
    }
}
