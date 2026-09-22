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
import com.team2.postservice.proposal.repository.ProposalRepository;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
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
    private final ProposalRepository proposalRepository;
    private final FixDealRepository fixDealRepository;

    @Transactional
    public Long createPost(PostRequestDto.Create request, List<MultipartFile> images, Long authorId) {
        // 사용자 ID로 최신 지역 정보를 조회한다.
        RegionResponse response = postViewerService.requireRegion(authorId);
        String content = PostContent.sanitize(request.content(), request.contentFormat());

        Post post = Post.builder()
                .title(request.title())
                .content(content)
                .contentFormat(request.contentFormat())
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

        post.update(request.title(), PostContent.sanitize(request.content(), request.contentFormat()),
                request.contentFormat(), request.category());
    }

    @Transactional
    public void deletePost(Long id, Long userId) {
        Post post = getPostOrThrow(id);
        validateAuthor(post, userId, ErrorCode.UNAUTHORIZED_POST_DELETE);
        guardNoActiveDeal(post.getId());
        deletePostInternal(post);
    }

    // 관리자는 작성자가 아니어도 삭제 가능 — 관리자 권한은 requireAdmin에서 매 요청 다시 검증한다.
    // 단, 진행 중인 거래가 있으면 관리자도 삭제 불가(문제 있는 유저는 글 삭제 대신 계정 정지로 처리).
    @Transactional
    public void deletePostAsAdmin(Long id, com.team2.common.security.LoginUser admin) {
        postViewerService.requireAdmin(admin);
        Post post = getPostOrThrow(id);
        guardNoActiveDeal(post.getId());
        deletePostInternal(post);
    }

    // 매칭~완료대기 사이(진행 중)인 거래가 있으면 삭제를 막는다. 완료/취소된 거래는 이미 끝난 일이라 허용.
    // (fix_deals는 posts와 실제 DB 외래키가 없어서 그냥 두면 에러 없이 삭제되지만, 그러면 두 당사자가
    //  주고받던 채팅/거래 맥락이 붕 뜬 채로 남으므로 정책적으로 막는다.)
    private void guardNoActiveDeal(Long postId) {
        fixDealRepository.findByPostId(postId).ifPresent(deal -> {
            if (deal.getStatus() != FixDealStatus.COMPLETED && deal.getStatus() != FixDealStatus.CANCELED) {
                throw new CustomException(ErrorCode.POST_HAS_ACTIVE_DEAL);
            }
        });
    }

    private void deletePostInternal(Post post) {
        List<String> storedFileNames = post.getImages().stream()
                .map(PostImage::getStoredFileName)
                .toList();

        // proposals.post_id -> posts.id 외래키 때문에, 제안이 하나라도 달려있으면 글 삭제가
        // DataIntegrityViolationException으로 막힌다. post_images는 JPA cascade(orphanRemoval)로
        // 알아서 지워지지만 Proposal은 Post와 JPA 연관관계가 없어서 직접 지워줘야 한다.
        proposalRepository.deleteAll(proposalRepository.findByPost(post));

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
