package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards the "no outlined text" rule across every GUI in this mod.
 *
 * <p>{@code GuiGraphics.drawCenteredString} has no overload that omits the drop shadow — all three of
 * its variants forward to a five-argument {@code drawString}, which hard-codes
 * {@code dropShadow = true}. Centring a string therefore forced an outline onto it. {@link GuiText}
 * exists to avoid that, so a reappearance of the vanilla call, or of an explicit {@code true} shadow
 * flag, is exactly the regression these tests look for.
 */
class GuiTextShadowTest {
    private static final Path GUI =
            Path.of("src/main/java/com/suntide_20210418/dimensiontech/client/gui");

    /** {@link GuiText} explains the rule in prose, so comments are stripped before searching. */
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\n]*");

    @Test
    void nothingCentresTextThroughTheVanillaCall() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : javaFiles()) {
            if (codeOf(file).contains("drawCenteredString(")) offenders.add(file.toString());
        }

        assertTrue(
                offenders.isEmpty(),
                "these would draw centred text with an outline: " + offenders);
    }

    @Test
    void nothingAsksForADropShadowExplicitly() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : javaFiles()) {
            for (String line : codeOf(file).split("\n")) {
                if (line.contains("drawString(") && line.trim().endsWith("true);")) {
                    offenders.add(file + " :: " + line.trim());
                }
            }
        }

        assertTrue(
                offenders.isEmpty(),
                "these draw text with dropShadow = true: " + offenders);
    }

    private static List<Path> javaFiles() throws IOException {
        try (Stream<Path> files = Files.walk(GUI)) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private static String codeOf(Path file) throws IOException {
        String source = Files.readString(file);
        return LINE_COMMENT.matcher(BLOCK_COMMENT.matcher(source).replaceAll("")).replaceAll("");
    }
}
