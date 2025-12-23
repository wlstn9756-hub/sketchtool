// 
// Decompiled by Procyon v0.6.0
// 

package com.devco;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.Map;
import java.time.Duration;
import chromeModule.ChromeDriverBuilder;
import java.io.File;
import java.util.HashMap;
import java.util.Random;
import java.util.Arrays;
import model.BlogPost;
import java.nio.file.Path;
import java.net.URISyntaxException;
import util.BlogContentLoader;
import util.PromptLoader;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import util.SysUtil;
import model.NaverAccount;
import java.util.ArrayList;
import java.io.IOException;
import util.ChromeTempCleaner;
import model.BlogTask;
import util.Util;
import config.Config;
import api.Api;
import api.ChatGPTClient;
import java.util.Scanner;
import org.openqa.selenium.JavascriptExecutor;
import chromeModule.DriverModule;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeDriver;

public class SketchNBlogAuto
{
    private static SketchNBlogAuto sketchNBlogAuto;
    private final String driverPath;
    private ChromeDriver chromeDriver;
    private Proxy seleniumProxy;
    private ChromeOptions options;
    private static final DriverModule driverModule;
    private JavascriptExecutor jsExecutor;
    private NBlogModuleBySample nBlogModuleBySample;
    private NBlogModuleByAI nBlogModuleByAI;
    public static boolean isHeadlessMode;
    public static boolean isExistChromeData;
    public static boolean isFirstTime;
    
    public SketchNBlogAuto(final String driverPath) {
        this.driverPath = driverPath;
    }
    
    public static void main(final String[] args) throws IOException, InterruptedException {
        setupGlobalExceptionLogger();
        final Scanner scanner = new Scanner(System.in);
        final ChatGPTClient chatGPTClient = new ChatGPTClient();
        final Api api = new Api();
        Config.CHAT_GPT_API_KEY = chatGPTClient.getApiKey();
        Config.PC_HW_CODE = Util.getPersistentHwCode();
    Label_1020:
        while (true) {
            System.out.println("\n===== \uba54\ub274\ub97c \uc120\ud0dd\ud558\uc138\uc694 =====");
            System.out.println("1. PC \uace0\uc720\ubc88\ud638 \ud655\uc778");
            System.out.println("2. \ube14\ub85c\uadf8 \ubc30\ud3ec \uc790\ub3d9\ud654 \uc2dc\uc791");
            System.out.println("3. \ube14\ub85c\uadf8 \ubc30\ud3ec \uc0ad\uc81c");
            System.out.println("0. \ud504\ub85c\uadf8\ub7a8 \uc885\ub8cc");
            System.out.print("\uc785\ub825: ");
            final String trim;
            final String input = trim = scanner.nextLine().trim();
            switch (trim) {
                case "1": {
                    System.out.println("[PC \uace0\uc720 \ubc88\ud638] : " + Config.PC_HW_CODE);
                    System.out.println("[GPT API KEY] : " + Config.CHAT_GPT_API_KEY);
                    (SketchNBlogAuto.sketchNBlogAuto = new SketchNBlogAuto(SketchNBlogAuto.driverModule.setupChromeDriver())).setChromeDriver();
                    SketchNBlogAuto.sketchNBlogAuto.setOptionsForChrome();
                    continue;
                }
                case "2": {
                    System.out.println("[\ube14\ub85c\uadf8 \ubc30\ud3ec \uc790\ub3d9\ud654 \uc2dc\uc791]");
                    System.out.println("[\ud574\uc57c\ud560 \uc791\uc5c5] : ");
                    final BlogTask blogTask = new BlogTask();
                    while (true) {
                        Label_0552: {
                            try {
                                while (api.getBlogTask(blogTask)) {
                                    ChromeTempCleaner.cleanupIfNeeded();
                                    if (blogTask.getNaverAccount() != null) {
                                        for (int i = 0; i < blogTask.getTaskCount(); ++i) {
                                            api.startBlogTask(blogTask);
                                            Util.Sleep(1500, "\ube14\ub85c\uadf8 \uc791\uc5c5\uc744 \uc704\ud558\uc5ec \uc11c\ubc84\ub85c\ubd80\ud130 \ub370\uc774\ud130\ub97c \uac00\uc838\uc635\ub2c8\ub2e4.");
                                            (SketchNBlogAuto.sketchNBlogAuto = new SketchNBlogAuto(SketchNBlogAuto.driverModule.setupChromeDriver())).setChromeDriver();
                                            final boolean success = SketchNBlogAuto.sketchNBlogAuto.run(blogTask);
                                            final String status = success ? "completed" : "fail";
                                            try {
                                                api.reportBlogTaskCompletion(blogTask.getTaskId(), Config.PC_HW_CODE, blogTask.getNaverAccount().getId(), blogTask.getResultPostUrl(), status, blogTask.getAssignmentId(), blogTask.getPostTitle());
                                            }
                                            catch (final NullPointerException npe) {
                                                System.out.println(npe.getMessage());
                                            }
                                        }
                                        break Label_0552;
                                    }
                                    System.out.println("\ud83d\udeab \uc0ac\uc6a9 \uac00\ub2a5\ud55c \uacc4\uc815\uc774 \uc5c6\uc5b4 \uc791\uc5c5\uc744 \uac74\ub108\ub701\ub2c8\ub2e4.");
                                }
                                System.out.println("\ud83d\udced \ud560\ub2f9\ub41c \uc791\uc5c5\uc774 \uc5c6\uc2b5\ub2c8\ub2e4. \ub2e4\uc74c \uccb4\ud06c\uae4c\uc9c0 \ub300\uae30 \uc911...");
                            }
                            catch (final IOException e) {
                                System.err.println("\u274c \uc11c\ubc84 \uc5f0\uacb0 \uc2e4\ud328: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                            }
                            catch (final Exception e2) {
                                e2.printStackTrace();
                                System.out.println(e2.getMessage());
                                System.out.println("\uc608\uc678 \ubc1c\uc0dd\ud558\uc5ec \ub2e4\uc2dc \uc2dc\uc791\ub429\ub2c8\ub2e4.");
                            }
                            try {
                                Thread.sleep(5000L);
                                continue;
                            }
                            catch (final InterruptedException e3) {
                                e3.printStackTrace();
                                continue Label_1020;
                            }
                        }
                    }
                    break;
                }
                case "3": {
                    System.out.println("[\uc6d0\uace0 \uc0ad\uc81c \uc791\uc5c5 \uc2dc\uc791]");
                    System.out.println("[\ud574\uc57c\ud560 \uc791\uc5c5] : ");
                    final List<NaverAccount> deletedAccounts = api.getDeletedTaskNaverAccounts();
                    final List<Integer> successTaskIds = new ArrayList<Integer>();
                    if (deletedAccounts.size() == 0 || deletedAccounts.isEmpty()) {
                        System.out.println("\uc0ad\uc81c \uc791\uc5c5\ud560 \uc6d0\uace0\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                        continue;
                    }
                    for (NaverAccount acc : deletedAccounts) {
                        System.out.println("\uc5c5\uccb4\uba85 : " + acc.getPlaceName());
                        System.out.println("\u2705 \uc544\uc774\ub514: " + acc.getId());
                        System.out.println("\ud83d\udd17 URL: " + acc.getBlogUrl());
                        if (SysUtil.isWindows()) {
                            try {
                                Thread.sleep(2000L);
                                Util.toggleMobileData();
                                Thread.sleep(5000L);
                                String ip2 = null;
                                for (int maxAttempts = 3, attempt = 1; attempt <= maxAttempts; ++attempt) {
                                    ip2 = Util.getPublicIp();
                                    System.out.println("\ud83d\udcf6 [\uc2dc\ub3c4 " + attempt + "] \ud604\uc7ac IP: " + ip2);
                                    if (ip2 != null && !ip2.isBlank() && !ip2.startsWith("ERROR")) {
                                        break;
                                    }
                                    if (attempt < maxAttempts) {
                                        System.out.println("\ud83d\udd01 IP \ud655\uc778 \uc7ac\uc2dc\ub3c4 \uc911... (" + attempt + "/" + maxAttempts);
                                        Thread.sleep(5000L);
                                    }
                                }
                                if (ip2 == null || ip2.isBlank() || ip2.startsWith("ERROR")) {
                                    System.err.println("\u274c \ucd5c\ub300 \uc7ac\uc2dc\ub3c4\uc5d0\ub3c4 IP \ud655\uc778 \uc2e4\ud328. \ud504\ub85c\uadf8\ub7a8 \uc885\ub8cc.");
                                    System.exit(1);
                                }
                            }
                            catch (final IOException e4) {
                                System.err.println("\u274c IP \uc870\ud68c \uc911 IOException \ubc1c\uc0dd: " + e4.getMessage());
                                System.exit(1);
                            }
                            catch (final Exception e5) {
                                System.err.println("\u274c \ub124\ud2b8\uc6cc\ud06c \ub9ac\uc14b \uc911 \uc608\uc678 \ubc1c\uc0dd: " + e5.getMessage());
                                System.exit(1);
                            }
                        }
                        (SketchNBlogAuto.sketchNBlogAuto = new SketchNBlogAuto(SketchNBlogAuto.driverModule.setupChromeDriver())).setChromeDriver();
                        final boolean success2 = SketchNBlogAuto.sketchNBlogAuto.runPostDelete(acc);
                        if (success2 && acc.getTaskId() != null) {
                            successTaskIds.add(acc.getTaskId());
                        }
                    }
                    if (!successTaskIds.isEmpty()) {
                        api.reportDeletionCompleted(successTaskIds);
                        continue;
                    }
                    continue;
                }
                case "0": {
                    break Label_1020;
                }
                default: {
                    System.out.println("\u274c \uc720\ud6a8\ud558\uc9c0 \uc54a\uc740 \uc785\ub825\uc785\ub2c8\ub2e4. 0, 1 \ub610\ub294 2\ub97c \uc785\ub825\ud558\uc138\uc694.");
                    continue;
                }
            }
        }
        System.out.println("\ud504\ub85c\uadf8\ub7a8\uc744 \uc885\ub8cc\ud569\ub2c8\ub2e4.");
        scanner.close();
    }
    
    private boolean isRunningFromJar() {
        final String className = this.getClass().getName().replace('.', '/');
        final String classJarPath = this.getClass().getResource("/" + className + ".class").toString();
        return classJarPath.startsWith("jar:");
    }
    
    public boolean run(final BlogTask blogTask) {
        try {
            Path promptPath = Paths.get("src/main/resources/prompt_restaurant_auto_image.txt", new String[0]);
            if (this.isRunningFromJar()) {
                final Path jarDir = Paths.get(System.getProperty("java.class.path"), new String[0]).toAbsolutePath().getParent();
                promptPath = jarDir.resolve("prompt_restaurant_auto_image.txt");
            }
            final PromptLoader loader = new PromptLoader(promptPath);
            final String finalPrompt = loader.buildPrompt(blogTask.getPrompt(), blogTask.getPlaceName(), blogTask.getPlaceAddress(), blogTask.getMainKeyword(), blogTask.getBottomTagsString(), blogTask.getForbiddenWordsString());
            String responseText = null;
            BlogPost blogPost = null;
            final int maxRetries = 10;
            boolean validResponse = false;
            for (int attempt = 1; attempt <= maxRetries; ++attempt) {
                System.out.println("\ud83d\udd01 GPT \uc751\ub2f5 \uc2dc\ub3c4 " + attempt + "/" + maxRetries);
                responseText = ChatGPTClient.generateBlogPost(finalPrompt);
                System.out.println("\ud83e\udde0 GPT \uc751\ub2f5:\n" + responseText);
                blogPost = BlogContentLoader.getBlogPostByAi(responseText);
                if (blogPost != null && !blogPost.getTitle().isEmpty() && !blogPost.getBody().isEmpty()) {
                    validResponse = true;
                    break;
                }
                System.out.println("\u26a0\ufe0f GPT \uc751\ub2f5 \ube44\uc815\uc0c1, \uc7ac\uc2dc\ub3c4\ud569\ub2c8\ub2e4...");
            }
            if (!validResponse) {
                System.out.println("\u274c 10\ud68c \uc2dc\ub3c4 \ud6c4\uc5d0\ub3c4 GPT \uc751\ub2f5\uc774 \uc720\ud6a8\ud558\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.");
                return false;
            }
            System.out.println("\u2705 \uc720\ud6a8\ud55c GPT \uc751\ub2f5\uc744 \ubc1b\uc558\uc2b5\ub2c8\ub2e4.");
            this.setOptions(blogTask.getNaverAccount());
            final GoogleAuthSetting googleAuthSetting = new GoogleAuthSetting(this.chromeDriver);
            googleAuthSetting.disableClipboardDirectV5Fix();
            this.nBlogModuleByAI = new NBlogModuleByAI(this.chromeDriver, blogTask.getNaverAccount(), blogPost, blogTask);
            final boolean result = this.nBlogModuleByAI.runNBlog();
            if (!result) {
                System.out.println("\ud83d\udeab runNBlog \uc2e4\ud328\ub85c \ud574\ub2f9 \uc791\uc5c5\uc740 \uac74\ub108\ub701\ub2c8\ub2e4.");
            }
            return result;
        }
        catch (final IOException e) {
            e.printStackTrace();
            return false;
        }
        catch (final URISyntaxException e2) {
            return false;
        }
        finally {
            if (this.chromeDriver != null) {
                try {
                    this.chromeDriver.quit();
                }
                catch (final Exception e3) {
                    System.out.println("\u2757 chromeDriver quit \uc911 \uc608\uc678: " + e3.getMessage());
                }
                this.chromeDriver = null;
            }
            try {
                final String userDataDir = Paths.get(System.getProperty("user.dir"), "SketchChrome").toString();
                final String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("taskkill /F /IM chrome.exe");
                }
                else {
                    Runtime.getRuntime().exec("pkill -f \"" + userDataDir);
                }
            }
            catch (final IOException e4) {
                System.err.println("\u2757 Chrome \ud504\ub85c\uc138\uc2a4 \uc885\ub8cc \uc2e4\ud328: " + e4.getMessage());
            }
            Util.Sleep(2000, "\ud504\ub85c\uc138\uc2a4 \uc885\ub8cc \uc911\uc785\ub2c8\ub2e4.");
        }
    }
    
    private boolean runPostDelete(final NaverAccount naverAccount) {
        try {
            this.setOptions(naverAccount);
            final NBlogPostDelete nBlogPostDelete = new NBlogPostDelete(this.chromeDriver, naverAccount);
            final boolean result = nBlogPostDelete.runNBlogPostDelete();
            return result;
        }
        catch (final Exception e) {
            return false;
        }
        finally {
            if (this.chromeDriver != null) {
                try {
                    this.chromeDriver.quit();
                }
                catch (final Exception e2) {
                    System.out.println("\u2757 chromeDriver quit \uc911 \uc608\uc678: " + e2.getMessage());
                }
                this.chromeDriver = null;
            }
        }
    }
    
    private void setChromeDriver() throws InterruptedException {
        final String projectRoot = System.getProperty("user.dir");
        System.setProperty("webdriver.chrome.driver", this.driverPath);
        System.setProperty("webdriver.chrome.logfile", projectRoot + "/drivers/driverlog.log");
        System.setProperty("webdriver.chrome.verboseLogging", "true");
    }
    
    private void setupJavascriptExecutor(final ChromeDriver chromeDriver) {
        (this.jsExecutor = chromeDriver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})", new Object[0]);
        this.jsExecutor.executeScript("window.navigator.chrome = { runtime: {} };", new Object[0]);
        this.jsExecutor.executeScript("Object.defineProperty(navigator, 'languages', {get: () => ['ko-KR', 'ko']});", new Object[0]);
    }
    
    private void setRandomUserAgent() {
        final String chromeVersion = SketchNBlogAuto.driverModule.getChromeVersion(false) + ".0.0.0";
        final List<String> userAgents = Arrays.asList("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + chromeVersion + " Safari/537.36", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + chromeVersion + " Safari/537.36", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + chromeVersion + " Safari/537.36", "Mozilla/5.0 (Windows NT 10.0; Win64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + chromeVersion + " Safari/537.36", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + chromeVersion + " Safari/537.36");
        final String randomUserAgent = userAgents.get(new Random().nextInt(userAgents.size()));
        this.options.addArguments("--user-agent=" + randomUserAgent);
        System.out.println("\uc124\uc815\ub41c User-Agent: " + randomUserAgent);
    }
    
    private void setOptionsForChrome() {
        this.options = new ChromeOptions();
        if (SketchNBlogAuto.isHeadlessMode) {
            this.options.addArguments("--headless");
        }
        this.options.addArguments("--disable-gpu", "--disable-save-password-bubble", "--disable-notifications", "--disable-translate", "--disable-infobars", "--disable-popup-blocking");
        this.options.setCapability("goog:loggingPrefs", new HashMap<String, Object>() {
            {
                ((HashMap<String, String>)this).put("browser", "ALL");
            }
        });
        this.options.addArguments("--enable-features=ClipboardReadWrite");
        String userDataDir;
        if (this.isRunningFromJar()) {
            userDataDir = Paths.get(getJarDirectory(), "SketchChrome").toString();
        }
        else {
            userDataDir = Paths.get(System.getProperty("user.dir"), "SketchChrome").toString();
        }
        final File userDataFolder = new File(userDataDir);
        boolean isFirstTime = false;
        if (!userDataFolder.exists()) {
            final boolean created = userDataFolder.mkdirs();
            if (created) {
                isFirstTime = true;
                System.out.println("\ud83d\udcc1 SketchChrome \ud3f4\ub354 \uc0dd\uc131\ub428: " + userDataDir);
            }
        }
        this.options.addArguments("--user-data-dir=" + userDataDir);
        System.out.println("\u2705 \uc0ac\uc6a9\ud560 \uc720\uc800 \ub370\uc774\ud130 \ub514\ub809\ud1a0\ub9ac: " + userDataDir);
        if (isFirstTime) {
            this.setupJavascriptExecutor(this.chromeDriver = new ChromeDriverBuilder().build(this.options, this.driverPath));
            System.out.println("\ud83c\udf10 [\ucd5c\ucd08 \uc2e4\ud589] \uad8c\ud55c \uc124\uc815\uc744 \uc704\ud55c \ud398\uc774\uc9c0\ub97c \uc5fd\ub2c8\ub2e4. \uc218\ub3d9 \uc870\uc791 \ud6c4 Enter\ub97c \ub204\ub974\uc138\uc694.");
            this.chromeDriver.get("https://www.naver.com");
            System.out.println("\ud83d\udc49 \ud074\ub9bd\ubcf4\ub4dc \uad8c\ud55c\uc744 \ud5c8\uc6a9 \ud6c4, Enter \ud0a4\ub97c \ub20c\ub7ec\uc8fc\uc138\uc694...");
            new Scanner(System.in).nextLine();
            this.chromeDriver.quit();
            this.chromeDriver = null;
        }
    }
    
    private void setOptions(final NaverAccount account) {
        this.options = new ChromeOptions();
        if (SketchNBlogAuto.isHeadlessMode) {
            this.options.addArguments("--headless");
        }
        this.options.addArguments("--disable-gpu", "--disable-save-password-bubble", "--disable-notifications", "--disable-translate", "--disable-infobars", "--disable-popup-blocking");
        this.options.setCapability("goog:loggingPrefs", new HashMap<String, Object>() {
            {
                ((HashMap<String, String>)this).put("browser", "ALL");
            }
        });
        this.options.addArguments("--enable-features=ClipboardReadWrite");
        final Map<String, Object> prefs = new HashMap<String, Object>();
        prefs.put("profile.default_content_setting_values.clipboard", 1);
        prefs.put("profile.default_content_setting_values.notifications", 1);
        this.options.setExperimentalOption("prefs", prefs);
        if (account != null && account.getProxyIp() != null && !account.getProxyIp().isEmpty()) {
            final String proxy = account.getProxyIp();
            this.options.addArguments("--proxy-server=" + proxy);
            System.out.println("\ud83c\udf10 Proxy \uc124\uc815\ub428: " + proxy);
        }
        this.setRandomUserAgent();
        this.setupJavascriptExecutor(this.chromeDriver = new ChromeDriverBuilder().build(this.options, this.driverPath));
        this.chromeDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(180L));
        this.chromeDriver.manage().window().maximize();
        Util.Sleep(1000, "\ud398\uc774\uc9c0 \ub85c\ub4dc \ub300\uae30 \uc911");
    }
    
    public static void setupGlobalExceptionLogger() {
        // 
        // This method could not be decompiled.
        // 
        // Original Bytecode:
        // 
        //     5: invokestatic    java/lang/Thread.setDefaultUncaughtExceptionHandler:(Ljava/lang/Thread$UncaughtExceptionHandler;)V
        //     8: return         
        // 
        // The error that occurred was:
        // 
        // java.lang.IllegalStateException: Could not infer any expression.
        //     at com.strobel.decompiler.ast.TypeAnalysis.runInference(TypeAnalysis.java:382)
        //     at com.strobel.decompiler.ast.TypeAnalysis.run(TypeAnalysis.java:95)
        //     at com.strobel.decompiler.ast.AstOptimizer.optimize(AstOptimizer.java:344)
        //     at com.strobel.decompiler.ast.AstOptimizer.optimize(AstOptimizer.java:42)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.createMethodBody(AstMethodBodyBuilder.java:206)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.createMethodBody(AstMethodBodyBuilder.java:93)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createMethodBody(AstBuilder.java:868)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createMethod(AstBuilder.java:761)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.addTypeMembers(AstBuilder.java:638)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createTypeCore(AstBuilder.java:605)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createTypeNoCache(AstBuilder.java:195)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createType(AstBuilder.java:162)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.addType(AstBuilder.java:137)
        //     at com.strobel.decompiler.languages.java.JavaLanguage.buildAst(JavaLanguage.java:71)
        //     at com.strobel.decompiler.languages.java.JavaLanguage.decompileType(JavaLanguage.java:59)
        //     at com.strobel.decompiler.DecompilerDriver.decompileType(DecompilerDriver.java:334)
        //     at com.strobel.decompiler.DecompilerDriver.decompileJar(DecompilerDriver.java:255)
        //     at com.strobel.decompiler.DecompilerDriver.main(DecompilerDriver.java:130)
        // 
        throw new IllegalStateException("An error occurred while decompiling this method.");
    }
    
    public static String getJarDirectory() {
        try {
            return new File(SketchNBlogAuto.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getAbsolutePath();
        }
        catch (final Exception e) {
            return ".";
        }
    }
    
    public static void killChromeIfRunning() {
        try {
            final String userDataDir = Paths.get(System.getProperty("user.dir"), "SketchChrome").toString();
            final String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec("taskkill /F /IM chrome.exe");
            }
            else {
                Runtime.getRuntime().exec("pkill -f \"" + userDataDir);
            }
            Util.Sleep(2000, "\ud83d\udd2a \uae30\uc874 Chrome \ud504\ub85c\uc138\uc2a4 \uc885\ub8cc \uc911...");
        }
        catch (final IOException e) {
            System.err.println("\u2757 \ucd08\uae30 \ud06c\ub86c \uc885\ub8cc \uc2e4\ud328: " + e.getMessage());
        }
    }
    
    static {
        driverModule = new DriverModule();
        SketchNBlogAuto.isHeadlessMode = false;
        SketchNBlogAuto.isExistChromeData = false;
        SketchNBlogAuto.isFirstTime = false;
    }
}
