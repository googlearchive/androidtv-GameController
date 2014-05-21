/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.fpl.gamecontroller;

import android.util.Log;

/**
 * A few utility functions.
 */
public class Utils {
    /**
     * Returns a random float between lowerBound and upperBound.
     */
    public static float randInRange(float lowerBound, float upperBound) {
        return (float) (Math.random() * (upperBound - lowerBound) + lowerBound);
    }

    /**
     * Returns a random int between lowerBound and upperBound, inclusive.
     */
    public static int randIntInRange(int lowerBound, int upperBound) {
        return (int) (Math.random() * (upperBound - lowerBound + 1) + lowerBound);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    public static float clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static class Color {
        private static final int RED_MASK = 0xffffff00;
        private static final int RED_SHIFT = 0;
        private static final int GREEN_MASK = 0xffff00ff;
        private static final int GREEN_SHIFT = 8;
        private static final int BLUE_MASK = 0xff00ffff;
        private static final int BLUE_SHIFT = 16;
        private static final int ALPHA_MASK = 0x00ffffff;
        private static final int ALPHA_SHIFT = 24;

        private int mABGR;

        private static int normalizedColorToInt(float normalizedColor) {
            if (clamp(normalizedColor, 0.0f, 1.0f) != normalizedColor) {
                Log.i("GameControllerSample", "Invalid color component.");
            }
            return (int)(255.0f * normalizedColor);
        }
        private static float intColorToNormalized(int intColor) {
            if (clamp(intColor, 0, 255) != intColor) {
                Log.i("GameControllerSample", "Invalid color component.");
            }
            return (float)intColor / 255.0f;
        }

        private static int packNormalizedABGR(float red, float green, float blue, float alpha) {
            return (normalizedColorToInt(red) << RED_SHIFT) |
                    (normalizedColorToInt(green) << GREEN_SHIFT) |
                    (normalizedColorToInt(blue) << BLUE_SHIFT) |
                    (normalizedColorToInt(alpha) << ALPHA_SHIFT);
        }

        private static int packABGR(int red, int green, int blue, int alpha) {
            if (clamp(alpha, 0, 255) != alpha ||
                    clamp(blue, 0, 255) != blue ||
                    clamp(green, 0, 255) != green ||
                    clamp(red, 0, 255) != red) {
                Log.i("GameControllerSample", "Invalid color component(s).");
            }

            return android.graphics.Color.argb(alpha, blue, green, red);
        }
        public Color() {}

        public Color(float red, float green, float blue, float alpha) {
            set(red, green, blue, alpha);
        }

        public Color(float red, float green, float blue) {
            mABGR = packNormalizedABGR(red, green, blue, 1.0f);
        }
        public void set(float red, float green, float blue, float alpha) {
            mABGR = packNormalizedABGR(red, green, blue, alpha);
        }
        public void set(Color other) {
            mABGR = other.mABGR;
        }
        public int getPackedABGR() {
            return mABGR;
        }

        public float red() {
            return intColorToNormalized((mABGR >> RED_SHIFT) & 0xff);
        }
        public void setRed(float red) {
            mABGR = (mABGR & RED_MASK) | (normalizedColorToInt(red) << RED_SHIFT);
        }
        public float green() {
            return intColorToNormalized((mABGR >> GREEN_SHIFT) & 0xff);
        }
        public void setGreen(float green) {
            mABGR = (mABGR & GREEN_MASK) | (normalizedColorToInt(green) << GREEN_SHIFT);
        }
        public float blue() {
            return intColorToNormalized((mABGR >> BLUE_SHIFT) & 0xff);
        }
        public void setBlue(float blue) {
            mABGR = (mABGR & BLUE_MASK) | (normalizedColorToInt(blue) << BLUE_SHIFT);
        }
        public float alpha() {
            return intColorToNormalized((mABGR >> ALPHA_SHIFT) & 0xff);
        }
        public void setAlpha(float alpha) {
            mABGR = (mABGR & ALPHA_MASK) | (normalizedColorToInt(alpha) << ALPHA_SHIFT);
        }

        public void scale(float redScale, float greenScale, float blueScale, float alphaScale) {
            set(red() * redScale, green() * greenScale, blue() * blueScale, alpha() * alphaScale);
        }

        public void setToLerp(Color colorA, Color colorB, float x) {
            set(colorA.red() * x + colorB.red() * (1.0f - x),
                    colorA.green() * x + colorB.green() * (1.0f - x),
                    colorA.blue() * x + colorB.blue() * (1.0f - x),
                    colorA.alpha() * x + colorB.alpha() * (1.0f - x));
        }
    }
}



