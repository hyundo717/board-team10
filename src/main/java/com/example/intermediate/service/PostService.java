package com.example.intermediate.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Value;
import com.example.intermediate.controller.response.*;
import com.example.intermediate.domain.Comment;
import com.example.intermediate.domain.Member;
import com.example.intermediate.domain.Post;
import com.example.intermediate.controller.request.PostWriteDto;
import com.example.intermediate.jwt.TokenProvider;
import com.example.intermediate.repository.CommentRepository;
import com.example.intermediate.repository.PostRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import com.example.intermediate.repository.RecommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PostService {

  private final PostRepository postRepository;
  private final CommentRepository commentRepository;
  private final RecommentRepository recommentRepository;

  private final TokenProvider tokenProvider;

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  @Value("${cloud.aws.s3.dir}")
  private String dir;

  private final AmazonS3Client s3Client;

//  @Transactional
//  public ResponseDto<?> createPost(PostWriteDto requestDto, HttpServletRequest request) {
//    if (null == request.getHeader("Refresh-Token")) {
//      return ResponseDto.fail("MEMBER_NOT_FOUND",
//          "로그인이 필요합니다.");
//    }
//
//    if (null == request.getHeader("Authorization")) {
//      return ResponseDto.fail("MEMBER_NOT_FOUND",
//          "로그인이 필요합니다.");
//    }
//
//    Member member = validateMember(request);
//    if (null == member) {
//      return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
//    }
//
//    Post post = Post.builder()
//        .title(requestDto.getTitle())
//        .content(requestDto.getContent())
//        .member(member)
//        .build();
//    postRepository.save(post);
//    return ResponseDto.success(
//        PostResponseDto.builder()
//            .id(post.getId())
//            .title(post.getTitle())
//            .content(post.getContent())
//            .author(post.getMember().getNickname())
//            .createdAt(post.getCreatedAt())
//            .modifiedAt(post.getModifiedAt())
//            .build()
//    );
//  }

  @Transactional
  public ResponseDto<?> createPost(MultipartFile multipartFile, PostWriteDto requestDto, HttpServletRequest request) throws IOException {
    System.out.println(requestDto.getTitle());
    System.out.println("create post");
    if (null == request.getHeader("Refresh-Token")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
              "로그인이 필요합니다.");
    }

    if (null == request.getHeader("Authorization")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
              "로그인이 필요합니다.");
    }

    Member member = validateMember(request);
    if (null == member) {
      return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
    }

    String imgUrl = getS3url(multipartFile);
    System.out.println(imgUrl);

    Post post = Post.builder()
            .title(requestDto.getTitle())
            .content(requestDto.getContent())
            .imgUrl(imgUrl)
            .member(member)
            .build();
    postRepository.save(post);

    return ResponseDto.success(
            PostResponseDto.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .content(post.getContent())
                    .author(post.getMember().getNickname())
                    .imgUrl(imgUrl)
                    .createdAt(post.getCreatedAt())
                    .modifiedAt(post.getModifiedAt())
                    .build()
    );
  }

  @Transactional(readOnly = true)
  public ResponseDto<?> getPost(Long id) {
    Post post = isPresentPost(id);
    if (null == post) {
      return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
    }

    List<Comment> commentList = commentRepository.findAllByPost(post);
    List<CommentResponseDto> commentResponseDtoList = new ArrayList<>();

    for (Comment comment : commentList) {
//      List<Recomment>  recommentList = recommentRepository.findAllByComment(comment);

      commentResponseDtoList.add(
          CommentResponseDto.builder()
              .id(comment.getId())
              .author(comment.getMember().getNickname())
              .content(comment.getContent())
              .createdAt(comment.getCreatedAt())
              .modifiedAt(comment.getModifiedAt())
              .build()
      );
    }

    return ResponseDto.success(
        PostResponseDto.builder()
            .id(post.getId())
            .title(post.getTitle())
            .content(post.getContent())
            .commentResponseDtoList(commentResponseDtoList)
            .author(post.getMember().getNickname())
            .createdAt(post.getCreatedAt())
            .modifiedAt(post.getModifiedAt())
            .build()
    );
  }

  @Transactional(readOnly = true)
  public ResponseDto<?> getAllPost() {
    List<Post> postList = postRepository.findAllByOrderByModifiedAtDesc();

    ListResponseProvider listResponse = new ListResponseProvider(commentRepository);

    return ResponseDto.success(listResponse.GetPostListResponse(postList));
  }

  @Transactional
  public ResponseDto<Post> updatePost(Long id, PostWriteDto requestDto, HttpServletRequest request) {
    if (null == request.getHeader("Refresh-Token")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
          "로그인이 필요합니다.");
    }

    if (null == request.getHeader("Authorization")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
          "로그인이 필요합니다.");
    }

    Member member = validateMember(request);
    if (null == member) {
      return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
    }

    Post post = isPresentPost(id);
    if (null == post) {
      return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
    }

    if (post.validateMember(member)) {
      return ResponseDto.fail("BAD_REQUEST", "작성자만 수정할 수 있습니다.");
    }

    post.update(requestDto);
    return ResponseDto.success(post);
  }

  @Transactional
  public ResponseDto<?> deletePost(Long id, HttpServletRequest request) {
    if (null == request.getHeader("Refresh-Token")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
          "로그인이 필요합니다.");
    }

    if (null == request.getHeader("Authorization")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
          "로그인이 필요합니다.");
    }

    Member member = validateMember(request);
    if (null == member) {
      return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
    }

    Post post = isPresentPost(id);
    if (null == post) {
      return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
    }

    if (post.validateMember(member)) {
      return ResponseDto.fail("BAD_REQUEST", "작성자만 삭제할 수 있습니다.");
    }

    postRepository.delete(post);
    return ResponseDto.success("delete success");
  }

  public String getS3url(MultipartFile multipartFile) throws IOException{

    InputStream inputStream = multipartFile.getInputStream();


    String originFileName = multipartFile.getOriginalFilename();

    String s3FileName = UUID.randomUUID() + "-" + originFileName;

    ObjectMetadata objMeta = new ObjectMetadata();
//    objMeta.setContentLength(Long.parseLong(String.valueOf(multipartFile.getSize())));
    objMeta.setContentLength(multipartFile.getSize());
    s3Client.putObject(bucket, s3FileName, inputStream, objMeta);

    return s3Client.getUrl(bucket, dir + s3FileName).toString();
  }

//  @Transactional
//  public ResponseDto<?> uploadFile(Long id, MultipartFile multipartFile, String fileSize, HttpServletRequest request) throws IOException {
//    if (null == request.getHeader("Refresh-Token")) {
//      return ResponseDto.fail("MEMBER_NOT_FOUND",
//              "로그인이 필요합니다.");
//    }
//
//    if (null == request.getHeader("Authorization")) {
//      return ResponseDto.fail("MEMBER_NOT_FOUND",
//              "로그인이 필요합니다.");
//    }
//
//    Post post = isPresentPost(id);
//    if (null == post) {
//      return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
//    }
//
//    InputStream inputStream = multipartFile.getInputStream();
//    String originFileName = multipartFile.getOriginalFilename();
//
//    String s3FileName = UUID.randomUUID() + "-" + originFileName;
//
//    ObjectMetadata objMeta = new ObjectMetadata();
//    objMeta.setContentLength(Long.parseLong(fileSize));
//    s3Client.putObject(bucket, s3FileName, inputStream, objMeta);
//
//    String imgUrl = s3Client.getUrl(bucket, dir + s3FileName).toString();
//
//  }


  @Transactional(readOnly = true)
  public Post isPresentPost(Long id) {
    Optional<Post> optionalPost = postRepository.findById(id);
    return optionalPost.orElse(null);
  }

  @Transactional
  public Member validateMember(HttpServletRequest request) {
    if (!tokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
      return null;
    }
    return tokenProvider.getMemberFromAuthentication();
  }

}
