// 
// Decompiled by Procyon v0.6.0
// 

package chromeModule;

import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import util.SysUtil;
import java.util.Collection;
import java.io.IOException;
import java.net.ServerSocket;
import org.openqa.selenium.chrome.ChromeDriver;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import com.alibaba.fastjson.JSON;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.util.HashMap;
import java.io.File;
import java.util.Locale;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import org.openqa.selenium.chrome.ChromeOptions;
import java.util.ArrayList;
import java.util.List;

public class ChromeDriverBuilder
{
    private boolean _keepUserDataDir;
    private String _userDataDir;
    private String _binaryLocation;
    private List<String> args;
    
    public ChromeDriverBuilder() {
        this._keepUserDataDir = false;
        this._userDataDir = null;
        this._binaryLocation = null;
        this.args = new ArrayList<String>();
    }
    
    private void buildPatcher(final String driverExecutablePath) throws RuntimeException {
        final Patcher patcher = new Patcher(driverExecutablePath);
        try {
            patcher.Auto();
        }
        catch (final Exception e) {
            throw new RuntimeException("patcher cdc replace fail");
        }
    }
    
    private ChromeOptions setHostAndPort(final ChromeOptions chromeOptions) throws RuntimeException {
        String debugHost = null;
        int debugPort = -1;
        if (this.args != null && this.args.size() > 0) {
            for (final String arg : this.args) {
                if (arg.contains("--remote-debugging-host")) {
                    try {
                        debugHost = arg.split("=")[1];
                    }
                    catch (final Exception ex) {}
                }
                if (arg.contains("--remote-debugging-port")) {
                    try {
                        debugPort = Integer.parseInt(arg.split("=")[1]);
                    }
                    catch (final Exception ex2) {}
                }
            }
        }
        if (debugHost == null) {
            debugHost = "127.0.0.1";
            chromeOptions.addArguments("--remote-debugging-host=" + debugHost);
        }
        if (debugPort == -1) {
            debugPort = this.findFreePort();
        }
        if (debugPort == -1) {
            throw new RuntimeException("free port not find");
        }
        chromeOptions.addArguments("--remote-debugging-port=" + String.valueOf(debugPort));
        try {
            final Field experimentalOptions = chromeOptions.getClass().getSuperclass().getDeclaredField("experimentalOptions");
            experimentalOptions.setAccessible(true);
            final Map<String, Object> experimentals = (Map<String, Object>)experimentalOptions.get(chromeOptions);
            if (experimentals != null && experimentals.get("debuggerAddress") != null) {
                return chromeOptions;
            }
        }
        catch (final Exception ex3) {}
        chromeOptions.setExperimentalOption("debuggerAddress", debugHost + ":" + String.valueOf(debugPort));
        return chromeOptions;
    }
    
    private ChromeOptions setUserDataDir(final ChromeOptions chromeOptions) throws RuntimeException {
        if (this.args != null) {
            for (final String arg : this.args) {
                if (arg.contains("--user-data-dir")) {
                    try {
                        this._userDataDir = arg.split("=")[1];
                    }
                    catch (final Exception ex) {}
                    break;
                }
            }
        }
        if (this._userDataDir == null || this._userDataDir.equals("")) {
            this._keepUserDataDir = false;
            try {
                this._userDataDir = Files.createTempDirectory("undetected_chrome_driver", (FileAttribute<?>[])new FileAttribute[0]).toString();
            }
            catch (final Exception e) {
                e.printStackTrace();
                throw new RuntimeException("temp user data dir create fail");
            }
            chromeOptions.addArguments("--user-data-dir=" + this._userDataDir);
        }
        else {
            this._keepUserDataDir = true;
        }
        return chromeOptions;
    }
    
    private ChromeOptions setLanguage(final ChromeOptions chromeOptions) {
        if (this.args != null) {
            for (final String arg : this.args) {
                if (arg.contains("--lang=")) {
                    return chromeOptions;
                }
            }
        }
        final String language = Locale.getDefault().getLanguage().replace("_", "-");
        chromeOptions.addArguments("--lang=" + language);
        return chromeOptions;
    }
    
    private ChromeOptions setBinaryLocation(final ChromeOptions chromeOptions, String binaryLocation) {
        if (binaryLocation == null) {
            try {
                binaryLocation = this._getChromePath();
            }
            catch (final Exception e) {
                throw new RuntimeException("chrome not find");
            }
            if (binaryLocation.equals("")) {
                throw new RuntimeException("chrome not find");
            }
            chromeOptions.setBinary(binaryLocation);
        }
        else {
            chromeOptions.setBinary(binaryLocation);
        }
        this._binaryLocation = binaryLocation;
        return chromeOptions;
    }
    
    private ChromeOptions suppressWelcome(final ChromeOptions chromeOptions, final boolean suppressWelcome) {
        if (suppressWelcome) {
            if (this.args != null) {
                if (!this.args.contains("--no-default-browser-check")) {
                    chromeOptions.addArguments("--no-default-browser-check");
                }
                if (!this.args.contains("--no-first-run")) {
                    chromeOptions.addArguments("--no-first-run");
                }
            }
            else {
                chromeOptions.addArguments("--no-default-browser-check", "--no-first-run");
            }
        }
        return chromeOptions;
    }
    
    private ChromeOptions setHeadless(final ChromeOptions chromeOptions, final boolean headless) {
        if (headless) {
            if (this.args != null) {
                if (!this.args.contains("--headless=new") || !this.args.contains("--headless=chrome")) {
                    chromeOptions.addArguments("--headless=new");
                }
                boolean hasWindowSize = false;
                for (final String arg : this.args) {
                    if (arg.contains("--window-size=")) {
                        hasWindowSize = true;
                        break;
                    }
                }
                if (!hasWindowSize) {
                    chromeOptions.addArguments("--window-size=1920,1080");
                }
                if (!this.args.contains("--start-maximized")) {
                    chromeOptions.addArguments("--start-maximized");
                }
                if (!this.args.contains("--no-sandbox")) {
                    chromeOptions.addArguments("--no-sandbox");
                }
            }
            else {
                chromeOptions.addArguments("--headless=new");
                chromeOptions.addArguments("--window-size=1920,1080");
                chromeOptions.addArguments("--start-maximized");
                chromeOptions.addArguments("--no-sandbox");
            }
        }
        return chromeOptions;
    }
    
    private ChromeOptions setLogLevel(final ChromeOptions chromeOptions) {
        if (this.args != null) {
            for (final String arg : this.args) {
                if (arg.contains("--log-level=")) {
                    return chromeOptions;
                }
            }
        }
        chromeOptions.addArguments("--log-level=0");
        return chromeOptions;
    }
    
    private void handlePrefs(final String userDataDir, final Map<String, Object> prefs) throws RuntimeException {
        final String defaultPath = userDataDir + File.separator + "Default";
        if (!new File(defaultPath).exists()) {
            new File(defaultPath).mkdirs();
        }
        Map<String, Object> newPrefs = new HashMap<String, Object>(prefs);
        final String prefsFile = defaultPath + File.separator + "Preferences";
        if (new File(prefsFile).exists()) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(prefsFile), StandardCharsets.ISO_8859_1));
                final StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append("\n");
                }
                newPrefs = JSON.parseObject(stringBuilder.toString()).getInnerMap();
            }
            catch (final Exception e) {
                throw new RuntimeException("Default Preferences dir not found");
            }
            finally {
                if (br != null) {
                    try {
                        br.close();
                    }
                    catch (final Exception ex) {}
                }
            }
            try {
                for (final Map.Entry<String, Object> pref : prefs.entrySet()) {
                    this.undotMerge(pref.getKey(), pref.getValue(), newPrefs);
                }
            }
            catch (final Exception e) {
                throw new RuntimeException("Prefs merge failed");
            }
            try (final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prefsFile), StandardCharsets.ISO_8859_1))) {
                bw.write(JSON.toJSONString(newPrefs));
                bw.flush();
            }
            catch (final Exception e) {
                throw new RuntimeException("Prefs write to file failed");
            }
        }
    }
    
    private ChromeOptions fixExitType(final ChromeOptions chromeOptions) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            final String filePath = this._userDataDir + File.separator + "Default" + File.separator + "Preferences";
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.ISO_8859_1));
            final StringBuilder jsonStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStr.append(line);
                jsonStr.append("\n");
            }
            reader.close();
            String json = jsonStr.toString();
            final Pattern pattern = Pattern.compile("(?<=exit_type\":)(.*?)(?=,)");
            final Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.ISO_8859_1));
                json = json.replace(matcher.group(), "null");
                writer.write(json);
                writer.close();
            }
        }
        catch (final Exception ex) {}
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (final Exception ex2) {}
            }
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (final Exception ex3) {}
            }
        }
        return chromeOptions;
    }
    
    private Process createBrowserProcess(final ChromeOptions chromeOptions, final boolean needPrintChromeInfo) throws RuntimeException {
        this.LoadChromeOptionsArgs(chromeOptions);
        if (this.args == null) {
            throw new RuntimeException("can't open browser, args not found");
        }
        Process p = null;
        try {
            this.args.add(0, this._binaryLocation);
            p = new ProcessBuilder(this.args).start();
        }
        catch (final Exception e) {
            throw new RuntimeException("chrome open fail");
        }
        final Process browser = p;
        final Thread outputThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final BufferedReader br = new BufferedReader(new InputStreamReader(browser.getInputStream()));
                    String buff = null;
                    while ((buff = br.readLine()) != null) {
                        System.out.println(buff);
                    }
                }
                catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
        final Thread errorPutThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final BufferedReader er = new BufferedReader(new InputStreamReader(browser.getErrorStream()));
                    String errors = null;
                    while ((errors = er.readLine()) != null) {
                        System.out.println(errors);
                    }
                }
                catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
        if (needPrintChromeInfo) {
            outputThread.start();
            errorPutThread.start();
        }
        return browser;
    }
    
    public ChromeDriver build(final ChromeOptions options, final String driverExecutablePath, final String binaryLocation, final boolean headless, final boolean suppressWelcome, final boolean needPrintChromeInfo, final Map<String, Object> prefs) throws RuntimeException {
        if (driverExecutablePath == null) {
            throw new RuntimeException("driverExecutablePath is required.");
        }
        this.LoadChromeOptionsArgs(options);
        this.buildPatcher(driverExecutablePath);
        ChromeOptions chromeOptions = options;
        if (chromeOptions == null) {
            chromeOptions = new ChromeOptions();
        }
        chromeOptions = this.setHostAndPort(chromeOptions);
        chromeOptions = this.setUserDataDir(chromeOptions);
        chromeOptions = this.setLanguage(chromeOptions);
        chromeOptions = this.setBinaryLocation(chromeOptions, binaryLocation);
        chromeOptions = this.suppressWelcome(chromeOptions, suppressWelcome);
        chromeOptions = this.setHeadless(chromeOptions, headless);
        chromeOptions = this.setLogLevel(chromeOptions);
        if (prefs != null) {
            this.handlePrefs(this._userDataDir, prefs);
        }
        chromeOptions = this.fixExitType(chromeOptions);
        final Process browser = this.createBrowserProcess(chromeOptions, needPrintChromeInfo);
        final UndetectedChromeDriver undetectedChromeDriver = new UndetectedChromeDriver(chromeOptions, headless, this._keepUserDataDir, this._userDataDir, browser);
        return undetectedChromeDriver;
    }
    
    public ChromeDriver build(final ChromeOptions options, final String driverExecutablePath, final String binaryLocation, final boolean suppressWelcome, final boolean needPrintChromeInfo) {
        boolean headless = false;
        try {
            final Field argsField = options.getClass().getSuperclass().getDeclaredField("args");
            argsField.setAccessible(true);
            final List<String> args = (List<String>)argsField.get(options);
            if (args.contains("--headless") || args.contains("--headless=new") || args.contains("--headless=chrome")) {
                headless = true;
            }
        }
        catch (final Exception ex) {}
        Map<String, Object> prefs = null;
        try {
            final Field argsField2 = options.getClass().getSuperclass().getDeclaredField("experimentalOptions");
            argsField2.setAccessible(true);
            final Map<String, Object> args2 = (Map<String, Object>)argsField2.get(options);
            if (args2.containsKey("prefs")) {
                prefs = new HashMap<String, Object>(args2.get("prefs"));
                args2.remove("prefs");
            }
        }
        catch (final Exception ex2) {}
        return this.build(options, driverExecutablePath, binaryLocation, headless, suppressWelcome, needPrintChromeInfo, prefs);
    }
    
    public ChromeDriver build(final ChromeOptions options, final String driverExecutablePath, final boolean suppressWelcome, final boolean needPrintChromeInfo) {
        return this.build(options, driverExecutablePath, null, suppressWelcome, needPrintChromeInfo);
    }
    
    public ChromeDriver build(final ChromeOptions options, final String driverExecutablePath) {
        return this.build(options, driverExecutablePath, true, false);
    }
    
    public ChromeDriver build(final String driverExecutablePath) {
        return this.build(null, driverExecutablePath);
    }
    
    private int findFreePort() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        }
        catch (final Exception e) {
            e.printStackTrace();
            return -1;
        }
        finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            }
            catch (final IOException ex) {}
        }
    }
    
    private void LoadChromeOptionsArgs(final ChromeOptions chromeOptions) {
        try {
            final Field argsField = chromeOptions.getClass().getSuperclass().getDeclaredField("args");
            argsField.setAccessible(true);
            this.args = new ArrayList<String>((Collection<? extends String>)argsField.get(chromeOptions));
        }
        catch (final Exception ex) {}
    }
    
    private String _getChromePath() throws RuntimeException {
        final String os = System.getProperties().getProperty("os.name");
        String chromeDataPath = null;
        final boolean IS_POSIX = SysUtil.isMacOs() || SysUtil.isLinux();
        final Set<String> possibles = new HashSet<String>();
        if (IS_POSIX) {
            final List<String> names = Arrays.asList("google-chrome", "chromium", "chromium-browser", "chrome", "google-chrome-stable");
            for (String path : SysUtil.getPath()) {
                for (String name : names) {
                    possibles.add(path + File.separator + name);
                }
            }
            if (SysUtil.isMacOs()) {
                possibles.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
                possibles.add("/Applications/Chromium.app/Contents/MacOS/Chromium");
            }
        }
        else {
            final List<String> paths = new ArrayList<String>();
            paths.add(SysUtil.getString("PROGRAMFILES"));
            paths.add(SysUtil.getString("PROGRAMFILES(X86)"));
            paths.add(SysUtil.getString("LOCALAPPDATA"));
            final List<String> middles = Arrays.asList("Google" + File.separator + "Chrome" + File.separator + "Application", "Google" + File.separator + "Chrome Beta" + File.separator + "Application", "Google" + File.separator + "Chrome Canary" + File.separator + "Application");
            for (String path2 : paths) {
                for (String middle : middles) {
                    possibles.add(path2 + File.separator + middle + File.separator + "chrome.exe");
                }
            }
        }
        for (final String possible : possibles) {
            final File file = new File(possible);
            if (file.exists() && file.canExecute()) {
                chromeDataPath = file.getAbsolutePath();
                break;
            }
        }
        if (chromeDataPath == null) {
            throw new RuntimeException("chrome not find in your pc, please use arg binaryLocation");
        }
        return chromeDataPath;
    }
    
    private void undotMerge(final String key, final Object value, final Map<String, Object> dict) {
        if (key.contains(".")) {
            final String[] splits = key.split("\\.", 2);
            final String k1 = splits[0];
            final String k2 = splits[1];
            if (!dict.containsKey(k1)) {
                dict.put(k1, new HashMap());
            }
            try {
                this.undotMerge(k2, value, dict.get(k1));
            }
            catch (final Exception ex) {}
            return;
        }
        dict.put(key, value);
    }
}
