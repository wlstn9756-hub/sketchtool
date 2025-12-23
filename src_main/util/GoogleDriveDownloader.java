// 
// Decompiled by Procyon v0.6.0
// 

package util;

import com.google.api.client.http.HttpResponse;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.io.IOException;
import java.io.FileOutputStream;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.Drive;

public class GoogleDriveDownloader
{
    private final Drive driveService;
    
    public GoogleDriveDownloader(final Drive driveService) {
        this.driveService = driveService;
    }
    
    public void downloadImagesFromFolder(final String folderId, final String localPath) throws IOException {
        final String query = "'" + folderId + "' in parents and mimeType contains 'image/' and trashed = false";
        final FileList result = this.driveService.files().list().setQ(query).setFields("files(id, name)").execute();
        final List<File> files = result.getFiles();
        if (files.isEmpty()) {
            System.out.println("\u274c \ud3f4\ub354\uc5d0 \uc774\ubbf8\uc9c0\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return;
        }
        System.out.printf("\ud83d\udcc1 \ub2e4\uc6b4\ub85c\ub4dc\ud560 \uc774\ubbf8\uc9c0 \uc218: %d\uac1c\n", files.size());
        int successCount = 0;
        int failCount = 0;
        final int total = files.size();
        int index = 1;
        for (File file : files) {
            final String fileId = file.getId();
            final String fileName = file.getName();
            final java.io.File localFile = new java.io.File(localPath + "/" + fileName);
            try (final OutputStream outputStream = new FileOutputStream(localFile)) {
                final HttpResponse response = this.driveService.files().get(fileId).executeMedia();
                response.download(outputStream);
                ++successCount;
                System.out.printf("\u2705 [%d/%d] \ub2e4\uc6b4\ub85c\ub4dc \uc644\ub8cc: %s\n", index, total, fileName);
            }
            catch (final IOException e) {
                ++failCount;
                System.out.printf("\u274c [%d/%d] \ub2e4\uc6b4\ub85c\ub4dc \uc2e4\ud328: %s (%s)\n", index, total, fileName, e.getMessage());
            }
            ++index;
        }
        System.out.println("\n\ud83d\udce6 \ub2e4\uc6b4\ub85c\ub4dc \uc694\uc57d");
        System.out.printf("- \ucd1d \ud30c\uc77c \uc218: %d\n", total);
        System.out.printf("- \uc131\uacf5: %d\uac1c\n", successCount);
        System.out.printf("- \uc2e4\ud328: %d\uac1c\n", failCount);
    }
}
