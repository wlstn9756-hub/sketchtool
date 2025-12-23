// 
// Decompiled by Procyon v0.6.0
// 

package util;

import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.model.File;
import java.util.Collections;
import com.google.api.services.drive.model.FileList;
import com.google.api.client.http.HttpRequestInitializer;
import java.util.Collection;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import com.google.api.services.drive.Drive;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.image.AffineTransformOp;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import java.awt.geom.AffineTransform;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.imaging.ImageMetadataReader;
import java.io.ByteArrayInputStream;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.InputStream;
import java.util.Iterator;
import java.nio.file.Path;
import java.io.IOException;
import java.awt.image.RenderedImage;
import javax.imageio.ImageIO;
import java.awt.image.ImageObserver;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import javax.swing.JFileChooser;
import java.nio.file.Paths;
import config.Config;
import java.util.List;

public class ImageUtil
{
    // ============================================
    // 고급 이미지 효과 설정값
    // ============================================
    private static final int PREMIUM_CORNER_RADIUS = 40;      // 더 부드러운 모서리
    private static final int PREMIUM_SHADOW_LAYERS = 25;      // 다중 레이어 그림자
    private static final float PREMIUM_SHADOW_OPACITY = 0.12f; // 그림자 투명도
    private static final int BORDER_WIDTH = 1;                 // 테두리 두께
    private static final Color BORDER_COLOR = new Color(230, 230, 230); // 연한 테두리
    public static void downloadImages(final List<String> imageUrls, final String placeName) {
        try {
            String desktopPath = "";
            Path baseDir = Paths.get(System.getProperty("user.home"), "Desktop", Config.FOLDER_1, "blogAutoData", "auto_image", placeName);
            if (SysUtil.isWindows()) {
                desktopPath = new JFileChooser().getFileSystemView().getHomeDirectory().getAbsolutePath();
                baseDir = Paths.get(desktopPath, Config.FOLDER_1, Config.FOLDER_2, Config.FOLDER_3, placeName);
            }
            if (!Files.exists(baseDir, new LinkOption[0])) {
                Files.createDirectories(baseDir, (FileAttribute<?>[])new FileAttribute[0]);
                System.out.println("\ud83d\udcc1 \ud3f4\ub354 \uc0dd\uc131\ub428: " + baseDir.toString());
            }
            int count = 1;
            for (String originalUrl : imageUrls) {
                try {
                    final int lastSlash = originalUrl.lastIndexOf(47);
                    final String baseUrl = originalUrl.substring(0, lastSlash + 1);
                    final String filename = originalUrl.substring(lastSlash + 1);
                    final String encodedFilename = URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
                    final String encodedUrl = baseUrl + encodedFilename;
                    final URL url = new URL(encodedUrl);
                    final HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    final InputStream in = conn.getInputStream();
                    try {
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        in.transferTo(baos);
                        final byte[] imageBytes = baos.toByteArray();
                        BufferedImage orientedImage = readImageWithOrientation(imageBytes);
                        if (orientedImage == null) {
                            System.err.println("\u274c \uc774\ubbf8\uc9c0 \uc77d\uae30 \uc2e4\ud328: " + originalUrl);
                            if (in == null) {
                                continue;
                            }
                            in.close();
                        }
                        else {
                            final int maxWidth = 960;
                            if (orientedImage.getWidth() > maxWidth) {
                                final int newWidth = maxWidth;
                                final int newHeight = orientedImage.getHeight() * newWidth / orientedImage.getWidth();
                                final Image tmp = orientedImage.getScaledInstance(newWidth, newHeight, 4);
                                final BufferedImage resized = new BufferedImage(newWidth, newHeight, 2);
                                final Graphics2D g2d = resized.createGraphics();
                                g2d.drawImage(tmp, 0, 0, null);
                                g2d.dispose();
                                orientedImage = resized;
                            }
                            final BufferedImage processedImage = createCardImageWithFixedCanvas(orientedImage);
                            final Path imagePath = baseDir.resolve(count + ".png");
                            ImageIO.write(processedImage, "png", imagePath.toFile());
                            System.out.println("\u2705 \uc774\ubbf8\uc9c0 \uc800\uc7a5 \uc644\ub8cc: " + String.valueOf(imagePath));
                            ++count;
                            if (in == null) {
                                continue;
                            }
                            in.close();
                        }
                    }
                    catch (final Throwable t) {
                        if (in != null) {
                            try {
                                in.close();
                            }
                            catch (final Throwable exception) {
                                t.addSuppressed(exception);
                            }
                        }
                        throw t;
                    }
                }
                catch (final Exception e) {
                    System.err.println("\u274c \uc774\ubbf8\uc9c0 \ub2e4\uc6b4\ub85c\ub4dc \uc2e4\ud328 (" + originalUrl + "): " + e.getMessage());
                }
            }
        }
        catch (final IOException e2) {
            System.err.println("\u274c \ub514\ub809\ud1a0\ub9ac \uc0dd\uc131 \ub610\ub294 \uc800\uc7a5 \uc911 \uc624\ub958: " + e2.getMessage());
        }
    }
    
    public static BufferedImage createCardImageWithFixedCanvas(final BufferedImage image) {
        final int canvasWidth = 960;
        final int canvasHeight = 720;
        final int shadowSize = 20;
        final int cornerRadius = 30;
        final int maxImageWidth = canvasWidth - shadowSize * 4;
        final int maxImageHeight = canvasHeight - shadowSize * 4;
        final float ratio = Math.min(maxImageWidth / (float)image.getWidth(), maxImageHeight / (float)image.getHeight());
        final int resizedW = Math.round(image.getWidth() * ratio);
        final int resizedH = Math.round(image.getHeight() * ratio);
        final Image tmp = image.getScaledInstance(resizedW, resizedH, 4);
        final BufferedImage resized = new BufferedImage(resizedW, resizedH, 2);
        final Graphics2D gResize = resized.createGraphics();
        gResize.drawImage(tmp, 0, 0, null);
        gResize.dispose();
        final BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, 1);
        final Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, canvasWidth, canvasHeight);
        final int x = (canvasWidth - resizedW) / 2;
        final int y = (canvasHeight - resizedH) / 2;
        for (int i = shadowSize; i > 0; --i) {
            final float alpha = i / (float)shadowSize * 0.03f;
            g.setColor(new Color(0, 0, 0, (int)(alpha * 255.0f)));
            g.fillRoundRect(x - i, y - i, resizedW + i * 2, resizedH + i * 2, cornerRadius + i, cornerRadius + i);
        }
        final BufferedImage rounded = new BufferedImage(resizedW, resizedH, 2);
        final Graphics2D g2 = rounded.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, resizedW, resizedH);
        g2.setComposite(AlphaComposite.Src);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0.0f, 0.0f, (float)resizedW, (float)resizedH, (float)cornerRadius, (float)cornerRadius));
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(resized, 0, 0, null);
        g2.dispose();
        g.drawImage(rounded, x, y, null);
        g.dispose();
        return canvas;
    }
    
    public static BufferedImage readImageWithOrientation(final byte[] imageBytes) throws Exception {
        final Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageBytes));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        final Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (directory != null && directory.containsTag(274)) {
            final int orientation = directory.getInt(274);
            final AffineTransform transform = new AffineTransform();
            switch (orientation) {
                case 6: {
                    transform.translate(image.getHeight(), 0.0);
                    transform.rotate(Math.toRadians(90.0));
                    image = transformImage(image, transform, image.getHeight(), image.getWidth());
                    break;
                }
                case 3: {
                    transform.translate(image.getWidth(), image.getHeight());
                    transform.rotate(Math.toRadians(180.0));
                    image = transformImage(image, transform, image.getWidth(), image.getHeight());
                    break;
                }
                case 8: {
                    transform.translate(0.0, image.getWidth());
                    transform.rotate(Math.toRadians(270.0));
                    image = transformImage(image, transform, image.getHeight(), image.getWidth());
                    break;
                }
            }
        }
        return image;
    }
    
    private static BufferedImage transformImage(final BufferedImage image, final AffineTransform transform, final int newW, final int newH) {
        final AffineTransformOp op = new AffineTransformOp(transform, 3);
        final BufferedImage rotated = new BufferedImage(newW, newH, image.getType());
        op.filter(image, rotated);
        return rotated;
    }
    
    public static BufferedImage roundCornersWithShadowAndBackground(final BufferedImage image) {
        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        final int baseShortSide = Math.min(imageWidth, imageHeight);
        final int padding = baseShortSide / 8;
        final int shadowSize = baseShortSide / 10;
        final int cornerRadius = baseShortSide / 10;
        final int canvasWidth = imageWidth + padding * 2 + shadowSize * 2;
        final int canvasHeight = imageHeight + padding * 2 + shadowSize * 2;
        final BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, 1);
        final Graphics2D g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, canvasWidth, canvasHeight);
        final BufferedImage shadow = new BufferedImage(canvasWidth, canvasHeight, 2);
        final Graphics2D sg = shadow.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final int x = padding + shadowSize;
        final int y = padding + shadowSize;
        for (int i = shadowSize; i >= 1; --i) {
            final float alpha = i / (float)shadowSize * 0.07f;
            sg.setColor(new Color(0, 0, 0, (int)(alpha * 255.0f)));
            sg.fillRoundRect(x - i, y - i, imageWidth + i * 2, imageHeight + i * 2, cornerRadius + i, cornerRadius + i);
        }
        sg.dispose();
        g2.drawImage(shadow, 0, 0, null);
        final BufferedImage rounded = new BufferedImage(imageWidth, imageHeight, 2);
        final Graphics2D gr = rounded.createGraphics();
        gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gr.setComposite(AlphaComposite.Clear);
        gr.fillRect(0, 0, imageWidth, imageHeight);
        gr.setComposite(AlphaComposite.Src);
        gr.setColor(Color.WHITE);
        gr.fill(new RoundRectangle2D.Float(0.0f, 0.0f, (float)imageWidth, (float)imageHeight, (float)cornerRadius, (float)cornerRadius));
        gr.setComposite(AlphaComposite.SrcAtop);
        gr.drawImage(image, 0, 0, null);
        gr.dispose();
        g2.drawImage(rounded, x, y, null);
        g2.dispose();
        return canvas;
    }
    
    public static void downloadImagesFromDriveFolder(final String driveUrl, final String placeName) {
        try {
            final String folderId = extractFolderId(driveUrl);
            if (folderId == null) {
                System.err.println("\u274c \ud3f4\ub354 ID\ub97c \ucd94\ucd9c\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return;
            }
            final String viewUrl = "https://drive.google.com/embeddedfolderview?id=" + folderId + "#grid";
            final String html = new String(new URL(viewUrl).openStream().readAllBytes());
            final List<String> imageUrls = extractImageUrlsFromHtml(html);
            if (imageUrls.isEmpty()) {
                System.err.println("\u274c \uc774\ubbf8\uc9c0 URL\uc744 \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return;
            }
            final String folderName = placeName + "_\uad6c\uae00\ub4dc\ub77c\uc774\ube0c";
            downloadImages(imageUrls, folderName);
        }
        catch (final Exception e) {
            System.err.println("\u274c \uad6c\uae00 \ub4dc\ub77c\uc774\ube0c \uc774\ubbf8\uc9c0 \ub2e4\uc6b4\ub85c\ub4dc \uc2e4\ud328: " + e.getMessage());
        }
    }
    
    private static String extractFolderId(final String url) {
        try {
            final Pattern pattern = Pattern.compile("/folders/([a-zA-Z0-9_-]+)");
            final Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        catch (final Exception ex) {}
        return null;
    }
    
    private static List<String> extractImageUrlsFromHtml(final String html) {
        final List<String> urls = new ArrayList<String>();
        final Pattern pattern = Pattern.compile("https://lh3.googleusercontent.com/[^\"'>]+");
        final Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            final String url = matcher.group();
            if (!urls.contains(url)) {
                urls.add(url);
            }
        }
        return urls;
    }
    
    public static BufferedImage roundCorners(final BufferedImage image, final int cornerRadius) {
        final int w = image.getWidth();
        final int h = image.getHeight();
        final BufferedImage output = new BufferedImage(w, h, 2);
        final Graphics2D g2 = output.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new RoundRectangle2D.Float(0.0f, 0.0f, (float)w, (float)h, (float)cornerRadius, (float)cornerRadius));
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return output;
    }

    // ============================================
    // 🎨 고급 이미지 효과 (Premium Effects)
    // ============================================

    /**
     * 프리미엄 카드 이미지 생성 - 더 고급스러운 그림자와 테두리
     */
    public static BufferedImage createPremiumCardImage(final BufferedImage image) {
        final int canvasWidth = 960;
        final int canvasHeight = 720;
        final int shadowSize = 30;
        final int cornerRadius = PREMIUM_CORNER_RADIUS;

        final int maxImageWidth = canvasWidth - shadowSize * 4;
        final int maxImageHeight = canvasHeight - shadowSize * 4;
        final float ratio = Math.min(maxImageWidth / (float)image.getWidth(), maxImageHeight / (float)image.getHeight());
        final int resizedW = Math.round(image.getWidth() * ratio);
        final int resizedH = Math.round(image.getHeight() * ratio);

        // 고품질 리사이즈
        final BufferedImage resized = resizeHighQuality(image, resizedW, resizedH);

        // 캔버스 생성
        final BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = canvas.createGraphics();
        setHighQualityRenderingHints(g);

        // 배경 (부드러운 그라데이션)
        g.setColor(new Color(252, 252, 252));
        g.fillRect(0, 0, canvasWidth, canvasHeight);

        final int x = (canvasWidth - resizedW) / 2;
        final int y = (canvasHeight - resizedH) / 2;

        // 프리미엄 다중 레이어 그림자
        drawPremiumShadow(g, x, y, resizedW, resizedH, cornerRadius, shadowSize);

        // 둥근 모서리 이미지 + 테두리
        final BufferedImage rounded = createRoundedImageWithBorder(resized, cornerRadius);
        g.drawImage(rounded, x, y, null);

        g.dispose();
        return canvas;
    }

    /**
     * 프리미엄 다중 레이어 소프트 그림자
     */
    private static void drawPremiumShadow(Graphics2D g, int x, int y, int w, int h, int cornerRadius, int shadowSize) {
        // 바깥쪽부터 안쪽으로 그림자 레이어를 그림 (더 자연스러운 효과)
        for (int i = PREMIUM_SHADOW_LAYERS; i > 0; i--) {
            float progress = (float) i / PREMIUM_SHADOW_LAYERS;
            // 비선형 알파값으로 더 부드러운 그림자
            float alpha = (float) Math.pow(progress, 2.5) * PREMIUM_SHADOW_OPACITY;
            int offset = (int) (progress * shadowSize);

            g.setColor(new Color(0, 0, 0, (int)(alpha * 255)));
            g.fillRoundRect(
                x - offset + 2,
                y - offset + 4,  // 아래쪽으로 약간 치우침 (자연광 효과)
                w + offset * 2 - 4,
                h + offset * 2 - 4,
                cornerRadius + offset,
                cornerRadius + offset
            );
        }
    }

    /**
     * 둥근 모서리 이미지 + 얇은 테두리 생성
     */
    private static BufferedImage createRoundedImageWithBorder(BufferedImage image, int cornerRadius) {
        final int w = image.getWidth();
        final int h = image.getHeight();

        final BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = output.createGraphics();
        setHighQualityRenderingHints(g2);

        // 둥근 모서리 클리핑
        RoundRectangle2D roundedRect = new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius);
        g2.setClip(roundedRect);
        g2.drawImage(image, 0, 0, null);
        g2.setClip(null);

        // 얇은 테두리 (고급스러운 느낌)
        g2.setColor(BORDER_COLOR);
        g2.setStroke(new java.awt.BasicStroke(BORDER_WIDTH));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, cornerRadius, cornerRadius));

        g2.dispose();
        return output;
    }

    // ============================================
    // 📐 2열 그리드 이미지 배치 (Side by Side)
    // ============================================

    /**
     * 2개의 이미지를 양옆으로 배치 (2열 1행)
     * 스크린샷에서 보여준 것처럼 양옆 배치
     */
    public static BufferedImage createTwoColumnGrid(BufferedImage img1, BufferedImage img2) {
        final int canvasWidth = 960;
        final int canvasHeight = 480;
        final int gap = 20;  // 이미지 간 간격
        final int padding = 30;
        final int cornerRadius = 30;

        final int imageWidth = (canvasWidth - gap - padding * 2) / 2;
        final int imageHeight = canvasHeight - padding * 2;

        // 캔버스 생성
        final BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = canvas.createGraphics();
        setHighQualityRenderingHints(g);

        // 배경
        g.setColor(new Color(252, 252, 252));
        g.fillRect(0, 0, canvasWidth, canvasHeight);

        // 이미지 리사이즈 및 배치
        BufferedImage resized1 = resizeAndCropToFit(img1, imageWidth, imageHeight);
        BufferedImage resized2 = resizeAndCropToFit(img2, imageWidth, imageHeight);

        // 둥근 모서리 적용
        resized1 = roundCorners(resized1, cornerRadius);
        resized2 = roundCorners(resized2, cornerRadius);

        // 그림자 및 이미지 배치 - 왼쪽
        int x1 = padding;
        int y1 = padding;
        drawSmallShadow(g, x1, y1, imageWidth, imageHeight, cornerRadius);
        g.drawImage(resized1, x1, y1, null);

        // 그림자 및 이미지 배치 - 오른쪽
        int x2 = padding + imageWidth + gap;
        int y2 = padding;
        drawSmallShadow(g, x2, y2, imageWidth, imageHeight, cornerRadius);
        g.drawImage(resized2, x2, y2, null);

        g.dispose();
        return canvas;
    }

    /**
     * 4개의 이미지를 2x2 그리드로 배치
     */
    public static BufferedImage createTwoByTwoGrid(BufferedImage img1, BufferedImage img2,
                                                    BufferedImage img3, BufferedImage img4) {
        final int canvasWidth = 960;
        final int canvasHeight = 720;
        final int gap = 15;
        final int padding = 25;
        final int cornerRadius = 25;

        final int imageWidth = (canvasWidth - gap - padding * 2) / 2;
        final int imageHeight = (canvasHeight - gap - padding * 2) / 2;

        // 캔버스 생성
        final BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = canvas.createGraphics();
        setHighQualityRenderingHints(g);

        // 배경
        g.setColor(new Color(252, 252, 252));
        g.fillRect(0, 0, canvasWidth, canvasHeight);

        // 이미지 배열
        BufferedImage[] images = {img1, img2, img3, img4};
        int[][] positions = {
            {padding, padding},                           // 좌상
            {padding + imageWidth + gap, padding},        // 우상
            {padding, padding + imageHeight + gap},       // 좌하
            {padding + imageWidth + gap, padding + imageHeight + gap}  // 우하
        };

        for (int i = 0; i < 4; i++) {
            if (images[i] != null) {
                BufferedImage resized = resizeAndCropToFit(images[i], imageWidth, imageHeight);
                resized = roundCorners(resized, cornerRadius);

                int x = positions[i][0];
                int y = positions[i][1];

                drawSmallShadow(g, x, y, imageWidth, imageHeight, cornerRadius);
                g.drawImage(resized, x, y, null);
            }
        }

        g.dispose();
        return canvas;
    }

    /**
     * 여러 이미지를 2열 그리드로 배치 (동적)
     * @param images 이미지 리스트
     * @return 2열로 배치된 이미지들의 리스트
     */
    public static List<BufferedImage> createMultipleGridImages(List<BufferedImage> images) {
        List<BufferedImage> result = new ArrayList<>();

        for (int i = 0; i < images.size(); i += 2) {
            if (i + 1 < images.size()) {
                // 2개씩 묶어서 양옆 배치
                result.add(createTwoColumnGrid(images.get(i), images.get(i + 1)));
            } else {
                // 마지막 1개는 단독 카드로
                result.add(createPremiumCardImage(images.get(i)));
            }
        }

        return result;
    }

    // ============================================
    // 🔧 유틸리티 메서드
    // ============================================

    /**
     * 작은 그림자 (그리드 이미지용)
     */
    private static void drawSmallShadow(Graphics2D g, int x, int y, int w, int h, int cornerRadius) {
        int shadowSize = 12;
        for (int i = shadowSize; i > 0; i--) {
            float alpha = (float) i / shadowSize * 0.06f;
            g.setColor(new Color(0, 0, 0, (int)(alpha * 255)));
            g.fillRoundRect(x - i + 2, y - i + 3, w + i * 2 - 4, h + i * 2 - 4,
                           cornerRadius + i, cornerRadius + i);
        }
    }

    /**
     * 고품질 리사이즈
     */
    private static BufferedImage resizeHighQuality(BufferedImage image, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        setHighQualityRenderingHints(g);
        g.drawImage(image.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        return resized;
    }

    /**
     * 이미지를 지정 크기에 맞게 리사이즈 및 크롭
     */
    private static BufferedImage resizeAndCropToFit(BufferedImage image, int targetWidth, int targetHeight) {
        float imgRatio = (float) image.getWidth() / image.getHeight();
        float targetRatio = (float) targetWidth / targetHeight;

        int newWidth, newHeight;
        if (imgRatio > targetRatio) {
            // 이미지가 더 넓음 - 높이 맞추고 좌우 크롭
            newHeight = targetHeight;
            newWidth = (int) (targetHeight * imgRatio);
        } else {
            // 이미지가 더 높음 - 너비 맞추고 상하 크롭
            newWidth = targetWidth;
            newHeight = (int) (targetWidth / imgRatio);
        }

        BufferedImage scaled = resizeHighQuality(image, newWidth, newHeight);

        // 중앙 크롭
        int x = (newWidth - targetWidth) / 2;
        int y = (newHeight - targetHeight) / 2;

        return scaled.getSubimage(x, y, targetWidth, targetHeight);
    }

    /**
     * 고품질 렌더링 힌트 설정
     */
    private static void setHighQualityRenderingHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }
    
    private static Drive getDriveService() throws IOException {
        InputStream credentialStream = null;
        credentialStream = ImageUtil.class.getClassLoader().getResourceAsStream("adsketchlab-8fbd8687cedf.json");
        if (credentialStream != null) {
            System.out.println("\u2705 classpath\uc5d0\uc11c JSON \ub85c\ub4dc\ub428");
        }
        else {
            final Path jarDir = Paths.get(System.getProperty("user.dir"), new String[0]);
            final Path jsonPath = jarDir.resolve("adsketchlab-8fbd8687cedf.json");
            if (Files.exists(jsonPath, new LinkOption[0])) {
                credentialStream = new FileInputStream(jsonPath.toFile());
                System.out.println("\u2705 \uc2e4\ud589 \ud3f4\ub354\uc5d0\uc11c JSON \ub85c\ub4dc\ub428");
            }
        }
        if (credentialStream == null) {
            throw new FileNotFoundException("\u274c \uad6c\uae00 \uc778\uc99d JSON \ud30c\uc77c\uc744 \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
        }
        final GoogleCredential credential = GoogleCredential.fromStream(credentialStream).createScoped(List.of("https://www.googleapis.com/auth/drive.readonly"));
        return new Drive.Builder(credential.getTransport(), credential.getJsonFactory(), credential).setApplicationName("AdSketchImageDownloader").build();
    }
    
    public static void downloadOriginalImagesFromDriveAPI(final String driveUrl, final String placeName) {
        try {
            final String folderId = extractFolderId(driveUrl);
            if (folderId == null) {
                System.err.println("\u274c \ud3f4\ub354 ID\ub97c \ucd94\ucd9c\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return;
            }
            final Drive driveService = getDriveService();
            final String query = "'" + folderId + "' in parents and mimeType contains 'image/' and trashed = false";
            final FileList result = driveService.files().list().setQ(query).setFields("files(id, name)").execute();
            List<File> files = result.getFiles();
            if (files == null || files.isEmpty()) {
                System.out.println("\u274c \ud3f4\ub354\uc5d0 \uc774\ubbf8\uc9c0\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return;
            }
            Collections.shuffle(files);
            final int maxDownloadCount = 15;
            if (files.size() > maxDownloadCount) {
                files = files.subList(0, maxDownloadCount);
            }
            final Path baseDir = Paths.get(System.getProperty("user.home"), "Desktop", Config.FOLDER_1, "blogAutoData", "auto_image", placeName + "_\uad6c\uae00\ub4dc\ub77c\uc774\ube0c\uc6d0\ubcf8");
            if (!Files.exists(baseDir, new LinkOption[0])) {
                Files.createDirectories(baseDir, (FileAttribute<?>[])new FileAttribute[0]);
                System.out.println("\ud83d\udcc1 \ud3f4\ub354 \uc0dd\uc131\ub428: " + String.valueOf(baseDir));
            }
            int index = 1;
            int success = 0;
            int fail = 0;
            for (File file : files) {
                final String fileId = file.getId();
                final String fileName = file.getName();
                final Path outputPath = baseDir.resolve(index + ".png");
                try {
                    final HttpResponse response = driveService.files().get(fileId).executeMedia();
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    response.getContent().transferTo(baos);
                    final byte[] imageBytes = baos.toByteArray();
                    final BufferedImage orientedImage = readImageWithOrientation(imageBytes);
                    final BufferedImage processedImage = createCardImageWithFixedCanvas(orientedImage);
                    ImageIO.write(processedImage, "png", outputPath.toFile());
                    System.out.printf("\u2705 [%d/%d] \uc800\uc7a5 \uc644\ub8cc: %s\n", index, files.size(), outputPath.getFileName());
                    ++success;
                }
                catch (final Exception e) {
                    System.err.printf("\u274c [%d/%d] \ub2e4\uc6b4\ub85c\ub4dc \uc2e4\ud328: %s (%s)\n", index, files.size(), fileName, e.getMessage());
                    ++fail;
                }
                ++index;
            }
            System.out.println("\n\ud83d\udce6 \ub2e4\uc6b4\ub85c\ub4dc \uc694\uc57d");
            System.out.printf("- \uc120\ud0dd\ub41c \ud30c\uc77c \uc218: %d\n", files.size());
            System.out.printf("- \uc131\uacf5: %d\uac1c\n", success);
            System.out.printf("- \uc2e4\ud328: %d\uac1c\n", fail);
        }
        catch (final Exception e2) {
            System.err.println("\u274c \uc804\uccb4 \uc2e4\ud328: " + e2.getMessage());
        }
    }
}
