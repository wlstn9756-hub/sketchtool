// 
// Decompiled by Procyon v0.6.0
// 

package model;

import java.nio.file.Path;

public class BlogPost
{
    private String title;
    private String body;
    private Path postFolderPath;
    private String placeName;
    private String placeAddress;
    
    public BlogPost(final String title, final String body, final Path postFolderPath, final String placeName, final String placeAddress) {
        this.title = title;
        this.body = body;
        this.postFolderPath = postFolderPath;
        this.placeName = placeName;
        this.placeAddress = placeAddress;
    }
    
    public String getTitle() {
        return this.title;
    }
    
    public String getBody() {
        return this.body;
    }
    
    public void setBody(final String body) {
        this.body = body;
    }
    
    public Path getPostFolderPath() {
        return this.postFolderPath;
    }
    
    public String getPlaceName() {
        return this.placeName;
    }
    
    public void setPlaceName(final String placeName) {
        this.placeName = placeName;
    }
    
    public String getPlaceAddress() {
        return this.placeAddress;
    }
    
    public void setPlaceAddress(final String placeAddress) {
        this.placeAddress = placeAddress;
    }
}
