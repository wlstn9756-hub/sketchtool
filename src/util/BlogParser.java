// 
// Decompiled by Procyon v0.6.0
// 

package util;

import java.util.Iterator;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Document;
import java.io.IOException;
import org.jsoup.nodes.Element;
import org.jsoup.Jsoup;

public class BlogParser
{
    private final String blogUrl;
    private String parsedContent;
    
    public BlogParser(final String blogUrl) {
        this.blogUrl = blogUrl;
    }
    
    public void parse() {
        try {
            final Document doc = Jsoup.connect(this.blogUrl).get();
            final Element content = doc.selectFirst("div.se-main-container");
            if (content != null) {
                final StringBuilder sb = new StringBuilder();
                final Elements paragraphs = content.select("p.se-text-paragraph");
                for (final Element paragraph : paragraphs) {
                    sb.append(paragraph.text()).append("\n");
                }
                this.parsedContent = sb.toString().trim();
                System.out.println("\ud83d\udcc4 \ube14\ub85c\uadf8 \ud30c\uc2f1 \uacb0\uacfc:\n" + this.parsedContent);
            }
            else {
                System.out.println("\u274c \ubcf8\ubb38\uc744 \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
            }
        }
        catch (final IOException e) {
            throw new RuntimeException("\u274c \ube14\ub85c\uadf8 \ud30c\uc2f1 \uc2e4\ud328: " + e.getMessage(), (Throwable)e);
        }
    }
    
    public String getParsedContent() {
        return this.parsedContent;
    }
}
