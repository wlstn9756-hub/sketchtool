// 
// Decompiled by Procyon v0.6.0
// 

package util;

import java.util.ArrayList;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Color;
import java.awt.image.ImageObserver;
import java.awt.Image;
import java.awt.RenderingHints;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.util.stream.Stream;
import java.awt.image.RenderedImage;
import java.nio.file.attribute.FileAttribute;
import javax.imageio.ImageIO;
import java.nio.file.LinkOption;
import java.util.Random;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.nio.file.Path;
import java.util.List;
import java.nio.file.Files;
import javax.swing.JFileChooser;
import java.nio.file.Paths;
import config.Config;

public class ThumbnailGenerator
{
    public static String makeThumbNailImageFromRandomBackground(final String titleStr) throws IOException {
        Path backgroundFolder = Paths.get(System.getProperty("user.home"), "Desktop", Config.FOLDER_1, "blogAutoData", "auto_image", "background");
        Path thumbnailFolder = Paths.get(System.getProperty("user.home"), "Desktop", Config.FOLDER_1, "blogAutoData", "auto_image", "thumbnail");
        String desktopPath = "";
        if (SysUtil.isWindows()) {
            desktopPath = new JFileChooser().getFileSystemView().getHomeDirectory().getAbsolutePath();
            backgroundFolder = Paths.get(desktopPath, Config.FOLDER_1, Config.FOLDER_2, Config.FOLDER_3, Config.FOLDER_4);
            thumbnailFolder = Paths.get(desktopPath, Config.FOLDER_1, Config.FOLDER_2, Config.FOLDER_3, Config.FOLDER_5);
        }
        final String resultImageName = "result_thumbNail.png";
        final Path resultImagePath = thumbnailFolder.resolve(resultImageName);
        List<Path> imageList;
        try (final Stream<Path> stream = Files.list(backgroundFolder)) {
            imageList = stream.filter(p -> {
                final String name = p.getFileName().toString().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
            }).collect((Collector<? super Path, ?, List<Path>>)Collectors.toList());
        }
        if (imageList.isEmpty()) {
            throw new IllegalStateException("\u274c \ubc30\uacbd \uc774\ubbf8\uc9c0\uac00 \uc874\uc7ac\ud558\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4: " + String.valueOf(backgroundFolder));
        }
        final Path selectedImage = imageList.get(new Random().nextInt(imageList.size()));
        System.out.println("\ud83d\udcf8 \uc120\ud0dd\ub41c \ubc30\uacbd \uc774\ubbf8\uc9c0: " + String.valueOf(selectedImage));
        if (Files.exists(resultImagePath, new LinkOption[0])) {
            Files.delete(resultImagePath);
            System.out.println("\ud83e\uddf9 \uae30\uc874 \uc378\ub124\uc77c \uc0ad\uc81c \uc644\ub8cc: " + String.valueOf(resultImagePath));
        }
        final BufferedImage original = ImageIO.read(selectedImage.toFile());
        final BufferedImage result = createSquareThumbnailWithText(original, titleStr);
        if (!Files.exists(thumbnailFolder, new LinkOption[0])) {
            Files.createDirectories(thumbnailFolder, (FileAttribute<?>[])new FileAttribute[0]);
        }
        ImageIO.write(result, "png", resultImagePath.toFile());
        System.out.println("\u2705 \uc378\ub124\uc77c \uc800\uc7a5 \uc644\ub8cc: " + String.valueOf(resultImagePath));
        return resultImagePath.toString();
    }
    
    private static BufferedImage createSquareThumbnailWithText(final BufferedImage original, final String titleStr) {
        final int targetSize = 800;
        final int originalWidth = original.getWidth();
        final int originalHeight = original.getHeight();
        BufferedImage squareBackground;
        if (originalWidth >= targetSize && originalHeight >= targetSize) {
            final int x = (originalWidth - targetSize) / 2;
            final int y = (originalHeight - targetSize) / 2;
            squareBackground = original.getSubimage(x, y, targetSize, targetSize);
        }
        else {
            final int size = Math.min(originalWidth, originalHeight);
            final BufferedImage cropped = original.getSubimage((originalWidth - size) / 2, (originalHeight - size) / 2, size, size);
            squareBackground = new BufferedImage(targetSize, targetSize, 2);
            final Graphics2D g2 = squareBackground.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(cropped, 0, 0, targetSize, targetSize, null);
            g2.dispose();
        }
        final BufferedImage result = new BufferedImage(targetSize, targetSize, 2);
        final Graphics2D g3 = result.createGraphics();
        g3.drawImage(squareBackground, 0, 0, null);
        final int boxSize = (int)(targetSize * 0.9);
        final int boxX = (targetSize - boxSize) / 2;
        final int boxY = (targetSize - boxSize) / 2;
        g3.setColor(new Color(255, 255, 255, 200));
        g3.fillRoundRect(boxX, boxY, boxSize, boxSize, 20, 20);
        g3.setColor(Color.BLACK);
        final Font font = new Font("Malgun Gothic", 1, 60);
        g3.setFont(font);
        final FontMetrics fm = g3.getFontMetrics();
        final String[] wrappedLines = wrapText(titleStr, fm, boxSize - 40);
        final int lineHeight = fm.getHeight();
        final int lineSpacing = 20;
        int textY = boxY + (boxSize - (lineHeight + lineSpacing) * wrappedLines.length) / 2 + fm.getAscent();
        for (final String line : wrappedLines) {
            final int textWidth = fm.stringWidth(line);
            final int textX = boxX + (boxSize - textWidth) / 2;
            g3.drawString(line, textX, textY);
            textY += lineHeight + lineSpacing;
        }
        g3.dispose();
        return result;
    }
    
    private static String[] wrapText(final String text, final FontMetrics fm, final int maxWidth) {
        final List<String> lines = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        final String[] split = text.split(" ");
        for (int length = split.length, i = 0; i < length; ++i) {
            final String word = split[i];
            final String candidate = String.valueOf(line) + word;
            if (fm.stringWidth(candidate) > maxWidth) {
                lines.add(line.toString().trim());
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        if (!line.isEmpty()) {
            lines.add(line.toString().trim());
        }
        return lines.toArray(new String[0]);
    }
}
