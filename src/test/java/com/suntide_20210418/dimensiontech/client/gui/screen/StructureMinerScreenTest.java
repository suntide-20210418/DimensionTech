package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class StructureMinerScreenTest {
    private static final byte[] PNG_SIGNATURE = {
        (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    /** Bytes needed to carry the signature plus the IHDR width and height. */
    private static final int IHDR_HEADER_BYTES = 24;

    @Test
    void energyFillUsesLongArithmeticForLargeCapacities() {
        assertEquals(82, StructureMinerScreen.fillPixels(1_000_000, 2_000_000, 164));
        assertEquals(164, StructureMinerScreen.fillPixels(Integer.MAX_VALUE, 2_000_000, 164));
        assertEquals(0, StructureMinerScreen.fillPixels(0, 2_000_000, 164));
    }

    /**
     * The blit size constants must equal the texture's own size.
     *
     * <p>{@code blit} normalises UVs by whatever size it is handed, so a stale pair does not fail —
     * it draws. That is how a width left at 258 while the art was re-exported at 256 made the energy
     * strip sample the panel for its last column and the fluid overlay sample the energy strip. The
     * constants are hand-written and the art is redrawn, so read the IHDR back and fail loudly.
     */
    @Test
    void textureConstantsMatchTheTexturesOwnSize() throws IOException {
        Path texture = minerTexture();
        byte[] header;
        try (InputStream in = Files.newInputStream(texture)) {
            header = in.readNBytes(IHDR_HEADER_BYTES);
        }
        assertEquals(IHDR_HEADER_BYTES, header.length, "truncated PNG header: " + texture);
        assertArrayEquals(PNG_SIGNATURE, Arrays.copyOf(header, 8), "not a PNG: " + texture);
        assertEquals(
                readInt(header, 16),
                StructureMinerScreen.TEXTURE_WIDTH,
                "TEXTURE_WIDTH no longer matches " + texture.getFileName());
        assertEquals(
                readInt(header, 20),
                StructureMinerScreen.TEXTURE_HEIGHT,
                "TEXTURE_HEIGHT no longer matches " + texture.getFileName());
    }

    /**
     * Locates the miner's GUI sheet from the test's working directory.
     *
     * <p>Walks up instead of resolving one relative path because the working directory differs between
     * Gradle (the project directory) and an IDE launcher (the module directory).
     */
    private static Path minerTexture() {
        Path relative =
                Path.of(
                        "src",
                        "main",
                        "resources",
                        "assets",
                        "dimension_tech",
                        "guis",
                        "void_structre_miner.png");
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve(relative);
            if (Files.isRegularFile(candidate)) return candidate;
        }
        throw new AssertionError(
                "miner GUI texture not found from " + Path.of("").toAbsolutePath());
    }

    /** Big-endian 32-bit read; PNG integers are network order. */
    private static int readInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }
}
