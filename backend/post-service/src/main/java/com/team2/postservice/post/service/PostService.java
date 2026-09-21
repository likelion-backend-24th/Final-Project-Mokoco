package com.team2.postservice.post.service;

import com.team2.postservice.client.dto.RegionResponse;
import com.team2.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.post.dto.PostRequestDto;
import com.team2.postservice.post.dto.NearbyRepairRequest;
import org.springframework.data.domain.PageRequest;
import com.team2.postservice.post.dto.PostResponseDto;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostCategory;
import com.team2.postservice.post.entity.RegionScope;
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
    private final PostViewerService postViewerService;

    @Transactional
    public Long createPost(PostRequestDto.Create request, List<MultipartFile> images, Long authorId) {
        // 사용자 ID로 최신 지역 정보를 조회한다.
        RegionResponse response = postViewerService.requireRegion(authorId);

        Post post = Post.builder()
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .authorId(authorId)
                .regionName(response.regionName())
                .regionCode(response.regionCode())
                .build();

        attachImages(post, images);

        return postRepository.save(post).getId();
    }

    public NearbyRepairRequest.Result getNearbyPosts(
            Long viewerId, PostCategory category, int page, int size, RegionScope regionScope) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
            throw new CustomException(ErrorCode.INVALID_INPUT);
        RegionResponse region = viewerId == null ? null : postViewerService.requireRegion(viewerId);
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return NearbyRepairRequest.Result.from(
                postRepository.findNearby(region == null ? null : regionScope.queryPattern(region.regionCode()),
                        category == PostCategory.ALL ? null : category, pageable), regionScope, region);
    }

    public PostResponseDto.Detail getPost(Long id) {
        Post post = getPostOrThrow(id);
        if (!post.isPubliclyVisible()) throw new CustomException(ErrorCode.POST_NOT_FOUND);
        return PostResponseDto.Detail.from(post);
    }

    @Transactional
    public void changeVisibility(Long id, boolean publiclyVisible, Long viewerId) {
        Post post = getPostOrThrow(id);
        validateAuthor(post, viewerId, ErrorCode.UNAUTHORIZED_POST_UPDATE);
        post.changeVisibility(publiclyVisible);
    }

    @Transactional
    public void updatePost(Long id, PostRequestDto.Update request, Long userId) {
        Post post = getPostOrThrow(id);
        validateAuthor(post, userId, ErrorCode.UNAUTHORIZED_POST_UPDATE);

        post.update(request.title(), request.content(), request.category());
    }

    @Transactional
    public void deletePost(Long id, Long userId) {
        Post post = getPostOrThrow(id);
        validateAuthor(post, userId, ErrorCode.UNAUTHORIZED_POST_DELETE);

        List<String> storedFileNames = post.getImages().stream()
                .map(PostImage::getStoredFileName)
                .toList();

        postRepository.delete(post);
        storedFileNames.forEach(fileStorageService::delete);
    }

    @Transactional
    public PostResponseDto.Detail addImages(Long postId, List<MultipartFile> images, Long userId) {
        Post post = getPostOrThrow(postId);
        validateAuthor(post, userId, ErrorCode.UNAUTHORIZED_POST_UPDATE);

        attachImages(post, images);

        return PostResponseDto.Detail.from(post);
    }

    @Transactional
    public void deleteImage(Long postId, Long imageId, Long userId) {
        Post post = getPostOrThrow(postId);
        validateAuthor(post, userId, ErrorCode.UNAUTHORIZED_POST_UPDATE);

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

    private void validateAuthor(Post post, Long userId, ErrorCode errorCode) {
        if (!post.getAuthorId().equals(userId)) {
            throw new CustomException(errorCode);
        }
    }
}
