// 
// Decompiled by Procyon v0.6.0
// 

package config;

public class Config
{
    public static final String API_BASE_URL = "http://sketchtool.co.kr/";
    public static final String LOCAL_BASE_URL = "http://127.0.0.1:8000/";
    public static String NAVER_LOGIN_URL;
    public static final String GOOGLE_SETTING_URL = "chrome://settings/content";
    public static String CHAT_GPT_API_KEY;
    public static String PC_HW_CODE;
    public static String FOLDER_1;
    public static String FOLDER_2;
    public static String FOLDER_3;
    public static String FOLDER_4;
    public static String FOLDER_5;
    public static String FOLDER_6;
    
    static {
        Config.NAVER_LOGIN_URL = "https://nid.naver.com/nidlogin.login?mode=form";
        Config.CHAT_GPT_API_KEY = "";
        Config.PC_HW_CODE = "";
        Config.FOLDER_1 = "sketchBlogAuto";
        Config.FOLDER_2 = "blogAutoData";
        Config.FOLDER_3 = "auto_image";
        Config.FOLDER_4 = "background";
        Config.FOLDER_5 = "thumbnail";
        Config.FOLDER_6 = "hospital";
    }
}
