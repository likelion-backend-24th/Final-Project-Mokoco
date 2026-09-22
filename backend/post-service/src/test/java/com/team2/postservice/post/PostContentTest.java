package com.team2.postservice.post;

import com.team2.common.exception.CustomException;
import com.team2.postservice.post.entity.ContentFormat;
import com.team2.postservice.post.service.PostContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PostContentTest {
    @Test void sanitizesHtmlAtTheServerBoundary() {
        String cleaned = PostContent.sanitize("""
                <h2 onclick="steal()">전원 고장</h2><p>버튼을 눌러도 <strong>켜지지 않아요</strong>.</p>
                <script>alert(1)</script><a href="javascript:alert(2)" style="color:red">링크</a>
                """, ContentFormat.HTML);

        assertThat(cleaned).contains("<h2>전원 고장</h2>", "<strong>켜지지 않아요</strong>");
        assertThat(cleaned).doesNotContain("script", "onclick", "javascript:", "style=");
    }

    @Test void rejectsHtmlWithoutActualText() {
        assertThatThrownBy(() -> PostContent.sanitize("<p><br></p><script>alert(1)</script>", ContentFormat.HTML))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> PostContent.sanitize("<p>&nbsp;</p>", ContentFormat.HTML))
                .isInstanceOf(CustomException.class);
    }
}
