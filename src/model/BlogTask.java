// 
// Decompiled by Procyon v0.6.0
// 

package model;

import java.util.List;

public class BlogTask
{
    private int taskId;
    private int assignmentId;
    private String postTitle;
    private String placeName;
    private String placeUrl;
    private String placeAddress;
    private String placeOriginAddress;
    private String mainKeyword;
    private String bottomTagsString;
    private String forbiddenWordsString;
    private int taskCount;
    private String gptPrompt;
    private NaverAccount naverAccount;
    private String category;
    private String prompt;
    private String resultPostUrl;
    private String imageOption;
    private List<String> imageUrls;
    private String googleDriveUrl;
    private String pixabayKeyword;
    
    public int getTaskId() {
        return this.taskId;
    }
    
    public void setTaskId(final int taskId) {
        this.taskId = taskId;
    }
    
    public int getAssignmentId() {
        return this.assignmentId;
    }
    
    public void setAssignmentId(final int assignmentId) {
        this.assignmentId = assignmentId;
    }
    
    public String getPlaceAddress() {
        return this.placeAddress;
    }
    
    public String getPostTitle() {
        return this.postTitle;
    }
    
    public void setPostTitle(final String postTitle) {
        this.postTitle = postTitle;
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
    
    public String getPlaceUrl() {
        return this.placeUrl;
    }
    
    public void setPlaceUrl(final String placeUrl) {
        this.placeUrl = placeUrl;
    }
    
    public String getMainKeyword() {
        return this.mainKeyword;
    }
    
    public void setMainKeyword(final String mainKeyword) {
        this.mainKeyword = mainKeyword;
    }
    
    public String getBottomTagsString() {
        return this.bottomTagsString;
    }
    
    public void setBottomTagsString(final String bottomTagsString) {
        this.bottomTagsString = bottomTagsString;
    }
    
    public String getForbiddenWordsString() {
        return this.forbiddenWordsString;
    }
    
    public void setForbiddenWordsString(final String forbiddenWordsString) {
        this.forbiddenWordsString = forbiddenWordsString;
    }
    
    public int getTaskCount() {
        return this.taskCount;
    }
    
    public void setTaskCount(final int taskCount) {
        this.taskCount = taskCount;
    }
    
    public String getGptPrompt() {
        return this.gptPrompt;
    }
    
    public void setGptPrompt(final String gptPrompt) {
        this.gptPrompt = gptPrompt;
    }
    
    public NaverAccount getNaverAccount() {
        return this.naverAccount;
    }
    
    public void setNaverAccount(final NaverAccount naverAccount) {
        this.naverAccount = naverAccount;
    }
    
    public String getPrompt() {
        return this.prompt;
    }
    
    public void setPrompt(final String prompt) {
        this.prompt = prompt;
    }
    
    public String getCategory() {
        return this.category;
    }
    
    public void setCategory(final String category) {
        this.category = category;
    }
    
    public String getResultPostUrl() {
        return this.resultPostUrl;
    }
    
    public void setResultPostUrl(final String resultPostUrl) {
        this.resultPostUrl = resultPostUrl;
    }
    
    public String getImageOption() {
        return this.imageOption;
    }
    
    public void setImageOption(final String imageOption) {
        this.imageOption = imageOption;
    }
    
    public List<String> getImageUrls() {
        return this.imageUrls;
    }
    
    public void setImageUrls(final List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
    
    public String getGoogleDriveUrl() {
        return this.googleDriveUrl;
    }
    
    public void setGoogleDriveUrl(final String googleDriveUrl) {
        this.googleDriveUrl = googleDriveUrl;
    }
    
    public String getPixabayKeyword() {
        return this.pixabayKeyword;
    }
    
    public void setPixabayKeyword(final String pixabayKeyword) {
        this.pixabayKeyword = pixabayKeyword;
    }
    
    public String getPlaceOriginAddress() {
        return this.placeOriginAddress;
    }
    
    public void setPlaceOriginAddress(final String placeOriginAddress) {
        this.placeOriginAddress = placeOriginAddress;
    }
}
