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

import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Handles collecting and rendering of triangles.
 */
public class ShapeBuffer {
    // Number of coordinates per vertex in this array.
    private static final int COORDS_PER_VERTEX = 3;
    private static final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4; // 4 bytes per vertex
    private static final int COLOR_CHANNELS_PER_VERTEX = 4;
    private static final int COLOR_STRIDE = COLOR_CHANNELS_PER_VERTEX;
    private static final int MAX_BUFFER_SIZE = 50000;

    private final int mProgram;

    private int mCurrentIndex;
    private float[] mVertexData;
    private int[] mColorData;
    private int[] mIndexData;
    private float mR, mG, mB, mA;
    private IntBuffer mColorBuffer;
    private FloatBuffer mVertexBuffer;
    private IntBuffer mIndexBuffer;

    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;
    private int mProjectionMatrixHandle;

    public ShapeBuffer() {
        mVertexData = new float[3 * MAX_BUFFER_SIZE];
        mColorData = new int[MAX_BUFFER_SIZE];
        mIndexData = new int[MAX_BUFFER_SIZE];
        for (int i = 0; i < MAX_BUFFER_SIZE; i++) {
            mIndexData[i] = i;
        }
        clear();

        ByteBuffer bb;
        // size = buffer size x1 value, x4 bytes per int
        bb = ByteBuffer.allocateDirect(MAX_BUFFER_SIZE * 1 * 4);
        bb.order(ByteOrder.nativeOrder());
        mIndexBuffer = bb.asIntBuffer();

        // size = buffer size x4 values per color, x4 bytes per color.
        bb = ByteBuffer.allocateDirect(MAX_BUFFER_SIZE * 4);
        bb.order(ByteOrder.nativeOrder());
        mColorBuffer = bb.asIntBuffer();

        // size = buffer size x3 values per coord, x4 bytes per float
        bb = ByteBuffer.allocateDirect(MAX_BUFFER_SIZE * 3 * 4);
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER,
                getRawAsset(R.raw.untextured_vs));
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER,
                getRawAsset(R.raw.untextured_fs));

        // Create empty OpenGL Program.
        mProgram = GLES20.glCreateProgram();
        // Add the vertex shader to program.
        GLES20.glAttachShader(mProgram, vertexShader);
        // Add the fragment shader to program.
        GLES20.glAttachShader(mProgram, fragmentShader);
        // Create OpenGL program executables.
        GLES20.glLinkProgram(mProgram);
    }

    public static String fromStream(InputStream in) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                out.append(line);
                out.append(newLine);
            }
        } catch (IOException e) {
            Log.e("GameControllerSample", e.toString());
        }
        return out.toString();
    }

    public static int loadShader(int type, String shaderCode) {
        // Create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER).
        int shader = GLES20.glCreateShader(type);

        // Add the source code to the shader and compile it.
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     * <p/>
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, &quot;vColor&quot;);
     * MyGLRenderer.checkGlError(&quot;glGetUniformLocation&quot;);
     * </pre>
     * <p/>
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("GameControllerSample", glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    private String getRawAsset(int id) {
        Resources res = GameState.getInstance().getResources();
        return fromStream(res.openRawResource(id));
    }

    public void clear() {
        mCurrentIndex = 0;
    }

    public void addShape(float x, float y, Utils.Color color, float[] data, float scaleX,
                         float scaleY, float headingX, float headingY) {
        // Normalize h1/h2:
        float magnitude = (float) Math.sqrt(headingX * headingX + headingY * headingY);
        if (magnitude == 0) {
            headingX = 0;
            headingY = 1;
        } else {
            headingX /= magnitude;
            headingY /= magnitude;
        }

        for (int i = 0; i < data.length - 1; i += 2) {
            float cx = (scaleX * data[i + 0] * headingX - scaleY * data[i + 1]
                    * headingY);
            float cy = (scaleX * data[i + 0] * headingY + scaleY * data[i + 1]
                    * headingX);

            mVertexData[3 * mCurrentIndex + 0] = cx + x;
            mVertexData[3 * mCurrentIndex + 1] = cy + y;
            mVertexData[3 * mCurrentIndex + 2] = 0;

            mColorData[mCurrentIndex] = color.getPackedABGR();
            mCurrentIndex += 1;

            // If we're on the first or last point, repeat it for stiching.
            if (i == 0) {
                stitchingHelper();
            }
        }
        stitchingHelper();
    }

    // Duplicates the last point, to aid in stitching.
    private void stitchingHelper() {
        mVertexData[3 * mCurrentIndex + 0] = mVertexData[3 * mCurrentIndex + 0 - 3];
        mVertexData[3 * mCurrentIndex + 1] = mVertexData[3 * mCurrentIndex + 1 - 3];
        mVertexData[3 * mCurrentIndex + 2] = mVertexData[3 * mCurrentIndex + 2 - 3];

        mColorData[mCurrentIndex] = mColorData[mCurrentIndex - 1];

        mCurrentIndex += 1;
    }

    public void draw(float[] projMatrix, float[] mvpMatrix) {

        if (mCurrentIndex == 0) {
            // Nothing to draw.
            return;
        }

        // Load up our data:
        checkGlError("draw init");

        mIndexBuffer.clear();
        mIndexBuffer.put(mIndexData);
        mIndexBuffer.position(0);
        checkGlError("index data");

        mVertexBuffer.clear();
        mVertexBuffer.put(mVertexData);
        mVertexBuffer.position(0);
        checkGlError("vert data");

        mColorBuffer.clear();
        mColorBuffer.put(mColorData);
        mColorBuffer.position(0);
        checkGlError("color data");

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        checkGlError("draw start");

        // Add program to OpenGL environment.
        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");

        // Get handle to vertex shader's vPosition member.
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        // Enable a handle to the triangle vertices.
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        // Prepare the triangle coordinate data.
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, VERTEX_STRIDE, mVertexBuffer);
        checkGlError("glVertexAttribPointer - vert");

        // Get handle to vertex shader's vColor member.
        mColorHandle = GLES20.glGetAttribLocation(mProgram, "vColor");
        // Enable a handle to the triangle vertices.
        GLES20.glEnableVertexAttribArray(mColorHandle);
        // Prepare the color data.
        GLES20.glVertexAttribPointer(mColorHandle, COLOR_CHANNELS_PER_VERTEX,
                GLES20.GL_UNSIGNED_BYTE, true, COLOR_STRIDE, mColorBuffer);
        checkGlError("glVertexAttribPointer - color");

        // Get handle to shape's transformation matrix.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation");

        // Get handle to the projection matrix.
        mProjectionMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uProjMatrix");
        checkGlError("glGetUniformLocation2");

        // Apply the projection and view transformation.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        checkGlError("glUniformMatrix4fv");

        GLES20.glUniformMatrix4fv(mProjectionMatrixHandle, 1, false, projMatrix, 0);
        checkGlError("glUniformMatrix4fv 2");

        // Draw the buffers!
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, mCurrentIndex,
                GLES20.GL_UNSIGNED_INT, mIndexBuffer);
        checkGlError("draw call");

        // Disable vertex arrays.
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        checkGlError("position attrib arrays disabled");
        GLES20.glDisableVertexAttribArray(mColorHandle);
        checkGlError("vertex attrib arrays disabled");
    }
}
