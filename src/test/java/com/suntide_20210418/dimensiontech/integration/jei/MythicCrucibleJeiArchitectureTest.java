package com.suntide_20210418.dimensiontech.integration.jei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The crucible model must stay usable without JEI, and the JEI layer must stay readable without a
 * running game. Both properties are what let
 * {@link MythicCrucibleJeiRecipesTest} run as a plain unit test.
 */
class MythicCrucibleJeiArchitectureTest {
    private static final Path MODEL =
            Path.of("src/main/java/com/suntide_20210418/dimensiontech/mythiccrucible");
    private static final Path INTEGRATION =
            Path.of("src/main/java/com/suntide_20210418/dimensiontech/integration/jei");

    @Test
    void theCrucibleModelNeverDependsOnJei() throws Exception {
        for (Path source : javaFiles(MODEL)) {
            assertFalse(
                    read(source).contains("mezz.jei"),
                    source.getFileName() + " must stay independent of JEI");
        }
    }

    @Test
    void theDisplayModelAndAdapterStayFreeOfJeiTypes() throws Exception {
        for (String name :
                List.of("MythicCrucibleJeiRecipe.java", "MythicCrucibleJeiRecipes.java")) {
            assertFalse(
                    read(INTEGRATION.resolve(name)).contains("mezz.jei"),
                    name + " must stay testable without the JEI runtime");
        }
    }

    @Test
    void theAdapterTakesRecipesAsPlainInputs() throws Exception {
        String adapter = read(INTEGRATION.resolve("MythicCrucibleJeiRecipes.java"));

        assertTrue(adapter.contains("Collection<MythicCrucibleRecipe<S>>"));
        assertFalse(adapter.contains("MythicCrucibleBlockEntity"));
        assertFalse(adapter.contains("new MythicCrucibleCycle"));
    }

    @Test
    void onlyThePluginReadsTheLiveRegistry() throws Exception {
        assertTrue(
                read(INTEGRATION.resolve("MythicCrucibleJeiPlugin.java"))
                        .contains("MythicCrucibleRecipes.all()"));
    }

    private static List<Path> javaFiles(Path directory) throws Exception {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path);
    }
}
