package com.carry.app.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageResizer {

    public static final int FALLBACK_MAX_DIMENSION = 256;
    public static final int FALLBACK_MAX_BYTES = 100 * 1024; // 100 KB
    public static final int DEFAULT_JPEG_QUALITY = 80;

    public static class Dimensions {
        public final int width;
        public final int height;

        public Dimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Dimensions)) return false;
            Dimensions that = (Dimensions) o;
            return width == that.width && height == that.height;
        }

        @Override
        public int hashCode() {
            return 31 * width + height;
        }

        @Override
        public String toString() {
            return width + "x" + height;
        }
    }

    /**
     * Pure math function to compute scaled dimensions preserving aspect ratio,
     * capping the longest edge at maxDimension.
     */
    public static Dimensions calculateScaledDimensions(int origWidth, int origHeight, int maxDimension) {
        if (origWidth <= 0 || origHeight <= 0 || maxDimension <= 0) {
            return new Dimensions(Math.max(1, origWidth), Math.max(1, origHeight));
        }

        if (origWidth <= maxDimension && origHeight <= maxDimension) {
            return new Dimensions(origWidth, origHeight);
        }

        if (origWidth >= origHeight) {
            int scaledWidth = maxDimension;
            int scaledHeight = Math.max(1, Math.round((float) origHeight * maxDimension / (float) origWidth));
            return new Dimensions(scaledWidth, scaledHeight);
        } else {
            int scaledHeight = maxDimension;
            int scaledWidth = Math.max(1, Math.round((float) origWidth * maxDimension / (float) origHeight));
            return new Dimensions(scaledWidth, scaledHeight);
        }
    }

    /**
     * Calculates the inSampleSize power of 2 for memory-efficient bitmap decoding.
     */
    public static int calculateInSampleSize(int origWidth, int origHeight, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (origHeight > reqHeight || origWidth > reqWidth) {
            final int halfHeight = origHeight / 2;
            final int halfWidth = origWidth / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Downscales an image from an input stream to maxDimension (both edges <= maxDimension)
     * and compresses to JPEG format guaranteed to be under maxBytes.
     */
    public static byte[] processAndCompress(InputStream inputStream, int maxDimension, int maxBytes, int initialQuality) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) return null;

            Dimensions scaled = calculateScaledDimensions(bitmap.getWidth(), bitmap.getHeight(), maxDimension);
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, scaled.width, scaled.height, true);

            int quality = initialQuality;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, quality, baos);

            // If compressed output exceeds maxBytes, reduce quality in steps
            while (baos.size() > maxBytes && quality > 20) {
                quality -= 10;
                baos.reset();
                resized.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            }

            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
