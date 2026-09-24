package com.team2.postservice.post.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

// 리치텍스트 에디터가 만들어내는 태그만 화이트리스트로 허용하고 나머지는 전부 벗겨낸다.
// content가 HTML로 저장/렌더링되므로(dangerouslySetInnerHTML), 여길 거치지 않은 HTML을
// 그대로 저장하면 저장형 XSS로 이어진다 — 반드시 저장 전에 이 필터를 통과시켜야 한다.
@Component
public class HtmlSanitizer {
    private static final Safelist SAFELIST = Safelist.none()
            .addTags("p", "br", "strong", "em", "u", "s", "h2", "h3", "ul", "ol", "li", "blockquote");

    public String sanitize(String html) {
        return html == null ? null : Jsoup.clean(html, SAFELIST);
    }
}
