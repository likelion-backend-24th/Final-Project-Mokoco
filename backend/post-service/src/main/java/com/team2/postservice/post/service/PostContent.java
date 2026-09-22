package com.team2.postservice.post.service;

import com.team2.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.post.entity.ContentFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

public final class PostContent {
    private static final int MAX_HTML_LENGTH = 10_000;
    private static final int MAX_TEXT_LENGTH = 5_000;
    private static final Safelist ALLOWED = new Safelist()
            .addTags("p", "br", "strong", "b", "em", "i", "u", "h2", "h3", "ul", "ol", "li", "blockquote", "a")
            .addAttributes("a", "href", "rel")
            .addProtocols("a", "href", "http", "https");

    private PostContent() {}

    public static String sanitize(String value, ContentFormat format) {
        if (value == null) throw invalid();
        ContentFormat actualFormat = format == null ? ContentFormat.PLAIN_TEXT : format;
        if (actualFormat == ContentFormat.PLAIN_TEXT) {
            if (value.isBlank() || value.length() > MAX_TEXT_LENGTH) throw invalid();
            return value;
        }
        if (value.length() > MAX_HTML_LENGTH) throw invalid();

        Document.OutputSettings output = new Document.OutputSettings().prettyPrint(false);
        Document document = Jsoup.parseBodyFragment(Jsoup.clean(value, "", ALLOWED, output));
        document.outputSettings(output);
        document.select("a[href]").attr("rel", "nofollow noopener noreferrer");
        String cleaned = document.body().html();
        String text = document.text().replace('\u00A0', ' ').trim();
        if (text.isEmpty() || text.length() > MAX_TEXT_LENGTH || cleaned.length() > MAX_HTML_LENGTH) throw invalid();
        return cleaned;
    }

    public static String plainText(String value, ContentFormat format) {
        if (value == null) return "";
        return format == ContentFormat.HTML ? Jsoup.parseBodyFragment(value).text() : value;
    }

    private static CustomException invalid() {
        return new CustomException(ErrorCode.INVALID_INPUT);
    }
}
