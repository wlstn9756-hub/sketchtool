package util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

/**
 * 고급 이미지 처리 유틸리티
 * - 프리미엄 카드 이미지 (고급 그림자 + 둥근 모서리)
 * - 2열 그리드 배치
 * - 2x2 그리드 배치
 */
public class ImageUtil {

    // ============================================
    // 고급 이미지 효과 설정값
    // ============================================
    private static final int PREMIUM_CORNER_RADIUS = 40;      // 더 부드러운 모서리
    private static final int PREMIUM_SHADOW_LAYERS = 25;      // 다중 레이어 그림자
    private static final float PREMIUM_SHADOW_OPACITY = 0.12f; // 그림자 투명도
    private static final int BORDER_WIDTH = 1;                 // 테두리 두께
    private static final Color BORDER_COLOR = new Color(230, 230, 230); // 연한 테두리

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

        // 배경 (부드러운 회색)
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
     * 기본 카드 이미지 생성 (기존 방식)
     */
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

        final Image tmp = image.getScaledInstance(resizedW, resizedH, Image.SCALE_SMOOTH);
        final BufferedImage resized = new BufferedImage(resizedW, resizedH, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D gResize = resized.createGraphics();
        gResize.drawImage(tmp, 0, 0, null);
        gResize.dispose();

        final BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, canvasWidth, canvasHeight);

        final int x = (canvasWidth - resizedW) / 2;
        final int y = (canvasHeight - resizedH) / 2;

        // 기본 그림자
        for (int i = shadowSize; i > 0; --i) {
            final float alpha = i / (float)shadowSize * 0.03f;
            g.setColor(new Color(0, 0, 0, (int)(alpha * 255.0f)));
            g.fillRoundRect(x - i, y - i, resizedW + i * 2, resizedH + i * 2, cornerRadius + i, cornerRadius + i);
        }

        final BufferedImage rounded = roundCorners(resized, cornerRadius);
        g.drawImage(rounded, x, y, null);
        g.dispose();
        return canvas;
    }

    /**
     * 프리미엄 다중 레이어 소프트 그림자
     */
    private static void drawPremiumShadow(Graphics2D g, int x, int y, int w, int h, int cornerRadius, int shadowSize) {
        for (int i = PREMIUM_SHADOW_LAYERS; i > 0; i--) {
            float progress = (float) i / PREMIUM_SHADOW_LAYERS;
            float alpha = (float) Math.pow(progress, 2.5) * PREMIUM_SHADOW_OPACITY;
            int offset = (int) (progress * shadowSize);

            g.setColor(new Color(0, 0, 0, (int)(alpha * 255)));
            g.fillRoundRect(
                x - offset + 2,
                y - offset + 4,
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

        RoundRectangle2D roundedRect = new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius);
        g2.setClip(roundedRect);
        g2.drawImage(image, 0, 0, null);
        g2.setClip(null);

        g2.setColor(BORDER_COLOR);
        g2.setStroke(new BasicStroke(BORDER_WIDTH));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, cornerRadius, cornerRadius));

        g2.dispose();
        return output;
    }

    // ============================================
    // 📐 2열 그리드 이미지 배치 (Side by Side)
    // ============================================

    /**
     * 2개의 이미지를 양옆으로 배치 (2열 1행)
     */
    public static BufferedImage createTwoColumnGrid(BufferedImage img1, BufferedImage img2) {
        final int canvasWidth = 960;
        final int canvasHeight = 480;
        final int gap = 20;
        final int padding = 30;
        final int cornerRadius = 30;

        final int imageWidth = (canvasWidth - gap - padding * 2) / 2;
        final int imageHeight = canvasHeight - padding * 2;

        final BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = canvas.createGraphics();
        setHighQualityRenderingHints(g);

        g.setColor(new Color(252, 252, 252));
        g.fillRect(0, 0, canvasWidth, canvasHeight);

        BufferedImage resized1 = resizeAndCropToFit(img1, imageWidth, imageHeight);
        BufferedImage resized2 = resizeAndCropToFit(img2, imageWidth, imageHeight);

        resized1 = roundCorners(resized1, cornerRadius);
        resized2 = roundCorners(resized2, cornerRadius);

        int x1 = padding;
        int y1 = padding;
        drawSmallShadow(g, x1, y1, imageWidth, imageHeight, cornerRadius);
        g.drawImage(resized1, x1, y1, null);

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

        final BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = canvas.createGraphics();
        setHighQualityRenderingHints(g);

        g.setColor(new Color(252, 252, 252));
        g.fillRect(0, 0, canvasWidth, canvasHeight);

        BufferedImage[] images = {img1, img2, img3, img4};
        int[][] positions = {
            {padding, padding},
            {padding + imageWidth + gap, padding},
            {padding, padding + imageHeight + gap},
            {padding + imageWidth + gap, padding + imageHeight + gap}
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
     */
    public static List<BufferedImage> createMultipleGridImages(List<BufferedImage> images) {
        List<BufferedImage> result = new ArrayList<>();

        for (int i = 0; i < images.size(); i += 2) {
            if (i + 1 < images.size()) {
                result.add(createTwoColumnGrid(images.get(i), images.get(i + 1)));
            } else {
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
     * 둥근 모서리 적용
     */
    public static BufferedImage roundCorners(final BufferedImage image, final int cornerRadius) {
        final int w = image.getWidth();
        final int h = image.getHeight();
        final BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = output.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return output;
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
            newHeight = targetHeight;
            newWidth = (int) (targetHeight * imgRatio);
        } else {
            newWidth = targetWidth;
            newHeight = (int) (targetWidth / imgRatio);
        }

        BufferedImage scaled = resizeHighQuality(image, newWidth, newHeight);

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

    /**
     * EXIF 방향 정보를 적용하여 이미지 읽기
     */
    public static BufferedImage readImageWithOrientation(final byte[] imageBytes) throws Exception {
        final Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageBytes));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        final Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

        if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
            final int orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            final AffineTransform transform = new AffineTransform();

            switch (orientation) {
                case 6:
                    transform.translate(image.getHeight(), 0);
                    transform.rotate(Math.toRadians(90));
                    image = transformImage(image, transform, image.getHeight(), image.getWidth());
                    break;
                case 3:
                    transform.translate(image.getWidth(), image.getHeight());
                    transform.rotate(Math.toRadians(180));
                    image = transformImage(image, transform, image.getWidth(), image.getHeight());
                    break;
                case 8:
                    transform.translate(0, image.getWidth());
                    transform.rotate(Math.toRadians(270));
                    image = transformImage(image, transform, image.getHeight(), image.getWidth());
                    break;
            }
        }
        return image;
    }

    private static BufferedImage transformImage(BufferedImage image, AffineTransform transform, int newW, int newH) {
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);
        BufferedImage rotated = new BufferedImage(newW, newH, image.getType());
        op.filter(image, rotated);
        return rotated;
    }

    /**
     * 파일에서 이미지 로드
     */
    public static BufferedImage loadImage(String path) throws IOException {
        return ImageIO.read(new File(path));
    }

    /**
     * 파일에서 이미지 로드 (EXIF 방향 적용)
     */
    public static BufferedImage loadImageWithOrientation(String path) throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of(path));
        return readImageWithOrientation(bytes);
    }

    /**
     * 이미지 저장
     */
    public static void saveImage(BufferedImage image, String path) throws IOException {
        String format = path.toLowerCase().endsWith(".png") ? "png" : "jpg";
        ImageIO.write(image, format, new File(path));
        System.out.println("✅ 이미지 저장 완료: " + path);
    }
}
