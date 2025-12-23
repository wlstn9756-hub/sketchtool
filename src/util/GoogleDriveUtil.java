// 
// Decompiled by Procyon v0.6.0
// 

package util;

import java.security.GeneralSecurityException;
import java.io.IOException;
import java.io.InputStream;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import java.util.Collection;
import java.util.List;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.drive.Drive;

public class GoogleDriveUtil
{
    public static Drive getDriveService() throws IOException, GeneralSecurityException {
        final InputStream in = GoogleDriveUtil.class.getClassLoader().getResourceAsStream("devco-460206-378b3d45776a.json");
        final GoogleCredential credential = GoogleCredential.fromStream(in).createScoped(List.of("https://www.googleapis.com/auth/drive"));
        return new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), credential).setApplicationName("PixabayUploader").build();
    }
}
