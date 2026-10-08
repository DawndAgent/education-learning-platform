package com.xxedu.learning.common.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

public final class HtmlSanitizer {

    private static final Safelist SAFELIST = safelist();
    private static final Document.OutputSettings OUTPUT = new Document.OutputSettings().prettyPrint(false);

    private HtmlSanitizer() {
    }

    public static String clean(String html) {
        if (html == null) {
            return null;
        }
        String cleaned = Jsoup.clean(html, "https://xxedu.local", SAFELIST, OUTPUT);
        return cleaned.replaceAll("(?i)javascript\\s*:", "")
                .replaceAll("(?i)expression\\s*\\(", "")
                .replaceAll("(?i)<\\s*/?\\s*(script|iframe|object|embed)\\b[^>]*>", "")
                .replaceAll("(?i)\\son\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", "");
    }

    public static boolean hasText(String html) {
        if (html == null || html.isBlank()) {
            return false;
        }
        return !Jsoup.parse(html).text().isBlank();
    }

    private static Safelist safelist() {
        Safelist list = Safelist.relaxed();
        list.preserveRelativeLinks(true);
        list.addAttributes(":all", "style", "class");
        list.addAttributes("p", "data-video-id");
        list.addProtocols("a", "href", "http", "https", "mailto");
        list.addProtocols("img", "src", "http", "https");
        return list;
    }
}
