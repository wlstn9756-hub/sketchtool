// 
// Decompiled by Procyon v0.6.0
// 

package api;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import javax.imageio.ImageIO;
import util.ImageUtil;
import java.nio.file.Path;
import java.nio.file.Paths;
import config.Config;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.io.File;
import java.util.Iterator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.Response;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import okhttp3.Request;
import okhttp3.OkHttpClient;
import java.util.ArrayList;
import java.util.List;
import com.google.api.services.drive.Drive;

public class PixabayToDriveUploader
{
    private static final String PIXABAY_API_KEY = "51208610-ee587b1ded361587de5026361";
    private static final String PIXABAY_API_URL = "https://pixabay.com/api/";
    private final Drive driveService;
    
    public PixabayToDriveUploader(final Drive driveService) {
        this.driveService = driveService;
    }
    
    public List<String> fetchImageUrls(final String keyword, final int count) throws IOException {
        final List<String> imageUrls = new ArrayList<String>();
        final String url = "https://pixabay.com/api/?key=51208610-ee587b1ded361587de5026361&q=" + keyword + "&image_type=photo&per_page=" + count;
        final OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder().url(url).build();
        try (final Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Pixabay API \uc2e4\ud328");
            }
            final JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            final JsonArray hits = json.getAsJsonArray("hits");
            for (final JsonElement hit : hits) {
                final String imageUrl = hit.getAsJsonObject().get("largeImageURL").getAsString();
                imageUrls.add(imageUrl);
            }
        }
        return imageUrls;
    }
    
    public void uploadImagesToDrive(final List<String> imageUrls, final String folderId) throws IOException {
        final int total = imageUrls.size();
        int success = 0;
        for (int i = 0; i < total; ++i) {
            final String imageUrl = imageUrls.get(i);
            try {
                final File tempFile = downloadImageToTempFile(imageUrl);
                this.uploadToDrive(tempFile, folderId);
                tempFile.delete();
                ++success;
                System.out.printf("\u2705 \uc5c5\ub85c\ub4dc \uc9c4\ud589\ub960: %d/%d (%.0f%% \uc644\ub8cc)\n", success, total, success * 100.0 / total);
            }
            catch (final Exception e) {
                System.out.printf("\u274c [%d/%d] \uc5c5\ub85c\ub4dc \uc2e4\ud328: %s\n", i + 1, total, e.getMessage());
            }
        }
        System.out.printf("\ud83c\udf89 \uc804\uccb4 \uc5c5\ub85c\ub4dc \uc644\ub8cc: %d\uac74 \uc911 %d\uac74 \uc131\uacf5\n", total, success);
    }
    
    public static File downloadImageToTempFile(final String imageUrl) throws IOException {
        final File tempFile = Files.createTempFile("pixabay_", ".jpg", (FileAttribute<?>[])new FileAttribute[0]).toFile();
        final InputStream in = new URL(imageUrl).openStream();
        try {
            final OutputStream out = new FileOutputStream(tempFile);
            try {
                final byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                System.out.println("\u2705 \uc784\uc2dc \uc774\ubbf8\uc9c0 \uc800\uc7a5 \uc644\ub8cc: " + tempFile.getAbsolutePath());
                final File file = tempFile;
                out.close();
                if (in != null) {
                    in.close();
                }
                return file;
            }
            catch (final Throwable t) {
                try {
                    out.close();
                }
                catch (final Throwable exception) {
                    t.addSuppressed(exception);
                }
                throw t;
            }
        }
        catch (final Throwable t2) {
            if (in != null) {
                try {
                    in.close();
                }
                catch (final Throwable exception2) {
                    t2.addSuppressed(exception2);
                }
            }
            throw t2;
        }
    }
    
    private void uploadToDrive(final File localFile, final String folderId) throws IOException {
        final com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName(localFile.getName());
        fileMetadata.setParents(List.of(folderId));
        final FileContent mediaContent = new FileContent("image/jpeg", localFile);
        final com.google.api.services.drive.model.File uploadedFile = this.driveService.files().create(fileMetadata, mediaContent).setFields("id, name").execute();
        System.out.println("\u2705 \uc5c5\ub85c\ub4dc \uc644\ub8cc: " + uploadedFile.getName() + " (ID: " + uploadedFile.getId());
    }
    
    public void downloadImagesFromPixabay(final String keyword, final int count) throws IOException {
        final List<String> imageUrls = this.fetchImageUrls(keyword, count);
        final Path baseDir = Paths.get(System.getProperty("user.home"), "Desktop", Config.FOLDER_1, "blogAutoData", "auto_image", "\ud53d\uc0ac\ubca0\uc774_" + keyword);
        final File targetDir = baseDir.toFile();
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        final int total = imageUrls.size();
        int success = 0;
        for (int i = 0; i < total; ++i) {
            final String imageUrl = imageUrls.get(i);
            try {
                final String fileName = "pixabay_image_" + (i + 1) + ".jpg";
                final File savedFile = downloadImageToFile(imageUrl, targetDir.getAbsolutePath(), fileName);
                ++success;
                System.out.printf("\u2705 [%d/%d] \ub2e4\uc6b4\ub85c\ub4dc \uc131\uacf5: %s\n", success, total, savedFile.getAbsolutePath());
            }
            catch (final Exception e) {
                System.out.printf("\u274c [%d/%d] \ub2e4\uc6b4\ub85c\ub4dc \uc2e4\ud328: %s\n", i + 1, total, e.getMessage());
            }
        }
        System.out.printf("\ud83c\udf89 \uc804\uccb4 \ub2e4\uc6b4\ub85c\ub4dc \uc644\ub8cc: %d\uac74 \uc911 %d\uac74 \uc131\uacf5\n", total, success);
    }
    
    public static File downloadImageToFile(final String imageUrl, final String saveDir, final String fileName) throws IOException {
        final File targetDir = new File(saveDir);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        final File imageFile = new File(targetDir, fileName);
        try (final InputStream in = new URL(imageUrl).openStream()) {
            final byte[] imageBytes = in.readAllBytes();
            final BufferedImage original = ImageUtil.readImageWithOrientation(imageBytes);
            final BufferedImage finalImage = ImageUtil.createCardImageWithFixedCanvas(original);
            ImageIO.write(finalImage, "jpg", imageFile);
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
        return imageFile;
    }
}
