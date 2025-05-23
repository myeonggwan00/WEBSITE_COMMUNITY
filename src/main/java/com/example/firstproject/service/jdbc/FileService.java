package com.example.firstproject.service.jdbc;

import com.example.firstproject.domain.dto.post.PostDto;
import com.example.firstproject.domain.jdbc.File;
import com.example.firstproject.domain.jdbc.Member;
import com.example.firstproject.domain.jdbc.Post;
import com.example.firstproject.repository.FileRepository;
import com.example.firstproject.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;
    private final PostRepository postRepository;

    public List<File> uploadFile(PostDto postDto) {
        return saveFiles(postDto);
    }

    public void updateFile(Long postId, PostDto postDto) {
        /* 추가 로직 */
        List<String> fileNames = postDto.getFileNames();
        List<File> files = fileRepository.findByPostId(postId);


        if(fileNames == null || fileNames.isEmpty()) {
            fileRepository.deleteByPostId(postId);
        } else {
            // 삭제 대상 파일 추출
            List<File> filesToDelete = files.stream()
                    .filter(file -> !fileNames.contains(file.getFileName()))
                    .toList();

            for (File file : filesToDelete) {
                fileRepository.deleteById(file.getId());
            }
        }

        List<File> fileList = saveFiles(postDto);

        for (File file : fileList) {
            file.uploadFile(postId);
            fileRepository.save(file);
        }
    }

    public void checkFile(PostDto postDto, BindingResult bindingResult) {
        List<String> fileNames = postDto.getFileNames();

        for (MultipartFile file : postDto.getFiles()) {
            String originalFilename = file.getOriginalFilename();

            if(originalFilename != null && fileNames != null && fileNames.contains(originalFilename)) {
                bindingResult.rejectValue("files", "duplicate", "이미 업로드된 파일입니다.");
                break;
            }
        }
    }

    private List<File> saveFiles(PostDto postDto) {
        List<MultipartFile> files = postDto.getFiles();
        List<File> fileList = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                if(file == null || file.isEmpty()) {
                    break;
                }

                String filePath = Paths.get("/Users/seomyeong-gwan/Desktop/first-project/", file.getOriginalFilename()).toString();

                file.transferTo(new java.io.File(filePath));

                fileList.add(File.builder()
                        .postId(postDto.getId())
                        .filePath(filePath)
                        .fileName(file.getOriginalFilename())
                        .uploadedAt(LocalDateTime.now())
                        .build());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return fileList;
    }

    public void deleteFile(Long postId, String fileName) {
        fileRepository.deleteByPostIdAndFileName(postId, fileName);
    }

    public Post savePost(PostDto postDto, Member loginMember) {
        return Post.builder()
                .title(postDto.getTitle())
                .content(postDto.getContent())
                .memberId(loginMember.getId())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public ResponseEntity<Resource> downloadFile(String filename) throws IOException {
        // 서버에 존재하는 파일의 위치, 즉 서버가 클라이언트에게 줄 파일이 서버에 어디에 있는지 지정하는 경로
        Path path = Paths.get("/Users/seomyeong-gwan/Desktop/first-project/", filename);
        // 스프링에서는 파일, URL, 클래스패스 리소스 등을 일관되게 다룰 수 있도록 추상 인터페이스 Resource 제공
        Resource resource = new FileSystemResource(path);

        // 파일이 실제로 존재하지 않으면 404 응답
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        // 파일의 MIME 타입을 자동 감지(probeContentType)
        // Files.probeContentType(path)는 OS 및 파일 확장자를 기반으로 MIME 타입을 추측
        String contentType = Files.probeContentType(path);
        // 그런데 확장자가 이상하거나 OS가 MIME 타입 감지를 지원하지 않는 경우 null 반환(실퍠)하므로 감지 실패시 기본 타입으로 설정
        contentType = contentType != null ? contentType : "application/octet-stream";

        // URLEncoder - 공백문자를 +로 인코딩
        // HTTP Header - +가 공백이 아니라 문자 +로 해석될 수 있으므로 +를 정확한 공백 의미인 %20으로 변경
        String encodedFilename = URLEncoder.encode(resource.getFilename(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        // 파일 리소스를 HTTP 응답으로 반환
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))  // Content-Type 헤더 설정
                // Content-Disposition 헤더 설정
                // filename*=UTF-8'' : 브라우저가 UTF-8 인코딩 파일명을 제대로 해석하게 함
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);  // 파일 내용을 응답 본문에 담음
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId);
    }
}
