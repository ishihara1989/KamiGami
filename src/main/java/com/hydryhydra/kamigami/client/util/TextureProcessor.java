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
     * Clears the texture cache. Call this when resources are reloaded.
     */
    public static void clearCache() {
        processedTextureCache.clear();
        KamiGami.LOGGER.info("Texture processor cache cleared");
    }
}
