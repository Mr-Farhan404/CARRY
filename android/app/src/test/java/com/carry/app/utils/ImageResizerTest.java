package com.carry.app.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class ImageResizerTest {

    @Test
    public void testSquareImageNoScaleNeeded() {
        ImageResizer.Dimensions dims = ImageResizer.calculateScaledDimensions(200, 200, 256);
        assertEquals(200, dims.width);
        assertEquals(200, dims.height);
    }

    @Test
    public void testLandscapeScaling() {
        // 1024x768 (4:3 aspect ratio) scaled to max 256 -> 256x192
        ImageResizer.Dimensions dims = ImageResizer.calculateScaledDimensions(1024, 768, 256);
        assertEquals(256, dims.width);
        assertEquals(192, dims.height);
    }

    @Test
    public void testPortraitScaling() {
        // 600x1200 (1:2 aspect ratio) scaled to max 256 -> 128x256
        ImageResizer.Dimensions dims = ImageResizer.calculateScaledDimensions(600, 1200, 256);
        assertEquals(128, dims.width);
        assertEquals(256, dims.height);
    }

    @Test
    public void testLargeImageScaling512() {
        // 4000x3000 scaled to max 512 -> 512x384
        ImageResizer.Dimensions dims = ImageResizer.calculateScaledDimensions(4000, 3000, 512);
        assertEquals(512, dims.width);
        assertEquals(384, dims.height);
    }

    @Test
    public void testExactBoundary() {
        ImageResizer.Dimensions dims = ImageResizer.calculateScaledDimensions(256, 256, 256);
        assertEquals(256, dims.width);
        assertEquals(256, dims.height);
    }

    @Test
    public void testExtremeAspectRatio() {
        // 1000x100 scaled to max 256 -> 256x26
        ImageResizer.Dimensions dims = ImageResizer.calculateScaledDimensions(1000, 100, 256);
        assertEquals(256, dims.width);
        assertEquals(26, dims.height);
    }

    @Test
    public void testZeroOrNegativeDimensions() {
        ImageResizer.Dimensions dims = ImageResizer.calculateScaledDimensions(0, -10, 256);
        assertTrue(dims.width >= 1);
        assertTrue(dims.height >= 1);
    }

    @Test
    public void testInSampleSizeCalculation() {
        int sampleSize = ImageResizer.calculateInSampleSize(2048, 1536, 256, 192);
        assertTrue("inSampleSize should be a power of 2 >= 4", sampleSize >= 4);
    }
}
