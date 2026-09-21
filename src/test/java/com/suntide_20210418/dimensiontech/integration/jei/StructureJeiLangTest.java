package com.suntide_20210418.dimensiontech.integration.jei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * A JEI key no lang file defines renders as its own raw path in game. Nothing catches that at
 * compile time and it is easy to miss in a tooltip, so every key the integration layer can print is
 * checked against both providers - source to source, without loading anything.
 */
class StructureJeiLangTest {
    private static final Path INTEGRATION =
            Path.of("src/main/java/com/suntide_20210418/dimensiontech/integration/jei");
    private static final List<Path> PROVIDERS =
            List.of(
                    Path.of(
                            "src/main/java/com/suntide_20210418/dimensiontech/datagen/ModEnusLangProvider.java"),
                    Path.of(
                            "src/main/java/com/suntide_20210418/dimensiontech/datagen/ModZhcnLangProvider.java"));

    private static final Pattern KEY =
            Pattern.compile("\"(jei\\.dimension_tech\\.[A-Za-z0-9_.]+)\"");

    @Test
    void everyKeyTheIntegrationCanPrintIsTranslated() throws Exception {
        List<String> keys = keys();
        assertFalse(keys.isEmpty(), "the integration layer must declare at least one JEI key");

        for (Path provider : PROVIDERS) {
            String source = Files.readString(provider);
            for (String key : keys) {
                assertTrue(
                        source.contains("\"" + key + "\""),
                        provider.getFileName() + " is missing " + key);
            }
        }
    }

    private static List<String> keys() throws Exception {
        List<String> keys = new ArrayList<>();
        try (Stream<Path> paths = Files.list(INTEGRATION)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".java")).toList()) {
                Matcher matcher = KEY.matcher(Files.readString(path));
                while (matcher.find()) {
                    if (!keys.contains(matcher.group(1))) keys.add(matcher.group(1));
                }
            }
        }
        return keys;
    }
}
