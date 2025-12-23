// 
// Decompiled by Procyon v0.6.0
// 

package model;

public class NaverAccount
{
    private String id;
    private String pw;
    private String blogUrl;
    private String proxyIp;
    private Integer taskId;
    private String placeName;
    
    public NaverAccount(final String id, final String pw, final String proxyIp) {
        this.id = id;
        this.pw = pw;
        this.proxyIp = proxyIp;
    }
    
    public String getId() {
        return this.id;
    }
    
    public void setId(final String id) {
        this.id = id;
    }
    
    public String getPw() {
        return this.pw;
    }
    
    public void setPw(final String pw) {
        this.pw = pw;
    }
    
    public String getBlogUrl() {
        return this.blogUrl;
    }
    
    public void setBlogUrl(final String blogUrl) {
        this.blogUrl = blogUrl;
    }
    
    public String getProxyIp() {
        return this.proxyIp;
    }
    
    public void setProxyIp(final String proxyIp) {
        this.proxyIp = proxyIp;
    }
    
    public Integer getTaskId() {
        return this.taskId;
    }
    
    public void setTaskId(final Integer taskId) {
        this.taskId = taskId;
    }
    
    public String getPlaceName() {
        return this.placeName;
    }
    
    public void setPlaceName(final String placeName) {
        this.placeName = placeName;
    }
    
    @Override
    public String toString() {
        return "NaverAccount{id='" + this.id + "', pw='" + this.pw + "'}";
    }
}
