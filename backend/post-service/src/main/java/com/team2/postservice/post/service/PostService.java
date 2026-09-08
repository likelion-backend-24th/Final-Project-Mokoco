package com.team2.postservice.post.service;

import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.RegionResponse;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.post.dto.PostRequestDto;
import com.team2.postservice.post.dto.PostResponseDto;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostCategory;
import com.team2.postservice.post.entity.PostImage;
import com.team2.postservice.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private static final int MAX_IMAGES_PER_POST = 5;

    private final PostRepository postRepository;
    private final FileStorageService fileStorageService;
    private final UserClient userClient; // 💡 OpenFeign 클라이언트 주입

    @Transactional
    public Long createPost(PostRequestDto.Create request, List<MultipartFile> images, String authorEmail) {
        // 💡 User-Service에서 이메일로 최신 지역 정보를 Feign을 통해 조회
        RegionResponse response = userClient.getRegionByEmail(authorEmail);

        System.out.println("조회된 지역 이름: " + response.regionName());

        Post post = Post.builder()
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .authorEmail(authorEmail)
                .regionName(response.regionName())
                .build();

        attachImages(post, images);

        return postRepository.save(post).getId();
    }

    public List<PostResponseDto.Detail> getAllPosts(PostCategory category, String regionName) {
        List<Post> posts;

        boolean hasRegion = regionName != null && !regionName.isBlank();
        boolean hasCategory = category != null && category != PostCategory.ALL;

        Sort sort = Sort.by(Sort.Direction.DESC, "id");

        if (hasRegion && hasCategory) {
            posts = postRepository.findByRegionNameAndCategory(regionName, category, sort);
        } else if (hasRegion) {
            posts = postRepository.findByRegionName(regionName, sort);
        } else if (hasCategory) {
            posts = postRepository.findByCategory(category, sort);
        } else {
            posts = postRepository.findAll(sort);
        }

        return posts.stream()
                .map(PostResponseDto.Detail::from)
                .toList();
    }

    public PostResponseDto.Detail getPost(Long id) {
        Post post = getPostOrThrow(id);
        return PostResponseDto.Detail.from(post);
    }

    @Transactional
    public void updatePost(Long id, PostRequestDto.Update request, String userEmail) {
        Post post = getPostOrThrow(id);
        validateAuthor(post, userEmail, ErrorCode.UNAUTHORIZED_POST_UPDATE);

        post.update(request.title(), request.content(), request.category());
    }

    @Transactional
    public void deletePost(Long id, String userEmail) {
        Post post = getPostOrThrow(id);
        validateAuthor(post, userEmail, ErrorCode.UNAUTHORIZED_POST_DELETE);

        List<String> storedFileNames = post.getImages().stream()
                .map(PostImage::getStoredFileName)
                .toList();

        postRepository.delete(post);
        storedFileNames.forEach(fileStorageService::delete);
    }

    @Transactional
    public PostResponseDto.Detail addImages(Long postId, List<MultipartFile> images, String userEmail) {
        Post post = getPostOrThrow(postId);
        validateAuthor(post, userEmail, ErrorCode.UNAUTHORIZED_POST_UPDATE);

        attachImages(post, images);

        return PostResponseDto.Detail.from(post);
    }

    @Transactional
    public void deleteImage(Long postId, Long imageId, String userEmail) {
        Post post = getPostOrThrow(postId);
        validateAuthor(post, userEmail, ErrorCode.UNAUTHORIZED_POST_UPDATE);

        PostImage image = post.getImages().stream()
                .filter(postImage -> postImage.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));

        post.removeImage(image);
        fileStorageService.delete(image.getStoredFileName());
    }

    private void attachImages(Post post, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        if (post.getImages().size() + images.size() > MAX_IMAGES_PER_POST) {
            throw new CustomException(ErrorCode.TOO_MANY_IMAGES);
        }

        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }
            FileStorageService.StoredFile stored = fileStorageService.store(image);
            post.addImage(stored.imageUrl(), stored.storedFileName());
        }
    }

    private Post getPostOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private void validateAuthor(Post post, String userEmail, ErrorCode errorCode) {
        if (!post.getAuthorEmail().equals(userEmail)) {
            throw new CustomException(errorCode);
        }
    }
}