package com.team2.postservice.post;

import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.*;
import com.team2.postservice.common.exception.*;
import com.team2.postservice.post.entity.*;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.post.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NearbyRepairRequestServiceTest {
    @Mock UserClient users;
    @Mock PostRepository posts;
    PostViewerService viewer;
    PostService service;
    @BeforeEach void setup() {
        viewer = new PostViewerService(users);
        service = new PostService(posts, mock(FileStorageService.class), viewer);
    }
    void login() { when(users.verifyToken("valid")).thenReturn(new UserClientResponse(1L, "a@test.com", "a", "A")); }

    @Test void usesVerifiedUsersCurrentRegionAndReflectsChanges() {
        login();
        when(users.getRegionByEmail("a@test.com")).thenReturn(new RegionResponse("1165053100", "서울 서초구 서초4동", "서울특별시", "서초구", "서초4동"), new RegionResponse("2611051000", "부산 중구 중앙동", "부산광역시", "중구", "중앙동"));
        when(posts.findNearby(anyString(), isNull(), any())).thenReturn(Page.empty());
        service.getNearbyPosts("Bearer valid", PostCategory.ALL, 0, 20, RegionScope.SIDO);
        service.getNearbyPosts("Bearer valid", null, 0, 20, RegionScope.SIDO);
        verify(posts).findNearby(eq("11%"), isNull(), any());
        verify(posts).findNearby(eq("26%"), isNull(), any());
    }

    @Test void malformedAuthenticationIsNotTreatedAsGuest() {
        for (String header : new String[]{"Bearer ", "Basic fake"})
            assertThatThrownBy(() -> service.getNearbyPosts(header, null, 0, 20, RegionScope.SIDO))
                    .isInstanceOfSatisfying(CustomException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED));
        verifyNoInteractions(users, posts);
    }

    @Test void guestsQueryAllRegionsWithoutCallingUserService() {
        when(posts.findNearby(isNull(), isNull(), any())).thenReturn(Page.empty());
        for (String header : new String[]{null, "", " "}) {
            var result = service.getNearbyPosts(header, PostCategory.ALL, 0, 20, RegionScope.DONG);
            assertThat(result.regionFilter()).isNull();
        }
        verify(posts, times(3)).findNearby(isNull(), isNull(), any());
        verifyNoInteractions(users);
    }

    @Test void selectedScopeUsesAdministrativeCodeAndReturnsStructuredRegionLabels() {
        login();
        when(users.getRegionByEmail("a@test.com")).thenReturn(
                new RegionResponse("1165053100", "서울특별시 서초구 서초4동", "서울특별시", "서초구", "서초4동"));
        when(posts.findNearby(anyString(), isNull(), any())).thenReturn(Page.empty());
        var district = service.getNearbyPosts("Bearer valid", null, 0, 20, RegionScope.SIGUNGU);
        service.getNearbyPosts("Bearer valid", null, 0, 20, RegionScope.DONG);
        verify(posts).findNearby(eq("11650%"), isNull(), any());
        verify(posts).findNearby(eq("1165053100"), isNull(), any());
        assertThat(district.regionFilter().scope()).isEqualTo(RegionScope.SIGUNGU);
        assertThat(district.regionFilter().sido()).isEqualTo("서울특별시");
        assertThat(district.regionFilter().sigungu()).isEqualTo("서초구");
        assertThat(district.regionFilter().dong()).isEqualTo("서초4동");
    }

    @Test void malformedStoredCodesCannotBecomeBroadWildcardQueries() {
        for (String code : new String[]{"", "%", "11%", "A", "11650", "116505310"})
            assertThatThrownBy(() -> RegionScope.SIDO.queryPattern(code))
                    .isInstanceOf(CustomException.class);
        assertThat(RegionScope.SIGUNGU.queryPattern("11650531")).isEqualTo("11650%");
        assertThat(RegionScope.DONG.queryPattern("11650531")).isEqualTo("11650531");
    }

    @Test void invalidPagesAreRejected() {
        login();
        for (int[] args : new int[][]{{-1, 20}, {0, 0}, {0, 101}})
            assertThatThrownBy(() -> service.getNearbyPosts("Bearer valid", null, args[0], args[1], RegionScope.SIDO))
                    .isInstanceOfSatisfying(CustomException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(posts);
    }

    @Test void regionFailureNeverFallsBackToGlobalRequests() {
        login();
        when(users.getRegionByEmail("a@test.com")).thenThrow(failure(404));
        assertThatThrownBy(() -> service.getNearbyPosts("Bearer valid", null, 0, 20, RegionScope.SIDO))
                .isInstanceOfSatisfying(CustomException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_REGION_REQUIRED));
        doThrow(failure(503)).when(users).getRegionByEmail("a@test.com");
        assertThatThrownBy(() -> service.getNearbyPosts("Bearer valid", null, 0, 20, RegionScope.SIDO))
                .isInstanceOfSatisfying(CustomException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_SERVICE_UNAVAILABLE));
        verifyNoInteractions(posts);
    }

    @Test void invalidTokenAndAuthOutageAreDistinct() {
        when(users.verifyToken("bad")).thenThrow(failure(401));
        assertThatThrownBy(() -> viewer.requireEmail("Bearer bad"))
                .isInstanceOfSatisfying(CustomException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED));
        doThrow(failure(503)).when(users).verifyToken("bad");
        assertThatThrownBy(() -> viewer.requireEmail("Bearer bad"))
                .isInstanceOfSatisfying(CustomException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_SERVICE_UNAVAILABLE));
    }

    static feign.FeignException failure(int status) {
        return feign.FeignException.errorStatus("user", feign.Response.builder().status(status).reason("test")
                .request(feign.Request.create(feign.Request.HttpMethod.GET, "http://test", java.util.Map.of(), null, java.nio.charset.StandardCharsets.UTF_8, null)).build());
    }
}
