// 
// Decompiled by Procyon v0.6.0
// 

package util;

import java.util.Iterator;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class PromptLoader
{
    private final String template;
    
    public PromptLoader(final Path promptPath) {
        try {
            final List<String> lines = Files.readAllLines(promptPath, StandardCharsets.UTF_8);
            final StringBuilder sb = new StringBuilder();
            for (final String line : lines) {
                final String trimmed = line.trim();
                if (!trimmed.startsWith("#")) {
                    if (trimmed.startsWith("//")) {
                        continue;
                    }
                    sb.append(line).append("\n");
                }
            }
            this.template = sb.toString().trim();
        }
        catch (final IOException e) {
            throw new RuntimeException("\u274c \ud504\ub86c\ud504\ud2b8 \ud30c\uc77c \ub85c\ub529 \uc2e4\ud328: " + e.getMessage(), (Throwable)e);
        }
    }
    
    public String buildPrompt(final String basePrompt, final String placeName, final String placeAddress, final String keyword, final String tags, final String forbiddenKeywords) {
        return this.template.replace("{{BASE_PROMPT}}", basePrompt).replace("{{PLACE_NAME}}", placeName).replace("{{PLACE_ADDRESS}}", placeAddress).replace("{{KEYWORD}}", keyword).replace("{{TAGS}}", tags).replace("{{FORBIDDEN_KEYWORDS}}", forbiddenKeywords);
    }
}
