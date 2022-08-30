package com.example.intermediate.controller.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecommentRequestDto {
    private Long postId;
    private Long commentId;
    private String content;
}