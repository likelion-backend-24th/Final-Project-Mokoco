package com.team2.postservice.post;

import com.team2.common.security.LoginUser;
import com.team2.postservice.client.dto.RegionResponse;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.post.dto.PostRequestDto;
import com.team2.postservice.post.entity.*;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.post.service.*;
import com.team2.postservice.proposal.repository.ProposalRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PostServiceContentTest {
    @Test void createStoresOnlySanitizedHtmlAndItsFormat() {
        PostRepository posts = mock(PostRepository.class);
        PostViewerService viewers = mock(PostViewerService.class);
        when(viewers.requireRegion(7L)).thenReturn(new RegionResponse("11", "서울", "서울", null, null));
        when(posts.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PostService service = new PostService(posts, mock(FileStorageService.class), viewers,
                mock(ProposalRepository.class), mock(FixDealRepository.class));

        LoginUser loginUser = new LoginUser(7L, "test");

        service.createPost(new PostRequestDto.Create("수리", "<p onclick=\"x()\">고장</p><script>x()</script>",
                PostCategory.LIVING_ETC, ContentFormat.HTML), null, loginUser);

        ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
        verify(posts).save(saved.capture());
        assertThat(saved.getValue().getContent()).isEqualTo("<p>고장</p>");
        assertThat(saved.getValue().getContentFormat()).isEqualTo(ContentFormat.HTML);
    }
}
