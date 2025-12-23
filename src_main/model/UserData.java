// 
// Decompiled by Procyon v0.6.0
// 

package model;

public class UserData
{
    private String placeName;
    private String placeAddress;
    private String blogContents;
    
    public String getBlogContents() {
        return this.blogContents;
    }
    
    public void setBlogContents(final String blogContents) {
        this.blogContents = blogContents;
    }
    
    public String getPlaceAddress() {
        return this.placeAddress;
    }
    
    public void setPlaceAddress(final String placeAddress) {
        this.placeAddress = placeAddress;
    }
    
    public String getPlaceName() {
        return this.placeName;
    }
    
    public void setPlaceName(final String placeName) {
        this.placeName = placeName;
    }
}
