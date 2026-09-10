package com.team2.postservice.post;

import com.team2.postservice.post.entity.*;
import com.team2.postservice.post.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class NearbyRepairRequestQueryTest {
    @Autowired TestEntityManager em;
    @Autowired PostRepository repository;

    Post post(String region, boolean visible, PostStatus status, PostCategory category) {
        var post = Post.builder().title("제품 수리").content("전원이 켜지지 않음").authorEmail("owner@example.com")
                .regionCode(region).regionName("같은 표시 이름").category(category).build();
        post.changeVisibility(visible);
        org.springframework.test.util.ReflectionTestUtils.setField(post, "status", status);
        return em.persist(post);
    }

    @Test void filtersRegionVisibilityAndStatusBeforeCountingAndPaging() {
        var category = PostCategory.LIVING_ETC;
        var first = post("A", true, PostStatus.WAITING, category);
        var second = post("A", true, PostStatus.WAITING, category);
        second.addImage("/images/first.jpg", "first.jpg");
        second.addImage("/images/second.jpg", "second.jpg");
        post("B", true, PostStatus.WAITING, category);
        post("A", false, PostStatus.WAITING, category);
        post("A", true, PostStatus.MATCHED, category);
        post("A", true, PostStatus.COMPLETED, category);
        post(null, true, PostStatus.WAITING, category);
        em.flush();
        // A tied timestamp must still give stable, non-overlapping pages.
        em.getEntityManager().createQuery("update Post p set p.createdAt = :time")
                .setParameter("time", java.time.LocalDateTime.of(2026, 9, 10, 0, 0)).executeUpdate();
        em.clear();
        var sort = Sort.by(Sort.Direction.DESC, "createdAt", "id");
        var page0 = repository.findNearby("A", null, PageRequest.of(0, 1, sort));
        var page1 = repository.findNearby("A", null, PageRequest.of(1, 1, sort));
        assertThat(page0.getContent()).extracting(p -> p.id()).containsExactly(second.getId());
        assertThat(page1.getContent()).extracting(p -> p.id()).containsExactly(first.getId());
        assertThat(page0.getTotalElements()).isEqualTo(2);
        assertThat(page0.getContent().getFirst().thumbnailUrl()).isEqualTo("/images/first.jpg");
        assertThat(page1.getContent().getFirst().thumbnailUrl()).isNull();
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page1.isLast()).isTrue();
        assertThat(repository.findNearby("A", null, PageRequest.of(2, 1, sort))).isEmpty();
        assertThat(repository.findNearby("C", null, PageRequest.of(0, 20))).isEmpty();
    }

    @Test void categoryFilterAndDetailFields() {
        var post = post("A", true, PostStatus.WAITING, PostCategory.LIVING_ETC);
        post("A", true, PostStatus.WAITING, PostCategory.PLUMBING);
        em.flush(); em.clear();
        var result = repository.findNearby("A", PostCategory.LIVING_ETC, PageRequest.of(0, 20));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(post.getId());
        var detail = com.team2.postservice.post.dto.PostResponseDto.Detail.from(repository.findById(post.getId()).orElseThrow());
        assertThat(detail.title()).isEqualTo("제품 수리");
        assertThat(detail.content()).isEqualTo("전원이 켜지지 않음");
        assertThat(detail.regionCode()).isEqualTo("A");
        assertThat(detail.regionName()).isEqualTo("같은 표시 이름");
    }

    @Test void broaderScopesIncludeOtherDongsAndDistrictsButNeverOtherProvinces() {
        var sameDong = post("1165053100", true, PostStatus.WAITING, PostCategory.LIVING_ETC);
        var sameDistrict = post("1165051000", true, PostStatus.WAITING, PostCategory.LIVING_ETC);
        var sameProvince = post("1168051000", true, PostStatus.WAITING, PostCategory.LIVING_ETC);
        post("2611051000", true, PostStatus.WAITING, PostCategory.LIVING_ETC);
        post("1168052000", false, PostStatus.WAITING, PostCategory.LIVING_ETC);
        post("1168053000", true, PostStatus.MATCHED, PostCategory.LIVING_ETC);
        post("1168054000", true, PostStatus.COMPLETED, PostCategory.LIVING_ETC);
        em.flush(); em.clear();
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));
        assertThat(repository.findNearby("11%", null, pageable).getContent()).extracting(p -> p.id())
                .containsExactly(sameProvince.getId(), sameDistrict.getId(), sameDong.getId());
        assertThat(repository.findNearby("11650%", null, pageable).getContent()).extracting(p -> p.id())
                .containsExactly(sameDistrict.getId(), sameDong.getId());
        assertThat(repository.findNearby("1165053100", null, pageable).getContent()).extracting(p -> p.id())
                .containsExactly(sameDong.getId());
        var first = repository.findNearby("11%", null, PageRequest.of(0, 1, pageable.getSort()));
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(3);
    }

    @Test void guestQueryIncludesAllRegionsAndLegacyRequestsWithPagingAndCategoryFilter() {
        var seoul = post("1165053100", true, PostStatus.WAITING, PostCategory.LIVING_ETC);
        var busan = post("2611051000", true, PostStatus.WAITING, PostCategory.LIVING_ETC);
        var legacy = post(null, true, PostStatus.WAITING, PostCategory.PLUMBING);
        post("1165053100", false, PostStatus.WAITING, PostCategory.LIVING_ETC);
        post("2611051000", true, PostStatus.MATCHED, PostCategory.LIVING_ETC);
        post(null, true, PostStatus.COMPLETED, PostCategory.LIVING_ETC);
        em.flush(); em.clear();
        var sort = Sort.by(Sort.Direction.DESC, "id");
        var first = repository.findNearby(null, null, PageRequest.of(0, 2, sort));
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getContent()).extracting(p -> p.id()).containsExactly(legacy.getId(), busan.getId());
        assertThat(repository.findNearby(null, null, PageRequest.of(1, 2, sort)).getContent())
                .extracting(p -> p.id()).containsExactly(seoul.getId());
        assertThat(repository.findNearby(null, PostCategory.PLUMBING, PageRequest.of(0, 20)).getContent())
                .extracting(p -> p.id()).containsExactly(legacy.getId());
    }
}
