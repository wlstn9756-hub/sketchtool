// 
// Decompiled by Procyon v0.6.0
// 

package util;

import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import java.io.InputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.stream.Stream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.nio.file.LinkOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import model.BlogPost;

public class BlogContentLoader
{
    public static BlogPost getBlogPostByAi(final String resultByAi) {
        BlogPost blogPost = null;
        final String[] lines = resultByAi.split("\\r?\\n");
        String title = "";
        String body = "";
        boolean bodyStarted = false;
        final StringBuilder bodyBuilder = new StringBuilder();
        String placeName = "";
        String placeAddress = "";
        for (final String line : lines) {
            if (line.startsWith("\uc81c\ubaa9")) {
                title = line.replaceFirst("\uc81c\ubaa9\\s*[:\uff1a]\\s*", "").trim();
            }
            else if (line.startsWith("\ubcf8\ubb38")) {
                bodyStarted = true;
                bodyBuilder.append(line.replaceFirst("\ubcf8\ubb38\\s*[:\uff1a]\\s*", "").trim()).append("\n");
            }
            else if (line.startsWith("PlaceName")) {
                placeName = line.replaceFirst("PlaceName\\s*[:\uff1a]\\s*", "").trim();
            }
            else if (line.startsWith("PlaceAddress")) {
                placeAddress = line.replaceFirst("PlaceAddress\\s*[:\uff1a]\\s*", "").trim();
            }
            else if (bodyStarted) {
                bodyBuilder.append(line).append("\n");
            }
        }
        body = bodyBuilder.toString().trim();
        if (!title.isEmpty() && !body.isEmpty()) {
            if (placeName.isEmpty()) {
                placeName = null;
            }
            if (placeAddress.isEmpty()) {
                placeAddress = null;
            }
            blogPost = new BlogPost(title, body, null, placeName, placeAddress);
            System.out.println("title : " + title);
            System.out.println("body : " + body);
            System.out.println("PlaceName : " + placeName);
            System.out.println("PlaceAddress : " + placeAddress);
        }
        else {
            System.out.println("\u26a0\ufe0f - \uc81c\ubaa9 \ub610\ub294 \ubcf8\ubb38\uc774 \ube44\uc5b4 \uc788\uc2b5\ub2c8\ub2e4.");
        }
        return blogPost;
    }
    
    public static List<BlogPost> loadTodayPosts() {
        final List<BlogPost> blogPosts = new ArrayList<BlogPost>();
        final String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        final Path basePath = Paths.get(System.getProperty("user.home"), "Desktop", "devco", "blogAutoData", "rumy_30", today);
        try {
            final Stream<Path> paths = Files.list(basePath);
            try {
                final List<Path> postFolders = paths.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).filter(path -> path.getFileName().toString().startsWith("post_")).sorted((p1, p2) -> {
                    final String name1 = p1.getFileName().toString().replace("post_", "");
                    final String name2 = p2.getFileName().toString().replace("post_", "");
                    return Integer.compare(Integer.parseInt(name1), Integer.parseInt(name2));
                }).collect((Collector<? super Path, ?, List<Path>>)Collectors.toList());
                for (Path postFolder : postFolders) {
                    final Path txtPath = postFolder.resolve("post.txt");
                    final Path docxPath = postFolder.resolve("post.docx");
                    String rawContent = null;
                    if (Files.exists(txtPath, new LinkOption[0])) {
                        rawContent = Files.readString(txtPath, StandardCharsets.UTF_8);
                    }
                    else {
                        if (!Files.exists(docxPath, new LinkOption[0])) {
                            System.out.println("\u274c " + String.valueOf(postFolder.getFileName()) + " \uc5d0\uc11c post.txt \ub610\ub294 post.docx\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                            continue;
                        }
                        rawContent = readDocx(docxPath);
                    }
                    final String[] lines = rawContent.split("\\r?\\n");
                    String title = "";
                    String body = "";
                    boolean bodyStarted = false;
                    final StringBuilder bodyBuilder = new StringBuilder();
                    String placeName = "";
                    String placeAddress = "";
                    for (final String line : lines) {
                        if (line.startsWith("\uc81c\ubaa9")) {
                            title = line.replaceFirst("\uc81c\ubaa9\\s*[:\uff1a]\\s*", "").trim();
                        }
                        else if (line.startsWith("\ubcf8\ubb38")) {
                            bodyStarted = true;
                            bodyBuilder.append(line.replaceFirst("\ubcf8\ubb38\\s*[:\uff1a]\\s*", "").trim()).append("\n");
                        }
                        else if (line.startsWith("PlaceName")) {
                            placeName = line.replaceFirst("PlaceName\\s*[:\uff1a]\\s*", "").trim();
                        }
                        else if (line.startsWith("PlaceAddress")) {
                            placeAddress = line.replaceFirst("PlaceAddress\\s*[:\uff1a]\\s*", "").trim();
                        }
                        else if (bodyStarted) {
                            bodyBuilder.append(line).append("\n");
                        }
                    }
                    body = bodyBuilder.toString().trim();
                    if (!title.isEmpty() && !body.isEmpty()) {
                        if (placeName.isEmpty()) {
                            placeName = null;
                        }
                        if (placeAddress.isEmpty()) {
                            placeAddress = null;
                        }
                        blogPosts.add(new BlogPost(title, body, postFolder, placeName, placeAddress));
                        System.out.println("title : " + title);
                        System.out.println("body : " + body);
                        System.out.println("PlaceName : " + placeName);
                        System.out.println("PlaceAddress : " + placeAddress);
                    }
                    else {
                        System.out.println("\u26a0\ufe0f " + String.valueOf(postFolder.getFileName()) + " - \uc81c\ubaa9 \ub610\ub294 \ubcf8\ubb38\uc774 \ube44\uc5b4 \uc788\uc2b5\ub2c8\ub2e4.");
                    }
                }
                if (paths != null) {
                    paths.close();
                }
            }
            catch (final Throwable t) {
                if (paths != null) {
                    try {
                        paths.close();
                    }
                    catch (final Throwable exception) {
                        t.addSuppressed(exception);
                    }
                }
                throw t;
            }
        }
        catch (final IOException e) {
            System.err.println("\ud83d\udcc2 \ud3f4\ub354 \ub85c\ub529 \uc2e4\ud328: " + e.getMessage());
        }
        return blogPosts;
    }
    
    private static String readDocx(final Path docxPath) {
        try (final FileInputStream fis = new FileInputStream(docxPath.toFile());
             final XWPFDocument document = new XWPFDocument(fis)) {
            final StringBuilder sb = new StringBuilder();
            for (final XWPFParagraph para : document.getParagraphs()) {
                sb.append(para.getText()).append("\n");
            }
            return sb.toString();
        }
        catch (final IOException e) {
            System.err.println("\u274c DOCX \uc77d\uae30 \uc2e4\ud328: " + e.getMessage());
            return "";
        }
    }
}
