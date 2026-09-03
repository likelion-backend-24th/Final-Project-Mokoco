package com.team2.postservice.post.dto;

public class PostRequestDto {
    public record Create(String title, String content) {}
    public record Update(String title, String content) {}
}