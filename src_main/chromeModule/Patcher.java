// 
// Decompiled by Procyon v0.6.0
// 

package chromeModule;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.io.File;

public class Patcher
{
    private String _driverExecutablePath;
    
    public Patcher(final String _driverExecutablePath) {
        this._driverExecutablePath = _driverExecutablePath;
    }
    
    public void Auto() throws Exception {
        if (!this.isBinaryPatched()) {
            this.patchExe();
        }
    }
    
    private boolean isBinaryPatched() throws Exception {
        if (this._driverExecutablePath == null) {
            throw new RuntimeException("driverExecutablePath is required.");
        }
        final File file = new File(this._driverExecutablePath);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("undetected chromedriver")) {
                    return true;
                }
            }
        }
        catch (final Exception e) {
            e.printStackTrace();
            if (br != null) {
                try {
                    br.close();
                }
                catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
        finally {
            if (br != null) {
                try {
                    br.close();
                }
                catch (final Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
        return false;
    }
    
    private int patchExe() {
        final int linect = 0;
        final String replacement = this.genRandomCdc();
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(this._driverExecutablePath, "rw");
            final byte[] buffer = new byte[1024];
            final StringBuilder stringBuilder = new StringBuilder();
            long read = 0L;
            while (true) {
                read = file.read(buffer, 0, buffer.length);
                if (read == 0L || read == -1L) {
                    break;
                }
                stringBuilder.append(new String(buffer, 0, (int)read, StandardCharsets.ISO_8859_1));
            }
            final String content = stringBuilder.toString();
            final Pattern pattern = Pattern.compile("\\{window\\.cdc.*?;\\}");
            final Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                final String group = matcher.group();
                final StringBuilder newTarget = new StringBuilder("{console.log(\"undetected chromedriver 1337!\"}");
                for (int k = group.length() - newTarget.length(), i = 0; i < k; ++i) {
                    newTarget.append(" ");
                }
                final String newContent = content.replace(group, newTarget.toString());
                file.seek(0L);
                file.write(newContent.getBytes(StandardCharsets.ISO_8859_1));
            }
        }
        catch (final Exception e) {
            e.printStackTrace();
            if (file != null) {
                try {
                    file.close();
                }
                catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
        finally {
            if (file != null) {
                try {
                    file.close();
                }
                catch (final Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
        return linect;
    }
    
    private String genRandomCdc() {
        final String chars = "abcdefghijklmnopqrstuvwxyz";
        final Random random = new Random();
        final char[] cdc = new char[27];
        for (int i = 0; i < 27; ++i) {
            cdc[i] = chars.charAt(random.nextInt(chars.length()));
        }
        return new String(cdc);
    }
}
