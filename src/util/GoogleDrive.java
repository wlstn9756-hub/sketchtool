// 
// Decompiled by Procyon v0.6.0
// 

package util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.File;
import java.net.URL;
import com.google.api.services.drive.Drive;

public class GoogleDrive
{
    public static void uploadImageToGoogleDrive(final String imageUrl, final String filename, final Drive driveService) throws IOException {
        final InputStream imageStream = new URL(imageUrl).openStream();
        final File fileMetadata = new File();
        fileMetadata.setName(filename);
        fileMetadata.setMimeType("image/jpeg");
        final FileContent mediaContent = new FileContent("image/jpeg", streamToTempFile(imageStream, filename));
        final File uploadedFile = driveService.files().create(fileMetadata, mediaContent).setFields("id, webViewLink").execute();
        System.out.println("\u2705 \uc5c5\ub85c\ub4dc \uc131\uacf5: " + uploadedFile.getWebViewLink());
    }
    
    private static java.io.File streamToTempFile(final InputStream inputStream, final String filename) throws IOException {
        final java.io.File tempFile = java.io.File.createTempFile(filename, ".jpg");
        try (final FileOutputStream out = new FileOutputStream(tempFile)) {
            final byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }
}
