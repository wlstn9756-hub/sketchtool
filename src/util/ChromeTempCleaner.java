// 
// Decompiled by Procyon v0.6.0
// 

package util;

import java.nio.file.AccessDeniedException;
import java.util.Comparator;
import java.nio.file.FileVisitOption;
import java.util.stream.Stream;
import java.util.function.Consumer;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.nio.file.OpenOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.time.LocalDate;
import java.nio.file.Path;

public class ChromeTempCleaner
{
    private static final Path CLEANUP_DATE_FILE;
    
    public static LocalDate loadLastCleanupDate() {
        try {
            if (Files.exists(ChromeTempCleaner.CLEANUP_DATE_FILE, new LinkOption[0])) {
                final String content = Files.readString(ChromeTempCleaner.CLEANUP_DATE_FILE).trim();
                return LocalDate.parse(content);
            }
        }
        catch (final Exception e) {
            System.err.println("\u274c \ub0a0\uc9dc \ud30c\uc77c \uc77d\uae30 \uc624\ub958: " + e.getMessage());
        }
        return null;
    }
    
    public static void saveLastCleanupDate(final LocalDate date) {
        try {
            Files.writeString(ChromeTempCleaner.CLEANUP_DATE_FILE, date.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (final IOException e) {
            System.err.println("\u274c \ub0a0\uc9dc \ud30c\uc77c \uc800\uc7a5 \uc624\ub958: " + e.getMessage());
        }
    }
    
    public static void clearChromeTempFiles() {
        final Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), new String[0]);
        System.out.println("\ud83e\uddf9 \ud06c\ub86c \uad00\ub828 \uc784\uc2dc\ud30c\uc77c \uc815\ub9ac \uc2dc\uc791: " + String.valueOf(tempDir.toAbsolutePath()));
        try (final Stream<Path> paths = Files.list(tempDir)) {
            paths.filter(p -> {
                final String name = p.getFileName().toString().toLowerCase();
                return name.startsWith("scoped_dir") || name.startsWith("chrome") || name.startsWith("cr_") || name.startsWith("undetected_chrome");
            }).forEach(ChromeTempCleaner::deleteRecursivelySafe);
            System.out.println("\u2705 \ud06c\ub86c \uc784\uc2dc\ud30c\uc77c \uc815\ub9ac \uc644\ub8cc");
        }
        catch (final IOException e) {
            System.err.println("\u274c \uc784\uc2dc\ud30c\uc77c \uc815\ub9ac \uc624\ub958: " + e.getMessage());
        }
    }
    
    private static void deleteRecursivelySafe(final Path root) {
        if (root == null || Files.notExists(root, new LinkOption[0])) {
            return;
        }
        try {
            final Stream<Path> walk = Files.walk(root, new FileVisitOption[0]);
            try {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    }
                    catch (final AccessDeniedException ex) {
                        System.out.println("\ud83d\udd12 \uc0ac\uc6a9\uc911(\uc7a0\uae40): " + String.valueOf(p) + " \u2192 \uac74\ub108\ub700");
                    }
                    catch (final IOException ex2) {
                        System.out.println("\u26a0\ufe0f \uc0ad\uc81c \uc2e4\ud328: " + String.valueOf(p) + " \u2192 " + ex2.getMessage());
                    }
                    return;
                });
                if (walk != null) {
                    walk.close();
                }
            }
            catch (final Throwable t) {
                if (walk != null) {
                    try {
                        walk.close();
                    }
                    catch (final Throwable exception) {
                        t.addSuppressed(exception);
                    }
                }
                throw t;
            }
        }
        catch (final IOException e) {
            System.out.println("\u26a0\ufe0f \uacbd\ub85c \ucc98\ub9ac \uc2e4\ud328: " + String.valueOf(root) + " \u2192 " + e.getMessage());
        }
    }
    
    public static void cleanupIfNeeded() {
        final LocalDate today = LocalDate.now();
        final LocalDate lastCleanupDate = loadLastCleanupDate();
        if (lastCleanupDate == null || !today.equals(lastCleanupDate)) {
            System.out.println("\ud83d\udd5b \uc790\uc815 \uc774\ud6c4 \uccab \uc791\uc5c5 \u2192 \ud06c\ub86c \uc784\uc2dc\ud30c\uc77c \uc815\ub9ac \uc2dc\uc791");
            clearChromeTempFiles();
            saveLastCleanupDate(today);
        }
        else {
            System.out.println("\u23ed\ufe0f \uc774\ubbf8 \uc624\ub298 \uc815\ub9ac\ub428 \u2192 \uac74\ub108\ub700 (" + String.valueOf(lastCleanupDate));
        }
    }
    
    static {
        CLEANUP_DATE_FILE = Paths.get("last_cleanup_date.txt", new String[0]);
    }
}
