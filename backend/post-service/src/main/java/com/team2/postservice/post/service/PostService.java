package com.team2.postservice.post.service;

import com.team2.common.exception.CustomException;
import com.team2.postservice.client.dto.RegionResponse;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.post.dto.NearbyRepairRequest;
import com.team2.postservice.post.dto.PostRequestDto;
import com.team2.postservice.post.dto.PostResponseDto;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostCategory;
import com.team2.postservice.post.entity.PostImage;
import com.team2.postservice.post.entity.RegionScope;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private static final int MAX_IMAGES_PER_POST = 5;

    private final PostRepository postRepository;
    private final FileStorageService fileStorageService;
    private final PostViewerService postViewerService;
    private final ProposalRepository proposalRepository;
    private final FixDealRepository fixDealRepository;

    @Transactional
    public Long createPost(
            PostRequestDto.Create request,
            List<MultipartFile> images,
            String authorEmail
    ) {
        RegionResponse response = postViewerService.requireRegion(authorEmail);

        String content = PostContent.sanitize(
                request.content(),
                request.contentFormat()
        );

        Post post = Post.builder()
                .title(request.title())
                .content(content)
                .contentFormat(request.contentFormat())
                .category(request.category())
                .authorEmail(authorEmail)
                .regionName(response.regionName())
                .regionCode(response.regionCode())
                .build();

        attachImages(post, images);

        return postRepository.save(post).getId();
    }

    public NearbyRepairRequest.Result getNearbyPosts(
            String email,
            PostCategory category,
            int page,
            int size,
            RegionScope regionScope
    ) {
        if (page < 0
                || size < 1
                || size > 100
                || (long) page * size > Integer.MAX_VALUE) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        var region = email == null
                ? null
                : regionScope == RegionScope.ALL
                  ? postViewerService.tryRegion(email)
                  : postViewerService.requireRegion(email);

        String regionPattern = regionScope == RegionScope.ALL
                ? null
                : regionScope.queryPattern(region.regionCode());

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );

        var posts = postRepository.findNearby(
                regionPattern,
                category == PostCategory.ALL ? null : category,
                pageable
        );

        var nicknameCache = new HashMap<String, String>();

        posts = posts.map(item ->
                item.withAuthorNickname(
                        nicknameCache.computeIfAbsent(
                                item.authorEmail(),
                                postViewerService::tryNickname
                        )
                )
        );

        return NearbyRepairRequest.Result.from(
                posts,
                regionScope,
                region
        );
    }

    public PostResponseDto.Detail getPost(Long id) {
        Post post = getPostOrThrow(id);

        if (!post.isPubliclyVisible()) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        return PostResponseDto.Detail.from(
                post,
                postViewerService.tryNickname(post.getAuthorEmail())
        );
    }

    @Transactional
    public void changeVisibility(
            Long id,
            boolean publiclyVisible,
            String email
    ) {
        Post post = getPostOrThrow(id);

        validateAuthor(
                post,
                email,
                ErrorCode.UNAUTHORIZED_POST_UPDATE
        );

        post.changeVisibility(publiclyVisible);
    }

    @Transactional
    public void updatePost(
            Long id,
            PostRequestDto.Update request,
            String userEmail
    ) {
        Post post = getPostOrThrow(id);

        validateAuthor(
                post,
                userEmail,
                ErrorCode.UNAUTHORIZED_POST_UPDATE
        );

        String content = PostContent.sanitize(
                request.content(),
                request.contentFormat()
        );

        post.update(
                request.title(),
                content,
                request.contentFormat(),
                request.category()
        );
    }

    @Transactional
    public void deletePost(
            Long id,
            String userEmail
    ) {
        Post post = getPostOrThrow(id);

        validateAuthor(
                post,
                userEmail,
                ErrorCode.UNAUTHORIZED_POST_DELETE
        );

        guardNoActiveDeal(post.getId());
        deletePostInternal(post);
    }

    @Transactional
    public void deletePostAsAdmin(
            Long id,
            com.team2.common.security.LoginUser admin
    ) {
        postViewerService.requireAdmin(admin);

        Post post = getPostOrThrow(id);

        guardNoActiveDeal(post.getId());
        deletePostInternal(post);
    }

    private void guardNoActiveDeal(Long postId) {
        fixDealRepository.findByPostId(postId)
                .ifPresent(deal -> {
                    if (deal.getStatus() != FixDealStatus.COMPLETED
                            && deal.getStatus() != FixDealStatus.CANCELED) {
                        throw new CustomException(
                                ErrorCode.POST_HAS_ACTIVE_DEAL
                        );
                    }
                });
    }

    private void deletePostInternal(Post post) {
        List<String> storedFileNames = post.getImages()
                .stream()
                .map(PostImage::getStoredFileName)
                .toList();

        proposalRepository.deleteAll(
                proposalRepository.findByPost(post)
        );

        postRepository.delete(post);

        storedFileNames.forEach(
                fileStorageService::delete
        );
    }

    @Transactional
    public PostResponseDto.Detail addImages(
            Long postId,
            List<MultipartFile> images,
            String userEmail
    ) {
        Post post = getPostOrThrow(postId);

        validateAuthor(
                post,
                userEmail,
                ErrorCode.UNAUTHORIZED_POST_UPDATE
        );

        attachImages(post, images);

        return PostResponseDto.Detail.from(
                post,
                postViewerService.tryNickname(post.getAuthorEmail())
        );
    }

    @Transactional
    public void deleteImage(
            Long postId,
            Long imageId,
            String userEmail
    ) {
        Post post = getPostOrThrow(postId);

        validateAuthor(
                post,
                userEmail,
                ErrorCode.UNAUTHORIZED_POST_UPDATE
        );

        PostImage image = post.getImages()
                .stream()
                .filter(postImage ->
                        postImage.getId().equals(imageId)
                )
                .findFirst()
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.IMAGE_NOT_FOUND
                        )
                );

        post.removeImage(image);

        fileStorageService.delete(
                image.getStoredFileName()
        );
    }

    private void attachImages(
            Post post,
            List<MultipartFile> images
    ) {
        if (images == null || images.isEmpty()) {
            return;
        }

        if (post.getImages().size() + images.size()
                > MAX_IMAGES_PER_POST) {
            throw new CustomException(
                    ErrorCode.TOO_MANY_IMAGES
            );
        }

        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }

            FileStorageService.StoredFile stored =
                    fileStorageService.store(image);

            post.addImage(
                    stored.imageUrl(),
                    stored.storedFileName()
            );
        }
    }

    private Post getPostOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.POST_NOT_FOUND
                        )
                );
    }

    private void validateAuthor(
            Post post,
            String userEmail,
            ErrorCode errorCode
    ) {
        if (!post.getAuthorEmail().equals(userEmail)) {
            throw new CustomException(errorCode);
        }
    }
}