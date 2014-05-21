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

import android.content.Context;
import android.hardware.input.InputManager;
import android.hardware.input.InputManager.InputDeviceListener;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.Log;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;

import com.google.fpl.gamecontroller.particles.Background;
import com.google.fpl.gamecontroller.particles.BaseParticle;
import com.google.fpl.gamecontroller.particles.ParticleGlitter;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * A singleton class that manages the OpenGL context, camera, players, etc.
 */
public class GameState extends GLSurfaceView implements Renderer, InputDeviceListener {

    public static final int WORLD_WIDTH = 340;
    public static final int WORLD_HEIGHT = 180;

    protected static final float  MS_PER_FRAME = 1000 / 60;
    protected static final int MAX_PLAYERS = 4;
    protected static final int MAX_POWERUPS = 2;
    protected static final Utils.Color PLAYER_COLORS[] = new Utils.Color[] {
            new Utils.Color(1.0f, 0.0f, 0.0f),
            new Utils.Color(0.0f, 1.0f, 0.0f),
            new Utils.Color(1.0f, 1.0f, 0.0f),
            new Utils.Color(0.0f, 0.0f, 1.0f),
            new Utils.Color(1.0f, 1.0f, 1.0f)
    };

    protected final float[] mMVPMatrix = new float[16];
    protected final float[] mProjMatrix = new float[16];
    protected final ViewportLocation[][] mCameraLocations = {
            // Zero players, still one camera...
            {
                    new ViewportLocation(0.0f, 0.0f, 1.0f, 1.0f),
            },
            {
                    new ViewportLocation(0.0f, 0.0f, 1.0f, 1.0f),
            },
            {
                    new ViewportLocation(0.0f, 0.0f, 0.5f, 1.0f),
                    new ViewportLocation(0.5f, 0.0f, 0.5f, 1.0f),
            },
            {
                    new ViewportLocation(0.0f, 0.0f, 0.5f, 0.5f),
                    new ViewportLocation(0.0f, 0.5f, 0.5f, 0.5f),
                    new ViewportLocation(0.5f, 0.0f, 0.5f, 1.0f),
            },
            {
                    new ViewportLocation(0.0f, 0.0f, 0.5f, 0.5f),
                    new ViewportLocation(0.5f, 0.0f, 0.5f, 0.5f),
                    new ViewportLocation(0.0f, 0.5f, 0.5f, 0.5f),
                    new ViewportLocation(0.5f, 0.5f, 0.5f, 0.5f)
            }
    };

    private static GameState sGameStateInstance = null;
    protected float mWindowWidth, mWindowHeight;
    protected Spaceship[] mPlayerList;
    protected Background mBackground;
    protected ParticleGlitter mShots;

    protected ParticleGlitter mExplosions;
    protected WallSegment[] mWallList;
    protected float mBackgroundResetCounter = 0;

    /**
     * The Activity Context
     */
    private Context mContext;
    private ShapeBuffer mShapeBuffer = null;
    private PowerUp[] mPowerupList;
    private GamepadController[] mGamepadControllerList;
    private long mLastUpdateTime;

    /**
     * Set this class as renderer for this GLSurfaceView.
     * Request Focus and set if focusable in touch mode to
     * receive the Input from Screen
     *
     * @param context - The Activity Context
     */
    public GameState(Context context) {
        super(context);
        sGameStateInstance = this;
        // Request GL ES 2.0 context.
        setEGLContextClientVersion(2);
        // Set this as Renderer.
        this.setRenderer(this);
        // Request focus.
        this.requestFocus();
        this.setFocusableInTouchMode(true);
        this.mContext = context;

        mPlayerList = new Spaceship[MAX_PLAYERS];
        for (int i = 0; i < MAX_PLAYERS; i++) {
            mPlayerList[i] = new Spaceship(this, PLAYER_COLORS[i]);
        }

        mBackground = new Background();
        mExplosions = new ParticleGlitter();

        // The true here means we want collision tracking data.
        mShots = new ParticleGlitter(true);

        mLastUpdateTime = System.currentTimeMillis();

        InputManager inputManager = (InputManager) context.getSystemService("input");
        inputManager.registerInputDeviceListener(this, null);

        mWallList = new WallSegment[]{
                new WallSegment(40, 80, 20, 60),
                new WallSegment(-40, -80, 20, 60),


                new WallSegment(80, -50, 20, 20),
                new WallSegment(-80, 50, 20, 20),
                new WallSegment(110, 30, 20, 20),
                new WallSegment(-110, -30, 20, 20),
                new WallSegment(0, 0, 60, 20),


                // World boundaries:
                new WallSegment(0, WORLD_HEIGHT / 2 + 10, WORLD_WIDTH + 40, 20),
                new WallSegment(0, -WORLD_HEIGHT / 2 - 10, WORLD_WIDTH + 40, 20),
                new WallSegment(WORLD_WIDTH / 2 + 10, 0, 20, WORLD_HEIGHT + 40),
                new WallSegment(-WORLD_WIDTH / 2 - 10, 0, 20, WORLD_HEIGHT + 40),
        };

        mPowerupList = new PowerUp[MAX_POWERUPS];
        for (int i = 0; i < MAX_POWERUPS; i++) {
            mPowerupList[i] = new PowerUp();
        }
    }

    public static GameState getInstance() {
        return sGameStateInstance;
    }

    public ParticleGlitter getShots() {
        return mShots;
    }

    public ParticleGlitter getExplosions() {
        return mExplosions;
    }

    /**
     * The Surface is created/init()
     */
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        //Settings
        // Set the background frame color
        GLES20.glClearColor(0.5f, 0.0f, 0.0f, 1.0f);

        float aspectRatio = mWindowWidth / mWindowHeight;
        Matrix.orthoM(mMVPMatrix, 0, -100 * aspectRatio, 100 * aspectRatio, -100, 100, -1, 1);

        mShapeBuffer = new ShapeBuffer();
    }

    /* ***** Listener Events ***** */

    /**
     * Here we do our drawing
     */
    public void onDrawFrame(GL10 unused) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        float aspectRatio = mWindowWidth / mWindowHeight;
        Matrix.orthoM(mMVPMatrix, 0, -100 * aspectRatio, 100 * aspectRatio, -100, 100, -1, 1);

        if (mShapeBuffer == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        float timeFactor = (float) (currentTime - mLastUpdateTime) / MS_PER_FRAME;
        update(timeFactor);
        draw();
        mLastUpdateTime = currentTime;
    }

    /**
     * If the surface changes, reset the view
     */
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        if (height == 0) {
            // Prevent a divide by zero.
            height = 1;
        }
        mWindowWidth = width;
        mWindowHeight = height;
    }

    /**
     * Override the touch screen listener.
     * <p/>
     * React to moves and presses on the touchscreen.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //We handled the event
        return true;
    }

    public void scorePoint(int playerID) {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (mPlayerList[i].isActive && mPlayerList[i].shipIndex == playerID) {
                mPlayerList[i].pointsEarned++;
                if (mPlayerList[i].pointsEarned >= 5) {
                    Log.i("GameControllerSample", "Match over! - Player " + playerID + " has " +
                            mPlayerList[i].pointsEarned + " points!");
                    for (int j = 0; j < MAX_PLAYERS; j++) {
                        if (mPlayerList[j].isActive && mPlayerList[j].shipIndex != playerID) {
                            mPlayerList[j].explodeForMatchEnd();
                        }
                    }
                    mPlayerList[i].pointsEarned = 0;
                    mBackground.transitionColorTo(mPlayerList[i].getColor(), 60 * 1.5f);
                    mBackgroundResetCounter = 60 * 4;
                }
            }
        }
    }

    protected void ringBurst(float x, float y, Utils.Color color, float minSpeed,
                             float maxSpeed, int count) {
        for (int i = 0; i < count; i++) {
            float xx, yy;
            xx = Utils.randInRange(-1, 1);
            yy = Utils.randInRange(-1, 1);
            float mag = (float) Math.sqrt(xx * xx + yy * yy);
            if (mag == 0) {
                mag = 1;
                xx = 1;
            }
            float speed = Utils.randInRange(minSpeed, maxSpeed) / mag;

            BaseParticle mySquare = mExplosions.addParticle(
                    x, y,                                  //position
                    1.5f * xx * speed, 1.5f * yy * speed,  //dx/dy
                    color,                                 //color
                    Utils.randInRange(15, 45),             //fuse
                    Utils.randInRange(0.5f, 2)             //size
            );
            if (mySquare != null) {
                mySquare.maxAlpha = 0.25f;
            }
        }
    }

    public boolean handleInputEvent(InputEvent e) {
        boolean wasHandled = false;
        int ledNumber = InputDevice.getDevice(e.getDeviceId()).getControllerNumber() - 1;
        Log.v("GameControllerSample", "----------------------------------------------");
        Log.v("GameControllerSample", "Input event: ");
        Log.v("GameControllerSample", "Source: " + e.getSource());
        Log.v("GameControllerSample", "isFromSource(gamepad): " +
                e.isFromSource(InputDevice.SOURCE_GAMEPAD));
        Log.v("GameControllerSample", "isFromSource(joystick): " +
                e.isFromSource(InputDevice.SOURCE_JOYSTICK));
        Log.v("GameControllerSample", "isFromSource(touch nav): " +
                e.isFromSource(InputDevice.SOURCE_TOUCH_NAVIGATION));
        Log.v("GameControllerSample", "LED: " + ledNumber);
        Log.v("GameControllerSample", "----------------------------------------------");

        if (ledNumber != -1) {
            mPlayerList[ledNumber].makeActiveIfNotActive();
            mPlayerList[ledNumber].deviceId = e.getDeviceId();
            mPlayerList[ledNumber].getController().handleInputEvent(e);
            wasHandled = true;
        }
        return wasHandled;
    }

    public void update(float timeFactor) {
        mBackground.update(timeFactor);
        mExplosions.update(timeFactor);
        mShots.update(timeFactor);

        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (mPlayerList[i].isActive) {
                mPlayerList[i].update(timeFactor);
            }
        }

        for (int i = 0; i < mWallList.length; i++) {
            mWallList[i].update(timeFactor);
        }
        for (int i = 0; i < mPowerupList.length; i++) {
            mPowerupList[i].update(timeFactor);
        }

        // Important - make sure this is last, so we can identify presses/releases correctly.
        for (int i = 0; i < MAX_PLAYERS; i++) {
            mPlayerList[i].getController().advanceFrame();
        }

        if (mBackgroundResetCounter > 0) {
            mBackgroundResetCounter -= timeFactor;
            if (mBackgroundResetCounter <= 0) {
                mBackgroundResetCounter = 0;
                mBackground.transitionColorTo(new Utils.Color(0.0f, 0.0f, 0.5f, 1.0f), 60 * 1.5f);
            }
        }
    }

    public void draw() {
        // If the draw happens faster than update, we don't
        // need to repopulate the buffers...
        mShapeBuffer.clear();
        mBackground.draw(mShapeBuffer);
        for (int i = 0; i < mWallList.length; i++) {
            mWallList[i].draw(mShapeBuffer);
        }
        for (int i = 0; i < mPowerupList.length; i++) {
            mPowerupList[i].draw(mShapeBuffer);
        }

        mExplosions.draw(mShapeBuffer);
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (mPlayerList[i].isActive) {
                mPlayerList[i].draw(mShapeBuffer);
            }
        }
        // Draw shots above everything else.
        mShots.draw(mShapeBuffer);

        CameraMode cameraMode = CameraMode.SPLIT_SCREEN;

        if (cameraMode == CameraMode.SPLIT_SCREEN) {
            // Indirection needed because numberofcameras is not always number of active players.
            int numberOfCameras = mCameraLocations[getActivePlayerCount()].length;

            for (int i = 0; i < numberOfCameras; i++) {
                float scale = 1;
                float cameraX = 0;
                float cameraY = 0;

                Spaceship targetShip = getPlayerByNumber(i);

                if (targetShip != null && targetShip.isActive) {
                    cameraX = targetShip.cameraX;
                    cameraY = targetShip.cameraY;
                    scale = 2;
                }


                GLES20.glViewport(
                        (int) (mCameraLocations[numberOfCameras][i].x * mWindowWidth),
                        (int) (mCameraLocations[numberOfCameras][i].y * mWindowHeight),
                        (int) (mCameraLocations[numberOfCameras][i].width * mWindowWidth),
                        (int) (mCameraLocations[numberOfCameras][i].height * mWindowHeight));

                float ratio = (float) (mWindowWidth * mCameraLocations[numberOfCameras][i].width)
                        / (mWindowHeight * mCameraLocations[numberOfCameras][i].height);

                Matrix.orthoM(mProjMatrix, 0, -100 * ratio, 100 * ratio, -100, 100, -1, 1);

                Matrix.setIdentityM(mMVPMatrix, 0);
                Matrix.scaleM(mMVPMatrix, 0, scale, scale, 1);
                Matrix.translateM(mMVPMatrix, 0, -cameraX, -cameraY, 0);

                mShapeBuffer.draw(mProjMatrix, mMVPMatrix);
            }
        } else if (cameraMode == CameraMode.SHARED_SCREEN) {
            GLES20.glViewport(0, 0, (int) mWindowWidth, (int) mWindowHeight);
            float ratio = (float) mWindowWidth / mWindowHeight;

            // This projection matrix is applied to object coordinates
            // in the onDrawFrame() method
            //Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
            mShapeBuffer.draw(mProjMatrix, mMVPMatrix);
        }
    }

    public int getActivePlayerCount() {
        int count = 0;
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (mPlayerList[i].isActive) {
                count++;
            }
        }
        return count;
    }

    public Spaceship getPlayerByNumber(int n) {
        // Returns the nth player, or null if there isn't one.
        if (n >= 0 && n < MAX_PLAYERS) {
            return mPlayerList[n];
        } else {
            return null;
        }
    }

    @Override
    public void onInputDeviceAdded(int arg0) {
        Log.d("GameControllerSample", "Device added: " + arg0);
    }

    @Override
    public void onInputDeviceChanged(int arg0) {
        Log.d("GameControllerSample", "Device changed: " + arg0);
    }

    @Override
    public void onInputDeviceRemoved(int arg0) {
        Log.d("GameControllerSample", "Device removed: " + arg0);
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (mPlayerList[i].isActive && mPlayerList[i].deviceId == arg0) {
                Log.i("GameControllerSample", "Deactivated player: " + i);
                mPlayerList[i].deactivateShip();
            }
        }
    }

    enum CameraMode {
        SHARED_SCREEN,
        SPLIT_SCREEN
    }

    class ViewportLocation {
        float x, y;
        float width, height;

        ViewportLocation(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
