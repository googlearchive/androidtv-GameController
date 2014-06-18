/*
 * Copyright 2014 Google Inc. All rights reserved.
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

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

/**
 * A few utility functions and classes used by this sample.
 */
public class Utils {
    /**
     * String used to identify log messages from this program.
     */
    private static final String LOG_TAG = "GameControllerSample";

    /**
     * The 2D vertex positions for a square centered around the origin.
     *
     * The vertices are ordered so that one triangle strip can be used to render
     * the square.
     *
     * Using scaling and rotation, these vertices can be used to render any rectangular shape.
     */
    public static final float[] SQUARE_SHAPE = {
             1.0f,  1.0f,
            -1.0f,  1.0f,
             1.0f, -1.0f,
            -1.0f, -1.0f
    };

    /**
     * Prints debugging messages to the console.
     *
     * Disabled for non-debug builds.
     *
     * @param message The message to print to the console.
     */
    public static void logDebug(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, message);
        }
    }

    /**
     * Prints an error message to the console.
     *
     * Disabled for non-debug builds.
     *
     * @param message The message to print to the console.
     */
    public static void logError(String message) {
        if (BuildConfig.DEBUG) {
            Log.e(LOG_TAG, message);
        }
    }

    /**
     * Returns a random floating point value >= lowerBound and < upperBound.
     */
    public static float randFloatInRange(float lowerBound, float upperBound) {
        return (float) (Math.random() * (upperBound - lowerBound) + lowerBound);
    }
    /**
     * Returns a random integer value >= lowerBound and < upperBound.
     */
    public static int randIntInRange(int lowerBound, int upperBound) {
        return (int) randFloatInRange(lowerBound, upperBound);
    }

    /**
     * Returns a randomly chosen point inside the given rectangle.
     *
     * @param rect a rectangle bounding the location for the point.
     * @return a new PointF object.
     */
    public static PointF randPointInRect(RectF rect) {
        float x = randFloatInRange(rect.left, rect.right);
        float y = randFloatInRange(rect.bottom, rect.top);

        return new PointF(x, y);
    }

    /**
     * Computes a randomly chosen direction vector.
     *
     * @return a new PointF object that is a normalized direction vector.
     */
    public static PointF randDirectionVector() {
        // Pick a random point in a square centered about the origin.
        PointF direction = randPointInRect(new RectF(-1.0f, 1.0f, 1.0f, -1.0f));

        // Turn the chosen point into a direction vector by normalizing it.
        normalizeDirectionVector(direction);

        return direction;
    }

    /**
     * Normalizes the given direction vector.
     *
     * Changes the length of the given vector so that it is "1".  If the given direction
     * already has a length of "0", the direction will be set to "(1, 0)".
     *
     * @param direction the direction to normalize.
     */
    public static void normalizeDirectionVector(PointF direction) {
        float length = direction.length();
        if (length == 0.0f) {
            direction.set(1.0f, 0.0f);
        } else {
            direction.x /= length;
            direction.y /= length;
        }
    }

    /**
     * Determines the squared length of the given 2-component vector.
     *
     * @param x the x component of the vector.
     * @param y the y component of the vector.
     * @return the squared length of the vector.
     */
    public static float vector2DLengthSquared(float x, float y) {
        return x * x + y * y;
    }

    /**
     * Determines the length of the given 2-component vector.
     *
     * @param x the x component of the vector.
     * @param y the y component of the vector.
     * @return the length of the vector.
     */
    public static float vector2DLength(float x, float y) {
        return (float) Math.sqrt(vector2DLengthSquared(x, y));
    }

    /**
     * Computes the distance between two 2-d points.
     *
     * @param x1 x position of the first point.
     * @param y1 y position of the first point.
     * @param x2 x position of the second point.
     * @param y2 y position of the second point.
     * @return the distance between the two points.
     */
    public static float distanceBetweenPoints(float x1, float y1, float x2, float y2) {
        float xSquared = (x2 - x1);
        xSquared *= xSquared;

        float ySquared = (y2 - y1);
        ySquared *= ySquared;

        return (float) Math.sqrt(xSquared + ySquared);
    }

    /**
     * Ensures that the given value falls within the given range.
     *
     * @param value The value to clamp.
     * @param min The lower bound of the range.
     * @param max The upper bound of the range.
     * @return The clamped value.
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    /**
     * Ensures that the given value falls within the given range.
     *
     * @param value The value to clamp.
     * @param min The lower bound of the range.
     * @param max The upper bound of the range.
     * @return The clamped value.
     */
    public static float clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Class for storing and manipulating RGBA colors.
     *
     * The color components are stored internally in a 32-bit int, with 8 bits for each
     * component.
     *
     * This class is similar to android.graphics.Color, but differs in 2 main ways:
     * 1) This class is a container for color data, so it can be instantiated and used as
     *      data type.  android.graphics.Color is just a set of static functions for packing
     *      color components into integers.
     * 2) android.graphics.Color only provides one component packing order, and that order
     *      does not match what OpenGl/OpenGl ES expects.  This class provides accessors
     *      to get the colors packed in the order expected by OpenGl/OpenGl ES.
     */
    public static class Color {
        // Some common colors for easy reference and readability.  Add more as needed.
        public static final Color RED = new Color(1.0f, 0.0f, 0.0f);
        public static final Color GREEN = new Color(0.0f, 1.0f, 0.0f);
        public static final Color BLUE = new Color(0.0f, 0.0f, 1.0f);
        public static final Color YELLOW = new Color(1.0f, 1.0f, 0.0f);
        public static final Color WHITE = new Color(1.0f, 1.0f, 1.0f);

        // Masks and bit locations for each color component.
        private static final int RED_MASK = 0xffffff00;
        private static final int RED_SHIFT = 0;
        private static final int GREEN_MASK = 0xffff00ff;
        private static final int GREEN_SHIFT = 8;
        private static final int BLUE_MASK = 0xff00ffff;
        private static final int BLUE_SHIFT = 16;
        private static final int ALPHA_MASK = 0x00ffffff;
        private static final int ALPHA_SHIFT = 24;

        /**
         * The packed color data.
         *
         * Alpha is packed into the high-order byte and red is the low-order byte.  This
         * matches the component packing order used by OpenGl/OpenGl ES.
         */
        private int mABGR;

        /**
         * Converts a floating point color value in the range [0..1] to an integer in the
         * range [0..255].
         *
         * @param normalizedColor A number in the range 0 to 1, inclusive.  No range-checking
         *                        is performed.
         * @return An int in the range 0 to 255, inclusive.
         */
        private static int normalizedColorToInt(float normalizedColor) {
            return (int) (255.0f * normalizedColor);
        }
        /**
         * Converts an integer color value in the range [0..255] to floating point number in the
         * range [0..1].
         *
         * @param intColor A number in the range 0 to 255, inclusive.  No range-checking
         *                        is performed.
         * @return A float in the range 0 to 1, inclusive.
         */
        private static float intColorToNormalized(int intColor) {
            return (float) intColor / 255.0f;
        }

        /**
         * Packs 4 floating point color components into a single 32-bit integer.
         *
         * The color components must be in the range 0 to 1, inclusive.
         */
        private static int packNormalizedRGBAToABGR(float red, float green, float blue,
                                                    float alpha) {
            return packABGR(
                    normalizedColorToInt(red),
                    normalizedColorToInt(green),
                    normalizedColorToInt(blue),
                    normalizedColorToInt(alpha));
        }

        /**
         * Packs 4 integer color components into a single 32-bit integer.
         *
         * The color components must be in the range 0 to 255, inclusive.
         */
        private static int packABGR(int red, int green, int blue, int alpha) {
            return (red << RED_SHIFT)
                    | (green << GREEN_SHIFT)
                    | (blue << BLUE_SHIFT)
                    | (alpha << ALPHA_SHIFT);
        }

        /**
         * Creates an uninitialized color object.
         */
        public Color() {}

        /**
         * Creates a Color object with the given color components.
         *
         * Each component must be in the range 0 to 1, inclusive.
         */
        public Color(float red, float green, float blue, float alpha) {
            set(red, green, blue, alpha);
        }
        /**
         * Creates a Color object with the given color components and an alpha of 1.0.
         *
         * Each component must be in the range 0 to 1, inclusive.
         */
        public Color(float red, float green, float blue) {
            mABGR = packNormalizedRGBAToABGR(red, green, blue, 1.0f);
        }

        /**
         * Creates a copy of the given color.
         */
        public Color(Utils.Color other) {
            set(other);
        }

        /**
         * Sets all of the color components.
         *
         * Each component must be in the range 0 to 1, inclusive.
         */
        public void set(float red, float green, float blue, float alpha) {
            mABGR = packNormalizedRGBAToABGR(red, green, blue, alpha);
        }

        /**
         * Changes our color to match the given color.
         */
        public void set(Color other) {
            this.mABGR = other.mABGR;
        }

        /**
         * Returns a packed integer representation of this color.
         *
         * @return The 32-bit packed color, with 8 bits per components.  Alpha is in the
         * high-order bits, then blue, then green, and then red in the low-order bits.
         */
        public int getPackedABGR() {
            return mABGR;
        }

        /**
         * Returns the red component of the color as a floating point number in the
         * range 0 to 1, inclusive.
         */
        public float red() {
            return intColorToNormalized((mABGR >> RED_SHIFT) & 0xff);
        }
        /**
         * Sets the red component of the color.  The given color component must be a
         * a floating point number in the range 0 to 1, inclusive.
         */
        public void setRed(float red) {
            mABGR = (mABGR & RED_MASK) | (normalizedColorToInt(red) << RED_SHIFT);
        }
        /**
         * Returns the green component of the color as a floating point number in the
         * range 0 to 1, inclusive.
         */
        public float green() {
            return intColorToNormalized((mABGR >> GREEN_SHIFT) & 0xff);
        }
        /**
         * Sets the green component of the color.  The given color component must be a
         * a floating point number in the range 0 to 1, inclusive.
         */
        public void setGreen(float green) {
            mABGR = (mABGR & GREEN_MASK) | (normalizedColorToInt(green) << GREEN_SHIFT);
        }
        /**
         * Returns the blue component of the color as a floating point number in the
         * range 0 to 1, inclusive.
         */
        public float blue() {
            return intColorToNormalized((mABGR >> BLUE_SHIFT) & 0xff);
        }
        /**
         * Sets the blue component of the color.  The given color component must be a
         * a floating point number in the range 0 to 1, inclusive.
         */
        public void setBlue(float blue) {
            mABGR = (mABGR & BLUE_MASK) | (normalizedColorToInt(blue) << BLUE_SHIFT);
        }
        /**
         * Returns the alpha component of the color as a floating point number in the
         * range 0 to 1, inclusive.
         */
        public float alpha() {
            return intColorToNormalized((mABGR >> ALPHA_SHIFT) & 0xff);
        }
        /**
         * Sets the alpha component of the color.  The given color component must be a
         * a floating point number in the range 0 to 1, inclusive.
         */
        public void setAlpha(float alpha) {
            mABGR = (mABGR & ALPHA_MASK) | (normalizedColorToInt(alpha) << ALPHA_SHIFT);
        }

        /**
         * Changes this color by multiplying each color component by the given factor.
         *
         * The alpha component of the color remains unchanged.
         *
         * @param factor A value in the range 0..1, inclusive, to indicate how much darkening
         *               to apply.  0 sets the color to black, and 1 leaves the color unchanged.
         *               Values outside of this range have undefined results.
         */
        public void darken(float factor) {
            set(red() * factor, green() * factor, blue() * factor, alpha());
        }

        /**
         * Sets this color to a color computed by interpolating between two other colors.
         *
         * The interpolated color is created by linearly interpolating each component
         * of the two given colors.
         *
         * @param colorA The first color to mix.
         * @param colorB The second color to mix.
         * @param factor A value between 0 and 1.  A value of "0" will set this color to
         *               colorA, and a value of "1" will set this color to colorB.
         */
        public void setToLerp(Color colorA, Color colorB, float factor) {
            set(colorA.red() * factor + colorB.red() * (1.0f - factor),
                    colorA.green() * factor + colorB.green() * (1.0f - factor),
                    colorA.blue() * factor + colorB.blue() * (1.0f - factor),
                    colorA.alpha() * factor + colorB.alpha() * (1.0f - factor));
        }
    }
}



