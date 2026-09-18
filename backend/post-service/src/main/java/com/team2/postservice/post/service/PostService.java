package com.team2.postservice.post.service;

import com.team2.postservice.client.dto.RegionResponse;
import com.team2.postservice.common.exception.CustomException;
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
    public Long createPost(PostRequestDto.Create request, List<MultipartFile> images, String authorEmail) {
        // 💡 User-Service에서 이메일로 최신 지역 정보를 Feign을 통해 조회
        RegionResponse response = postViewerService.requireRegion(authorEmail);

        Post post = Post.builder()
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .authorEmail(authorEmail)
                .regionName(response.regionName())
                .regionCode(response.regionCode())
                .build();

        attachImages(post, images);

        return postRepository.save(post).getId();
    }

    public NearbyRepairRequest.Result getNearbyPosts(
            String authorization, PostCategory category, int page, int size, RegionScope regionScope) {
        String email = authorization == null || authorization.isBlank()
                ? null : postViewerService.requireEmail(authorization);
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
            throw new CustomException(ErrorCode.INVALID_INPUT);
        // ALL(기본값)은 활동 지역 설정 여부와 무관하게 필터링 없이 전체를 보여준다.
        // 다만 칩에 표시할 지역명은 있으면 보여주도록 best-effort로만 조회(없어도 에러 아님).
        var region = email == null ? null
                : regionScope == RegionScope.ALL ? postViewerService.tryRegion(email)
                : postViewerService.requireRegion(email);
        String regionPattern = regionScope == RegionScope.ALL ? null : regionScope.queryPattern(region.regionCode());
        var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var posts = postRepository.findNearby(regionPattern, category == PostCategory.ALL ? null : category, pageable);
        // 같은 페이지 안에서 작성자가 겹칠 수 있어(같은 사람의 여러 글), 이메일당 한 번만 조회하도록
        // 이 요청 범위에서만 쓰는 로컬 캐시를 사용한다(인스턴스 필드로 두면 요청 간에 공유되어 버그가 된다).
        var nicknameCache = new java.util.HashMap<String, String>();
        posts = posts.map(item -> item.withAuthorNickname(
                nicknameCache.computeIfAbsent(item.authorEmail(), postViewerService::tryNickname)));
        return NearbyRepairRequest.Result.from(posts, regionScope, region);
    }

    public PostResponseDto.Detail getPost(Long id) {
        Post post = getPostOrThrow(id);
        if (!post.isPubliclyVisible()) throw new CustomException(ErrorCode.POST_NOT_FOUND);
        return PostResponseDto.Detail.from(post, postViewerService.tryNickname(post.getAuthorEmail()));
    }

    @Transactional
    public void changeVisibility(Long id, boolean publiclyVisible, String authorization) {
        String email = postViewerService.requireEmail(authorization);
        Post post = getPostOrThrow(id);
        validateAuthor(post, email, ErrorCode.UNAUTHORIZED_POST_UPDATE);
        post.changeVisibility(publiclyVisible);
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
        guardNoActiveDeal(post.getId());
        deletePostInternal(post);
    }

    // 관리자는 작성자가 아니어도 삭제 가능 — authorization은 requireAdmin에서 매 요청 다시 검증한다.
    // 단, 진행 중인 거래가 있으면 관리자도 삭제 불가(문제 있는 유저는 글 삭제 대신 계정 정지로 처리).
    @Transactional
    public void deletePostAsAdmin(Long id, String authorization) {
        postViewerService.requireAdmin(authorization);
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
    public PostResponseDto.Detail addImages(Long postId, List<MultipartFile> images, String userEmail) {
        Post post = getPostOrThrow(postId);
        validateAuthor(post, userEmail, ErrorCode.UNAUTHORIZED_POST_UPDATE);

        attachImages(post, images);

        return PostResponseDto.Detail.from(post, postViewerService.tryNickname(post.getAuthorEmail()));
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
