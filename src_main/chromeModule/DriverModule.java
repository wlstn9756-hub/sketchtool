// 
// Decompiled by Procyon v0.6.0
// 

package chromeModule;

import java.util.stream.Stream;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.Comparator;
import java.nio.file.LinkOption;
import java.nio.file.Files;
import java.nio.file.FileVisitOption;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import util.SysUtil;
import io.github.bonigarcia.wdm.WebDriverManager;

public class DriverModule
{
    public String setupChromeDriver() {
        final String chromeVersion = this.getChromeVersion(false);
        if (chromeVersion == null) {
            System.out.println("Chrome \ubc84\uc804\uc744 \uac10\uc9c0\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return null;
        }
        final String projectRoot = System.getProperty("user.dir");
        final String downloadPath = projectRoot + "/drivers";
        WebDriverManager.chromedriver().driverVersion(chromeVersion).cachePath(downloadPath).setup();
        final String driverPath = this.findLatestChromeDriverPath(downloadPath);
        if (driverPath == null) {
            System.out.println("Failed to obtain ChromeDriver path. Check WebDriverManager configuration.");
            return null;
        }
        return driverPath;
    }
    
    public String getChromeVersion(final boolean fullVersion) {
        String version = null;
        try {
            ProcessBuilder processBuilder;
            if (SysUtil.getOsLowerCaseName().contains("win")) {
                processBuilder = new ProcessBuilder(new String[] { "reg", "query", "HKEY_CURRENT_USER\\Software\\Google\\Chrome\\BLBeacon", "/v", "version" });
            }
            else {
                if (!SysUtil.getOsLowerCaseName().contains("mac")) {
                    throw new UnsupportedOperationException("\uc9c0\uc6d0\ud558\uc9c0 \uc54a\ub294 \uc6b4\uc601 \uccb4\uc81c: " + SysUtil.getOsLowerCaseName());
                }
                processBuilder = new ProcessBuilder(new String[] { "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome", "--version" });
            }
            final Process process = processBuilder.start();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (SysUtil.getOsLowerCaseName().contains("win")) {
                    if (!line.contains("version")) {
                        continue;
                    }
                    version = line.split("\\s+")[line.split("\\s+").length - 1];
                }
                else {
                    if (!SysUtil.getOsLowerCaseName().contains("mac") || !line.contains("Google Chrome")) {
                        continue;
                    }
                    version = line.replace("Google Chrome ", "").trim();
                }
            }
            if (version != null && !fullVersion && (SysUtil.getOsLowerCaseName().contains("mac") || SysUtil.getOsLowerCaseName().contains("win"))) {
                version = version.split("\\.")[0];
            }
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        return version;
    }
    
    private String findLatestChromeDriverPath(final String rootPath) {
        try (final Stream<Path> paths = Files.walk(Paths.get(rootPath, new String[0]), new FileVisitOption[0])) {
            return paths.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).filter(path -> path.getFileName().toString().startsWith("chromedriver")).sorted(Comparator.comparing((Function<? super Object, ? extends Comparable>)DriverModule::getVersionFromPath).reversed()).map((Function<? super Path, ? extends String>)Path::toString).findFirst().orElse(null);
        }
        catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private String findClosestChromeDriverPath(final String rootPath, final String chromeVersion) {
        try (final Stream<Path> paths = Files.walk(Paths.get(rootPath, new String[0]), new FileVisitOption[0])) {
            return paths.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).filter(path -> path.getFileName().toString().startsWith("chromedriver")).min(Comparator.comparing(path -> this.versionDifference(getVersionFromPath(path), chromeVersion))).map((Function<? super Path, ? extends String>)Path::toString).orElse(null);
        }
        catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private int versionDifference(final String driverVersion, final String chromeVersion) {
        try {
            final int driverMajorVersion = Integer.parseInt(driverVersion.split("\\.")[0]);
            final int chromeMajorVersion = Integer.parseInt(chromeVersion.split("\\.")[0]);
            return Math.abs(driverMajorVersion - chromeMajorVersion);
        }
        catch (final NumberFormatException e) {
            e.printStackTrace();
            return Integer.MAX_VALUE;
        }
    }
    
    public static String getVersionFromPath(final Path path) {
        final Path parent = path.getParent();
        return (parent != null) ? parent.getFileName().toString() : "";
    }
}
