// 
// Decompiled by Procyon v0.6.0
// 

package util;

import java.nio.file.Path;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SysUtil
{
    public static boolean isWindows() {
        final String osName = getOsName();
        return osName != null && osName.startsWith("Windows");
    }
    
    public static boolean isMacOs() {
        final String osName = getOsName();
        return osName != null && osName.startsWith("Mac");
    }
    
    public static boolean isLinux() {
        final String osName = getOsName();
        return (osName != null && osName.startsWith("Linux")) || (!isWindows() && !isMacOs());
    }
    
    public static String getOsName() {
        return System.getProperty("os.name");
    }
    
    public static String getOsLowerCaseName() {
        return System.getProperty("os.name").toLowerCase();
    }
    
    public static List<String> getPath() {
        final String sep = System.getProperty("path.separator");
        final String paths = System.getenv("PATH");
        return new ArrayList<String>(Arrays.asList(paths.split(sep)));
    }
    
    public static String getString(final String key) {
        return System.getenv(key);
    }
    
    public static String getVersionFromPath(final Path path) {
        final Path parent = path.getParent();
        return (parent != null) ? parent.getFileName().toString() : "";
    }
}
