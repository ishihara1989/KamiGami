package com.hydryhydra.kamigami.client.util;

import com.hydryhydra.kamigami.KamiGami;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Utility class for processing vanilla textures and creating modified versions.
 * Supports operations like desaturation and brightness adjustment.
 */
public class TextureProcessor {
    private static final Map<String, ResourceLocation> processedTextureCache = new HashMap<>();

    /**
     * Processes a vanilla texture by applying desaturation and brightness
     * adjustment.
     *
     * @param vanillaTexture
     *            The ResourceLocation of the vanilla texture to process
     * @param saturation
     *            Saturation multiplier (0.0 = grayscale, 1.0 = original)
     * @param brightness
     *            Brightness adjustment (-255 to 255, negative = darker)
     * @param cacheKey
     *            Unique key for caching the processed texture
     * @return ResourceLocation of the processed texture
     */
    public static ResourceLocation processTexture(ResourceLocation vanillaTexture, float saturation, int brightness,
            String cacheKey) {
        // Check cache first
        if (processedTextureCache.containsKey(cacheKey)) {
            return processedTextureCache.get(cacheKey);
        }

        try {
            // Load vanilla texture
            Minecraft minecraft = Minecraft.getInstance();
            ResourceManager resourceManager = minecraft.getResourceManager();
            Resource resource = resourceManager.getResource(vanillaTexture)
                    .orElseThrow(() -> new IOException("Texture not found: " + vanillaTexture));

            try (InputStream inputStream = resource.open()) {
                NativeImage originalImage = NativeImage.read(inputStream);

                // Create new image with same dimensions
                NativeImage processedImage = new NativeImage(originalImage.format(), originalImage.getWidth(),
                        originalImage.getHeight(), false);

                // Process each pixel
                for (int y = 0; y < originalImage.getHeight(); y++) {
                    for (int x = 0; x < originalImage.getWidth(); x++) {
                        int pixel = originalImage.getPixel(x, y);
                        int processedPixel = processPixel(pixel, saturation, brightness);
                        processedImage.setPixel(x, y, processedPixel);
                    }
                }

                // Register as dynamic texture
                ResourceLocation processedLocation = ResourceLocation.fromNamespaceAndPath(KamiGami.MODID,
                        "dynamic/" + cacheKey);
                DynamicTexture dynamicTexture = new DynamicTexture(() -> "Processed texture: " + cacheKey,
                        processedImage);
                minecraft.getTextureManager().register(processedLocation, dynamicTexture);

                // Cache and return
                processedTextureCache.put(cacheKey, processedLocation);
                KamiGami.LOGGER.info("Processed texture: {} -> {} (saturation: {}, brightness: {})", vanillaTexture,
                        processedLocation, saturation, brightness);

                originalImage.close();
                return processedLocation;
            }
        } catch (IOException e) {
            KamiGami.LOGGER.error("Failed to process texture: {}", vanillaTexture, e);
            // Fallback to original texture
            return vanillaTexture;
        }
    }

    /**
     * Processes a single pixel by adjusting saturation and brightness. NativeImage
     * uses ABGR format in memory.
     *
     * @param pixel
     *            Original pixel in ABGR format
     * @param saturation
     *            Saturation multiplier (0.0 = grayscale, 1.0 = original)
     * @param brightness
     *            Brightness adjustment (-255 to 255)
     * @return Processed pixel in ABGR format
     */
    private static int processPixel(int pixel, float saturation, int brightness) {
        // Extract ABGR components
        int a = (pixel >> 24) & 0xFF;
        int b = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int r = pixel & 0xFF;

        // Apply desaturation (convert to grayscale then blend)
        // Using luminance formula: 0.299R + 0.587G + 0.114B
        int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
        r = (int) (gray + saturation * (r - gray));
        g = (int) (gray + saturation * (g - gray));
        b = (int) (gray + saturation * (b - gray));

        // Apply brightness adjustment
        r = clamp(r + brightness);
        g = clamp(g + brightness);
        b = clamp(b + brightness);

        // Reconstruct ABGR pixel
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    /**
     * Clamps a value to the valid color range [0, 255].
     */
    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /**
     * Creates a texture atlas for Tatari Tree entity by combining vanilla textures.
     * The atlas is 256x256 pixels and contains textures for trunk, head, branches,
     * and leaves.
     *
     * @param cacheKey
     *            Unique key for caching the processed texture
     * @return ResourceLocation of the processed texture atlas
     */
    public static ResourceLocation createTatariTreeAtlas(String cacheKey) {
        // Check cache first
        if (processedTextureCache.containsKey(cacheKey)) {
            return processedTextureCache.get(cacheKey);
        }

        try {
            Minecraft minecraft = Minecraft.getInstance();
            ResourceManager resourceManager = minecraft.getResourceManager();

            // Load vanilla textures and apply color inversion + brightness adjustment
            NativeImage logSide = processTextureImage(
                    loadTexture(resourceManager, ResourceLocation.withDefaultNamespace("textures/block/oak_log.png")),
                    true, -50); // Invert hue, -50 brightness
            NativeImage logTop = processTextureImage(loadTexture(resourceManager,
                    ResourceLocation.withDefaultNamespace("textures/block/oak_log_top.png")), true, -50);
            NativeImage pumpkinFace = processTextureImage(loadTexture(resourceManager,
                    ResourceLocation.withDefaultNamespace("textures/block/carved_pumpkin.png")), true, -50);
            NativeImage pumpkinSide = processTextureImage(loadTexture(resourceManager,
                    ResourceLocation.withDefaultNamespace("textures/block/pumpkin_side.png")), true, -50);
            NativeImage pumpkinTop = processTextureImage(loadTexture(resourceManager,
                    ResourceLocation.withDefaultNamespace("textures/block/pumpkin_top.png")), true, -50);
            NativeImage leaves = processTextureImage(loadTexture(resourceManager,
                    ResourceLocation.withDefaultNamespace("textures/block/oak_leaves.png")), true, -50);

            // Create 256x256 atlas
            NativeImage atlas = new NativeImage(logSide.format(), 256, 256, false);

            // Fill with transparent pixels
            for (int y = 0; y < 256; y++) {
                for (int x = 0; x < 256; x++) {
                    atlas.setPixel(x, y, 0); // Transparent
                }
            }

            // 1. Trunk (texOffs(0, 0)) - 16x96x16 box
            // Using corrected UV mapping based on actual model requirements:
            // (0,0)-(0,5) = (0, 0)-(0, 80) = log side (front)
            // (1,0) = (16, 0) = log TOP
            // (1,1)-(1,5) = (16, 16)-(16, 80) = log side
            // (2,0) = (32, 0) = log TOP
            // (2,1)-(2,5) = (32, 16)-(32, 80) = log side
            // (3,0)-(3,5) = (48, 0)-(48, 80) = log side (left)
            // (0,6)-(3,6) = (0, 96)-(48, 96) = log side

            // Front face column (0, 0) - tiles 0-5 vertically
            for (int i = 0; i < 6; i++) {
                copyResized(logSide, atlas, 0, i * 16, 16, 16);
            }
            // Column x=16: top at (1,0), then log side at (1,1)-(1,5)
            copyResized(logTop, atlas, 16, 0, 16, 16); // (1,0)
            for (int i = 1; i < 6; i++) { // (1,1)-(1,5)
                copyResized(logSide, atlas, 16, i * 16, 16, 16);
            }
            // Column x=32: top at (2,0), then log side at (2,1)-(2,5)
            copyResized(logTop, atlas, 32, 0, 16, 16); // (2,0)
            for (int i = 1; i < 6; i++) { // (2,1)-(2,5)
                copyResized(logSide, atlas, 32, i * 16, 16, 16);
            }
            // Left face column (48, 0) - tiles 0-5 vertically
            for (int i = 0; i < 6; i++) {
                copyResized(logSide, atlas, 48, i * 16, 16, 16);
            }
            // Row at y=96 (0,6)-(3,6) should all be log side
            copyResized(logSide, atlas, 0, 96, 16, 16); // (0,6)
            copyResized(logSide, atlas, 16, 96, 16, 16); // (1,6)
            copyResized(logSide, atlas, 32, 96, 16, 16); // (2,6)
            copyResized(logSide, atlas, 48, 96, 16, 16); // (3,6)

            // 2. Head (Jack-o-Lantern) (texOffs(0, 112)) - 16x16x16 box
            // Using corrected UV mapping based on actual model requirements:
            // (0,7) = (0, 112) = pumpkin side
            // (1,7) = (16, 112) = pumpkin TOP
            // (2,7) = (32, 112) = pumpkin TOP
            // (3,7) = (48, 112) = pumpkin side
            // (0,8) = (0, 128) = pumpkin top
            // (1,8) = (16, 128) = face
            // (2,8) = (32, 128) = pumpkin side
            copyResized(pumpkinSide, atlas, 0, 112, 16, 16); // (0,7)
            copyResized(pumpkinTop, atlas, 16, 112, 16, 16); // (1,7)
            copyResized(pumpkinTop, atlas, 32, 112, 16, 16); // (2,7)
            copyResized(pumpkinSide, atlas, 48, 112, 16, 16); // (3,7)
            copyResized(pumpkinTop, atlas, 0, 128, 16, 16); // (0,8)
            copyResized(pumpkinFace, atlas, 16, 128, 16, 16); // (1,8) - face
            copyResized(pumpkinSide, atlas, 32, 128, 16, 16); // (2,8)

            // 3. Branches (texOffs(64, 0)) - 32x8x8 box
            // Using corrected UV mapping:
            // (7,0) and (8,0) = (112, 0) and (128, 0) should be branch
            // Front face (64, 0) - 32x8 spans (4,0) and (5,0)
            copyResized(logSide, atlas, 64, 0, 32, 8);
            // Right face (96, 0) - 8x8 at (6,0)
            copyResized(logSide, atlas, 96, 0, 8, 8);
            // Additional branch texture at (112, 0) - 16x8 at (7,0)
            copyResized(logSide, atlas, 112, 0, 16, 8);
            // Back face (128, 0) - 32x8 spans (8,0) and (9,0) ✓
            copyResized(logSide, atlas, 128, 0, 32, 8);
            // Left face (160, 0) - 8x8 at (10,0)
            copyResized(logSide, atlas, 160, 0, 8, 8);
            // Top face (64, 8) - 32x8
            copyResized(logTop, atlas, 64, 8, 32, 8);
            // Bottom face (96, 8) - 32x8
            copyResized(logTop, atlas, 96, 8, 32, 8);

            // 4. Leaves (texOffs(64, 16)) - 16x16x16 box
            // Using corrected UV mapping:
            // (6,2) and (7,2) = (96, 32) and (112, 32) should be leaves
            copyResized(leaves, atlas, 64, 16, 16, 16); // (4,1)
            copyResized(leaves, atlas, 80, 16, 16, 16); // (5,1)
            copyResized(leaves, atlas, 96, 16, 16, 16); // (6,1)
            copyResized(leaves, atlas, 112, 16, 16, 16); // (7,1)
            copyResized(leaves, atlas, 64, 32, 16, 16); // (4,2)
            copyResized(leaves, atlas, 80, 32, 16, 16); // (5,2)
            copyResized(leaves, atlas, 96, 32, 16, 16); // (6,2) - leaves ✓
            copyResized(leaves, atlas, 112, 32, 16, 16); // (7,2) - leaves ✓

            // DEBUG: Uncomment to draw coordinate grid for debugging
            // drawDebugGrid(atlas);

            // Register as dynamic texture
            ResourceLocation atlasLocation = ResourceLocation.fromNamespaceAndPath(KamiGami.MODID,
                    "dynamic/" + cacheKey);
            DynamicTexture dynamicTexture = new DynamicTexture(() -> "Tatari Tree atlas: " + cacheKey, atlas);
            minecraft.getTextureManager().register(atlasLocation, dynamicTexture);

            // Cache and return
            processedTextureCache.put(cacheKey, atlasLocation);
            KamiGami.LOGGER.info("Created Tatari Tree texture atlas: {}", atlasLocation);

            // Close source images
            logSide.close();
            logTop.close();
            pumpkinFace.close();
            pumpkinSide.close();
            pumpkinTop.close();
            leaves.close();

            return atlasLocation;
        } catch (IOException e) {
            KamiGami.LOGGER.error("Failed to create Tatari Tree texture atlas", e);
            // Fallback to original texture if it exists
            return ResourceLocation.fromNamespaceAndPath(KamiGami.MODID, "textures/entity/tatari_tree/tatari_tree.png");
        }
    }

    /**
     * Loads a texture from the resource manager.
     */
    private static NativeImage loadTexture(ResourceManager resourceManager, ResourceLocation location)
            throws IOException {
        Resource resource = resourceManager.getResource(location)
                .orElseThrow(() -> new IOException("Texture not found: " + location));
        try (InputStream inputStream = resource.open()) {
            return NativeImage.read(inputStream);
        }
    }

    /**
     * Processes a NativeImage by inverting hue and adjusting brightness.
     *
     * @param source
     *            Source image to process
     * @param invertHue
     *            Whether to invert hue (opposite color on color wheel)
     * @param brightness
     *            Brightness adjustment (-255 to 255)
     * @return New processed image (original is not modified)
     */
    private static NativeImage processTextureImage(NativeImage source, boolean invertHue, int brightness) {
        NativeImage processed = new NativeImage(source.format(), source.getWidth(), source.getHeight(), false);

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int pixel = source.getPixel(x, y);

                // Extract ABGR components
                int a = (pixel >> 24) & 0xFF;
                int b = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int r = pixel & 0xFF;

                if (invertHue) {
                    // Invert hue by inverting RGB values
                    r = 255 - r;
                    g = 255 - g;
                    b = 255 - b;
                }

                // Apply brightness adjustment
                r = clamp(r + brightness);
                g = clamp(g + brightness);
                b = clamp(b + brightness);

                // Reconstruct ABGR pixel
                int processedPixel = (a << 24) | (b << 16) | (g << 8) | r;
                processed.setPixel(x, y, processedPixel);
            }
        }

        return processed;
    }

    /**
     * Copies and resizes a source image to a destination image at a specific
     * position. Uses nearest-neighbor filtering to maintain pixel art style.
     *
     * @param source
     *            Source image
     * @param dest
     *            Destination image
     * @param destX
     *            X position in destination
     * @param destY
     *            Y position in destination
     * @param width
     *            Target width
     * @param height
     *            Target height
     */
    private static void copyResized(NativeImage source, NativeImage dest, int destX, int destY, int width, int height) {
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Nearest-neighbor sampling
                int srcX = (x * srcWidth) / width;
                int srcY = (y * srcHeight) / height;

                // Clamp to valid range
                srcX = Math.min(srcX, srcWidth - 1);
                srcY = Math.min(srcY, srcHeight - 1);

                int pixel = source.getPixel(srcX, srcY);
                dest.setPixel(destX + x, destY + y, pixel);
            }
        }
    }

    /**
     * Draws debug grid with hex coordinates on atlas texture. Each 16x16 tile shows
     * its X,Y coordinates in hexadecimal (0-F).
     *
     * @param atlas
     *            The atlas image to draw on
     */
    @SuppressWarnings("unused")
    private static void drawDebugGrid(NativeImage atlas) {
        // White color for text (ABGR format)
        int white = 0xFFFFFFFF;

        // Draw coordinate labels for each 16x16 tile
        for (int tileY = 0; tileY < 16; tileY++) {
            for (int tileX = 0; tileX < 16; tileX++) {
                int pixelX = tileX * 16;
                int pixelY = tileY * 16;

                // Draw X coordinate (hex) at top-left of tile
                drawHexDigit(atlas, pixelX + 1, pixelY + 1, tileX, white);
                // Draw Y coordinate (hex) below X coordinate
                drawHexDigit(atlas, pixelX + 1, pixelY + 6, tileY, white);
            }
        }

        KamiGami.LOGGER.info("Drew debug coordinate grid on texture atlas");
    }

    /**
     * Draws a single hexadecimal digit (0-F) at specified position.
     *
     * @param image
     *            Image to draw on
     * @param x
     *            X position (top-left corner)
     * @param y
     *            Y position (top-left corner)
     * @param value
     *            Value to draw (0-15)
     * @param color
     *            Color in ABGR format
     */
    private static void drawHexDigit(NativeImage image, int x, int y, int value, int color) {
        // Simple 3x5 bitmap font for hex digits 0-F
        final int[][] FONT = {
                // 0
                {0b111, 0b101, 0b101, 0b101, 0b111},
                // 1
                {0b010, 0b110, 0b010, 0b010, 0b111},
                // 2
                {0b111, 0b001, 0b111, 0b100, 0b111},
                // 3
                {0b111, 0b001, 0b111, 0b001, 0b111},
                // 4
                {0b101, 0b101, 0b111, 0b001, 0b001},
                // 5
                {0b111, 0b100, 0b111, 0b001, 0b111},
                // 6
                {0b111, 0b100, 0b111, 0b101, 0b111},
                // 7
                {0b111, 0b001, 0b001, 0b001, 0b001},
                // 8
                {0b111, 0b101, 0b111, 0b101, 0b111},
                // 9
                {0b111, 0b101, 0b111, 0b001, 0b111},
                // A
                {0b111, 0b101, 0b111, 0b101, 0b101},
                // B
                {0b110, 0b101, 0b110, 0b101, 0b110},
                // C
                {0b111, 0b100, 0b100, 0b100, 0b111},
                // D
                {0b110, 0b101, 0b101, 0b101, 0b110},
                // E
                {0b111, 0b100, 0b110, 0b100, 0b111},
                // F
                {0b111, 0b100, 0b110, 0b100, 0b100}};

        if (value < 0 || value >= FONT.length)
            return;

        int[] glyph = FONT[value];
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 3; col++) {
                if ((glyph[row] & (1 << (2 - col))) != 0) {
                    int px = x + col;
                    int py = y + row;
                    if (px >= 0 && px < image.getWidth() && py >= 0 && py < image.getHeight()) {
                        image.setPixel(px, py, color);
                    }
                }
            }
        }
    }

    /**
     * Clears the texture cache. Call this when resources are reloaded.
     */
    public static void clearCache() {
        processedTextureCache.clear();
        KamiGami.LOGGER.info("Texture processor cache cleared");
    }
}
