package com.example.intermediate.service;

import com.example.intermediate.controller.response.ResponseDto;
import com.example.intermediate.domain.*;
import com.example.intermediate.jwt.TokenProvider;
import com.example.intermediate.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final TokenProvider tokenProvider;
    private final LikePostRepository likePostRepository;
    private final LikeCoRepository likeCoRepository;
    private final LikeRecoRepository likeRecoRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RecommentRepository recommentRepository;

    @Transactional
    public ResponseDto<?> likePost(Long id, HttpServletRequest request) {
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

        Post post = postRepository.findById(id).orElse(null);

        if (post == null){
            return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
        }

        LikePost likePost = likePostRepository.findLikePostByMemberAndPostId(member,id).orElse(null);

        if (likePost == null){
            LikePost saveLikePost = LikePost.builder()
                    .member(member)
                    .postId(id)
                    .build();
            likePostRepository.save(saveLikePost);
            post.update(1);
            return ResponseDto.success("post like success");
        } else {
            likePostRepository.delete(likePost);
            post.update(-1);
            return ResponseDto.success("successfully deleted post like");
        }

    }

    @Transactional
    public ResponseDto<?> likeComment(Long id, HttpServletRequest request) {
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


        Comment comment =  commentRepository.findById(id).orElse(null);

        if (comment == null){
            return ResponseDto.fail("NOT_FOUND", "존재하지 않는 댓글 id 입니다.");
        }

        LikeCo likeComment = likeCoRepository.findLikeCoByMemberAndCommentId(member,id).orElse(null);

        if (likeComment == null){
            LikeCo saveLikeComment = LikeCo.builder()
                    .member(member)
                    .commentId(id)
                    .build();
            likeCoRepository.save(saveLikeComment);
            comment.update(1);
            return ResponseDto.success("comment like success");
        } else {
            likeCoRepository.delete(likeComment);
            comment.update(-1);
            return ResponseDto.success("successfully deleted comment like");
        }
    }

    @Transactional
    public ResponseDto<?> likeRecomment(Long id, HttpServletRequest request) {
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


        Recomment recomment =  recommentRepository.findById(id).orElse(null);

        if (recomment == null){
            return ResponseDto.fail("NOT_FOUND", "존재하지 않는 대댓글 id 입니다.");
        }

        LikeReco likeReco = likeRecoRepository.findLikePostByMemberAndRecommentId(member,id).orElse(null);

        if (likeReco == null){
            LikeReco saveLikeRecomment = LikeReco.builder()
                    .member(member)
                    .recommentId(id)
                    .build();
            likeRecoRepository.save(saveLikeRecomment);
            recomment.update(1);
            return ResponseDto.success("recomment like success");
        } else {
            likeRecoRepository.delete(likeReco);
            recomment.update(-1);
            return ResponseDto.success("successfully deleted recomment like");
        }
    }


    @Transactional
    public Member validateMember(HttpServletRequest request) {
        if (!tokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
            return null;
        }
        return tokenProvider.getMemberFromAuthentication();
    }
}
