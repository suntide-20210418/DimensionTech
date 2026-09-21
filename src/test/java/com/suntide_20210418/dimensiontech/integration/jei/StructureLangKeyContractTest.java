package com.suntide_20210418.dimensiontech.integration.jei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Translation keys only fail in game, and both ways they fail are silent: a key no lang file defines
 * renders as its own path, and a key whose text carries placeholders but whose call passes none
 * renders {@code %s} literally - which is how {@code %s-%s} reached the reactor tooltip. Neither is
 * visible to the compiler, so both are checked against the sources that produce them.
 */
class StructureLangKeyContractTest {
    private static final Path INTEGRATION =
            Path.of("src/main/java/com/suntide_20210418/dimensiontech/integration/jei");

    private static final Path JEI_TEXT = INTEGRATION.resolve("StructureReactorJeiText.java");

    private static final Path REACTOR_BLOCK =
            Path.of(
                    "src/main/java/com/suntide_20210418/dimensiontech/block/StructureReactorBlock.java");

    /** Files whose {@code String} constants name translation keys, so their calls can be audited. */
    private static final List<Path> KEYED_SOURCES = List.of(JEI_TEXT, REACTOR_BLOCK);

    private static final List<Path> PROVIDERS =
            List.of(
                    Path.of(
                            "src/main/java/com/suntide_20210418/dimensiontech/datagen/ModEnusLangProvider.java"),
                    Path.of(
                            "src/main/java/com/suntide_20210418/dimensiontech/datagen/ModZhcnLangProvider.java"));

    private static final Path LANG_DIR = Path.of("src/generated/resources/assets/dimension_tech/lang");

    private static final String CALL = "translatable\\(";
    private static final String SLOT = "%s";

    private static final Pattern KEY =
            Pattern.compile("\"(jei\\.dimension_tech\\.[A-Za-z0-9_.]+)\"");

    private static final Pattern CONSTANT =
            Pattern.compile("static final String (\\w+)\\s*=\\s*\"([^\"]+)\";");

    private static final Pattern LANG_ENTRY =
            Pattern.compile("^\\s*\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"", Pattern.MULTILINE);

    @Test
    void everyKeyTheIntegrationCanPrintIsTranslated() throws Exception {
        List<String> keys = integrationKeys();
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

    @Test
    void everyKeyIsGivenTheArgumentsItsTextAsksFor() throws Exception {
        Map<String, Integer> chinese = slots("zh_cn");
        Map<String, Integer> english = slots("en_us");
        List<String> problems = new ArrayList<>();

        for (Path source : KEYED_SOURCES) {
            String body = Files.readString(source);
            String name = source.getFileName().toString();
            for (Map.Entry<String, String> constant : constants(body).entrySet()) {
                String key = constant.getValue();
                Integer slots = chinese.get(key);
                if (slots == null) {
                    problems.add(name + ": " + key + " has no entry in the generated zh_cn lang");
                    continue;
                }
                Integer other = english.get(key);
                if (other != null && !other.equals(slots)) {
                    problems.add(key + ": en_us has " + other + " placeholders, zh_cn has " + slots);
                }
                for (int open : callsOf(body, constant.getKey())) {
                    int arguments = valueArguments(body, open);
                    if (arguments != slots) {
                        problems.add(
                                name
                                        + ": "
                                        + key
                                        + " has "
                                        + slots
                                        + " placeholders but is called with "
                                        + arguments
                                        + " arguments");
                    }
                }
            }
        }

        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    /** How many {@code %s} each key's text carries, read from the shipped lang file. */
    private static Map<String, Integer> slots(String language) throws Exception {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Matcher matcher = LANG_ENTRY.matcher(Files.readString(LANG_DIR.resolve(language + ".json")));
        while (matcher.find()) {
            counts.put(matcher.group(1), count(matcher.group(2)));
        }
        return counts;
    }

    private static int count(String value) {
        int total = 0;
        for (int at = value.indexOf(SLOT); at >= 0; at = value.indexOf(SLOT, at + SLOT.length())) {
            total++;
        }
        return total;
    }

    private static Map<String, String> constants(String body) {
        Map<String, String> found = new LinkedHashMap<>();
        Matcher matcher = CONSTANT.matcher(body);
        while (matcher.find()) {
            if (matcher.group(2).contains("dimension_tech.")) {
                found.put(matcher.group(1), matcher.group(2));
            }
        }
        return found;
    }

    /** The index of the opening parenthesis of every {@code translatable(NAME} call. */
    private static List<Integer> callsOf(String body, String name) {
        List<Integer> calls = new ArrayList<>();
        Matcher matcher =
                Pattern.compile(CALL + "\\s*" + Pattern.quote(name) + "\\s*(?=[,)])").matcher(body);
        while (matcher.find()) {
            calls.add(matcher.start() + CALL.length() - 1);
        }
        return calls;
    }

    /** Arguments after the key, counted as the commas at the call's own nesting level. */
    private static int valueArguments(String body, int open) {
        int depth = 1;
        int commas = 0;
        for (int at = open + 1; at < body.length(); at++) {
            char character = body.charAt(at);
            if (character == '(') {
                depth++;
            } else if (character == ')') {
                if (--depth == 0) return commas;
            } else if (character == ',' && depth == 1) {
                commas++;
            }
        }
        throw new AssertionError("unbalanced translatable call");
    }

    private static List<String> integrationKeys() throws Exception {
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
