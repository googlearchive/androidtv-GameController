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
import android.view.InputDevice;
import android.view.InputEvent;

import com.google.fpl.gamecontroller.particles.Background;
import com.google.fpl.gamecontroller.particles.ParticleGlitter;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * A singleton class that manages the OpenGL context, camera, players, etc.
 */
public class GameState extends GLSurfaceView implements Renderer, InputDeviceListener {

    // The "world" is everything that is visible on the screen.  The world extends to the outside
    // edges of the walls that surround the world.
    public static final int WORLD_WIDTH = 1280 / 4;
    public static final int WORLD_HEIGHT = 720 / 4;
    // Define the rectangle that bounds the world.  The coordinate system used by the world
    // is centered around 0, 0.
    public static final int WORLD_TOP_COORDINATE = WORLD_HEIGHT / 2;
    public static final int WORLD_BOTTOM_COORDINATE = -WORLD_HEIGHT / 2;
    public static final int WORLD_LEFT_COORDINATE = -WORLD_WIDTH / 2;
    public static final int WORLD_RIGHT_COORDINATE = WORLD_WIDTH / 2;

    // Mapping for Z values when projected into the world.
    private static final float WORLD_NEAR_PLANE = -1.0f;
    private static final float WORLD_FAR_PLANE = 1.0f;
    private static final float WORLD_ASPECT_RATIO = (float) WORLD_WIDTH / (float) WORLD_HEIGHT;

    // The thickness of the walls that bound the world.
    private static final int MAP_WALL_THICKNESS = 20;

    // The "map" is the area where ships can move.  The map is bounded by the inside edges of
    // that walls that surround the world.
    public static final int MAP_WIDTH = WORLD_WIDTH - 2 * MAP_WALL_THICKNESS;
    public static final int MAP_HEIGHT = WORLD_HEIGHT - 2 * MAP_WALL_THICKNESS;
    // Define the rectangle that bounds the map.  The map and the world share the same coordinate
    // system centered at 0, 0.
    public static final int MAP_TOP_COORDINATE = MAP_HEIGHT / 2;
    public static final int MAP_BOTTOM_COORDINATE = -MAP_HEIGHT / 2;
    public static final int MAP_LEFT_COORDINATE = -MAP_WIDTH / 2;
    public static final int MAP_RIGHT_COORDINATE = MAP_WIDTH / 2;

    // Set to "true" to print info about every controller event.
    private static final boolean CONTROLLER_DEBUG_PRINT = false;

    // An arbitrary frame-rate used to compute the "frameDelta" value that is passed to
    // update and other functions that require a time delta.  This frame-rate does not have
    // any relationship to the actual rate at which the screen refreshes.
    // See onDrawFrame() and update() for more info on how this value is used.
    private static final float ANIMATION_FRAMES_PER_SECOND = 60.0f;

    // The maximum number of controllers supported by this game.
    private static final int MAX_PLAYERS = 4;

    // The number of "power-ups" to draw on the map.
    private static final int MAX_POWERUPS = 2;

    // The first player to join is red, second is green, etc.
    private static final Utils.Color PLAYER_COLORS[] = new Utils.Color[] {
            Utils.Color.RED,
            Utils.Color.GREEN,
            Utils.Color.YELLOW,
            Utils.Color.BLUE
    };

    // The number of points a player must score to win a match.
    private static final int POINTS_PER_MATCH = 5;

    // Singleton instance of the GameState.
    private static GameState sGameStateInstance = null;

    // The combined model, view, and projection matrices.
    private final float[] mMVPMatrix = new float[16];

    // The window dimensions in pixels.
    private int mWindowWidth, mWindowHeight;

    // One Spaceship per controller.
    private Spaceship[] mPlayerList;

    // The animated background particles.
    private Background mBackground;

    // Manages the shots fired by the ships.
    private ParticleGlitter mShots;

    // Manages explosion particles.
    private ParticleGlitter mExplosions;

    // The walls and other obstacles in the world.
    private WallSegment[] mWallList;

    // All geometry for the frame goes into a single ShapeBuffer.
    private ShapeBuffer mShapeBuffer = null;

    // The list of power ups shown on the map.
    private PowerUp[] mPowerupList;

    // The system time (in milliseconds) of the last frame update.
    private long mLastUpdateTimeMillis;

    /**
     * @return The global GameState object.
     */
    public static GameState getInstance() {
        return sGameStateInstance;
    }

    /**
     * Converts a duration in seconds to a duration in number of elapsed frames.
     */
    public static float secondsToFrameDelta(float seconds) {
        return seconds * ANIMATION_FRAMES_PER_SECOND;
    }
    /**
     * Converts a duration in milliseconds to a duration in number of elapsed frames.
     */
    public static float millisToFrameDelta(long milliseconds) {
        return secondsToFrameDelta((float) milliseconds / 1000.0f);
    }

    /**
     * Set this class as renderer for this GLSurfaceView.
     * Request Focus and set if focusable in touch mode to
     * receive the Input from Screen
     *
     * @param context - The Activity Context.
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

        // Create the lists of players and power-ups.
        mPlayerList = new Spaceship[MAX_PLAYERS];
        for (int i = 0; i < mPlayerList.length; i++) {
            mPlayerList[i] = new Spaceship(this, i, PLAYER_COLORS[i]);
        }
        mPowerupList = new PowerUp[MAX_POWERUPS];
        for (int i = 0; i < mPowerupList.length; i++) {
            mPowerupList[i] = new PowerUp();
        }

        mBackground = new Background();
        mExplosions = new ParticleGlitter();

        // The true here means we want collision tracking data.
        mShots = new ParticleGlitter(true);

        mLastUpdateTimeMillis = System.currentTimeMillis();

        InputManager inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        inputManager.registerInputDeviceListener(this, null);

        buildMap();
    }

    /* ***** Listener Events ***** */
    @Override
    public void onInputDeviceAdded(int arg0) {
        Utils.logDebug("Device added: " + arg0);
    }

    @Override
    public void onInputDeviceChanged(int arg0) {
        Utils.logDebug("Device changed: " + arg0);
    }

    @Override
    public void onInputDeviceRemoved(int arg0) {
        Utils.logDebug("Device removed: " + arg0);
        // Deactivate a player when their corresponding input device is removed.
        for (Spaceship player : mPlayerList) {
            if (player.isActive() && player.getController().getDeviceId() == arg0) {
                Utils.logDebug("Deactivated player: " + arg0);
                player.deactivateShip();
            }
        }
    }

    public ParticleGlitter getShots() {
        return mShots;
    }
    public ParticleGlitter getExplosions() {
        return mExplosions;
    }
    public Background getBackgroundParticles() {
        return mBackground;
    }
    public Spaceship[] getPlayerList() {
        return mPlayerList;
    }
    public WallSegment[] getWallList() {
        return mWallList;
    }

    /**
     * The Surface is created/init()
     */
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // The ShapeBuffer creates OpenGl resources, so don't create it until after the
        // primary rendering surface has been created.
        mShapeBuffer = new ShapeBuffer();
        mShapeBuffer.loadResources();
    }

    /**
     * Here we do our drawing
     */
    public void onDrawFrame(GL10 unused) {
        // Clear the screen to black.
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Don't try to draw if the shape buffer failed to initialize.
        if (!mShapeBuffer.isInitialized()) {
            return;
        }

        long currentTimeMillis = System.currentTimeMillis();

        // Compute frame delta.  frameDelta = # of "ideal" frames that have occurred since the
        // last update.  "ideal" assumes a constant frame-rate (60 FPS or 16.7 milliseconds per
        // frame).  Since the delta doesn't depend on the "real" frame-rate, the animations always
        // run at the same wall clock speed, regardless of what the real refresh rate is.
        //
        // frameDelta was used instead of a time delta in order to make the values passed
        // to update easier to understand when debugging the code.  For example, a frameDelta
        // of "1.5" means that one and a half hypothetical frames have passed since the last
        // update.  In wall time this would be 25 milliseconds or 0.025 seconds.
        float frameDelta = millisToFrameDelta(currentTimeMillis - mLastUpdateTimeMillis);

        update(frameDelta);
        draw();
        mLastUpdateTimeMillis = currentTimeMillis;
    }

    /**
     * If the surface changes, reset the view size.
     */
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Make sure the window dimensions are never 0.
        mWindowWidth = Math.max(width, 1);
        mWindowHeight = Math.max(height, 1);
    }

    /**
     * Indicate that the given player has scored a point.
     *
     * Will end the match if the scoring player has enough points to win.
     *
     * @param playerId - the id of the scoring player.
     */
    public void scorePoint(int playerId) {
        for (Spaceship player : mPlayerList) {
            if (player.isActive() && player.getPlayerId() == playerId) {
                player.changeScore(1);
                // See if the scoring player has enough points to win the match.
                if (player.getScore() >= POINTS_PER_MATCH) {
                    endMatch(player);
                }
            }
        }
    }

    /**
     * Handles game controller input.
     *
     * Events that do not come from game controllers are ignored.  Game controller events
     * are routed to the correct player's controller object.
     *
     * @param event - The InputEvent to handle.
     * @return - true if the event was handled.
     */
    public boolean handleInputEvent(InputEvent event) {
        boolean wasHandled = false;

        // getControllerNumber() will return "0" for devices that are not game controllers or
        // joysticks.
        int controllerNumber = InputDevice.getDevice(event.getDeviceId()).getControllerNumber() - 1;

        if (CONTROLLER_DEBUG_PRINT) {
            Utils.logDebug("----------------------------------------------");
            Utils.logDebug("Input event: ");
            Utils.logDebug("Source: " + event.getSource());
            Utils.logDebug("isFromSource(gamepad): "
                    + event.isFromSource(InputDevice.SOURCE_GAMEPAD));
            Utils.logDebug("isFromSource(joystick): "
                    + event.isFromSource(InputDevice.SOURCE_JOYSTICK));
            Utils.logDebug("isFromSource(touch nav): "
                    + event.isFromSource(InputDevice.SOURCE_TOUCH_NAVIGATION));
            Utils.logDebug("Controller: " + controllerNumber);
            Utils.logDebug("----------------------------------------------");
        }

        if (controllerNumber >= 0 && controllerNumber < mPlayerList.length) {
            mPlayerList[controllerNumber].makeActiveIfNotActive(event.getDeviceId());
            mPlayerList[controllerNumber].getController().handleInputEvent(event);
            wasHandled = true;
        } else {
            Utils.logDebug("Unhandled input event.");
        }
        return wasHandled;
    }

    /**
     * Update positions, animations, etc.
     *
     * @param frameDelta - The amount of time (in "frame units") that has elapsed since the last
     *                   call to update().
     */
    public void update(float frameDelta) {
        mBackground.update(frameDelta);
        mExplosions.update(frameDelta);
        mShots.update(frameDelta);

        for (Spaceship player : mPlayerList) {
            // Only update the active players.
            if (player.isActive()) {
                player.update(frameDelta);
            }
        }

        for (WallSegment wall : mWallList) {
            wall.update(frameDelta);
        }
        for (PowerUp powerUp : mPowerupList) {
            powerUp.update(frameDelta);
        }
    }

    /**
     * Draws the world.
     */
    public void draw() {
        // Each world element adds triangles to the shape buffer.  No OpenGl calls are made
        // until after the whole scene has been added to the shape buffer.
        mShapeBuffer.clear();
        mBackground.draw(mShapeBuffer);
        for (WallSegment wall : mWallList) {
            wall.draw(mShapeBuffer);
        }
        for (PowerUp powerUp : mPowerupList) {
            powerUp.draw(mShapeBuffer);
        }

        mExplosions.draw(mShapeBuffer);
        for (Spaceship player : mPlayerList) {
            if (player.isActive()) {
                player.draw(mShapeBuffer);
            }
        }
        // Draw shots above everything else.
        mShots.draw(mShapeBuffer);

        // Prepare for rendering to the screen.
        updateViewportAndProjection();

        // Send the triangles to OpenGl.
        mShapeBuffer.draw(mMVPMatrix);
    }

    /**
     * Builds the static map, including walls and obstacles.
     */
    protected void buildMap() {
        // The origin of the map coordinate system.
        final int mapCenterX = 0;
        final int mapCenterY = 0;

        final int mapTopWallCenterY = MAP_TOP_COORDINATE + MAP_WALL_THICKNESS / 2;
        final int mapBottomWallCenterY = MAP_BOTTOM_COORDINATE - MAP_WALL_THICKNESS / 2;
        final int mapRightWallCenterX = MAP_RIGHT_COORDINATE + MAP_WALL_THICKNESS / 2;
        final int mapLeftWallCenterX = MAP_LEFT_COORDINATE - MAP_WALL_THICKNESS / 2;

        final int rectangleShortEdgeLength = 20;
        final int rectangleLongEdgeLength = 60;

        final int squareEdgeLength = 20;

        mWallList = new WallSegment[]{
                // Rectangles:
                // Rectangle touching top edge.
                new WallSegment(
                        mapCenterX + 40,
                        WORLD_TOP_COORDINATE - rectangleLongEdgeLength / 2,
                        rectangleShortEdgeLength,
                        rectangleLongEdgeLength),
                // Rectangle touching bottom edge.
                new WallSegment(
                        mapCenterX - 40,
                        WORLD_BOTTOM_COORDINATE + rectangleLongEdgeLength / 2,
                        rectangleShortEdgeLength,
                        rectangleLongEdgeLength),
                // Rectangle in center of map.
                new WallSegment(
                        mapCenterX,
                        mapCenterY,
                        rectangleLongEdgeLength,
                        rectangleShortEdgeLength),

                // Squares: one in each quadrant of the map.
                // Square in lower right.
                new WallSegment(
                        mapCenterX + 80,
                        mapCenterY - 50,
                        squareEdgeLength, squareEdgeLength),
                // Square in upper left.
                new WallSegment(
                        mapCenterX - 80,
                        mapCenterY + 50,
                        squareEdgeLength,
                        squareEdgeLength),
                // Square in upper right.
                new WallSegment(
                        mapCenterX + 110,
                        mapCenterY + 30,
                        squareEdgeLength,
                        squareEdgeLength),
                // Square in lower left.
                new WallSegment(
                        mapCenterX - 110,
                        mapCenterY - 30,
                        squareEdgeLength,
                        squareEdgeLength),

                // Walls: around the edge of the map.
                // Top
                new WallSegment(
                        mapCenterX,
                        mapTopWallCenterY,
                        WORLD_WIDTH,
                        MAP_WALL_THICKNESS),
                // Bottom
                new WallSegment(
                        mapCenterX,
                        mapBottomWallCenterY,
                        WORLD_WIDTH,
                        MAP_WALL_THICKNESS),
                // Right
                new WallSegment(
                        mapRightWallCenterX,
                        mapCenterY, MAP_WALL_THICKNESS,
                        WORLD_HEIGHT),
                // Left
                new WallSegment(
                        mapLeftWallCenterX,
                        mapCenterY,
                        MAP_WALL_THICKNESS,
                        WORLD_HEIGHT),
        };
    }

    /**
     * Computes the view projections and sets the OpenGl viewport.
     */
    protected void updateViewportAndProjection() {
        // Assume a square viewport if the width and height haven't been initialized.
        float viewportAspectRatio = 1.0f;
        if ((mWindowWidth > 0) && (mWindowHeight > 0)) {
            viewportAspectRatio = (float) mWindowWidth / (float) mWindowHeight;
        }
        float viewportWidth = (float) mWindowWidth;
        float viewportHeight = (float) mWindowHeight;
        float viewportOffsetX = 0.0f;
        float viewportOffsetY = 0.0f;

        if (WORLD_ASPECT_RATIO > viewportAspectRatio) {
            // Our window is taller than the ideal aspect ratio needed to accommodate the world
            // without stretching.
            // Reduce the viewport height to match the aspect ratio of the world.  The world
            // will fill the whole width of the screen, but have some empty space on the top and
            // bottom of the screen.
            viewportHeight = viewportWidth / WORLD_ASPECT_RATIO;
            // Center the viewport on the screen.
            viewportOffsetY = ((float) mWindowHeight - viewportHeight) / 2.0f;
        } else if (viewportAspectRatio > WORLD_ASPECT_RATIO) {
            // Our window is wider than the ideal aspect ratio needed to accommodate the world
            // without stretching.
            // Reduce the viewport width to match the aspect ratio of the world.  The world
            // will fill the whole height of the screen, but have some empty space on the
            // left and right of the screen.
            viewportWidth = viewportHeight * WORLD_ASPECT_RATIO;
            // Center the viewport on the screen.
            viewportOffsetX = ((float) mWindowWidth - viewportWidth) / 2.0f;
        }

        Matrix.orthoM(mMVPMatrix, 0,
                WORLD_LEFT_COORDINATE,
                WORLD_RIGHT_COORDINATE,
                WORLD_BOTTOM_COORDINATE,
                WORLD_TOP_COORDINATE,
                WORLD_NEAR_PLANE,
                WORLD_FAR_PLANE);
        GLES20.glViewport((int) viewportOffsetX, (int) viewportOffsetY,
                (int) viewportWidth, (int) viewportHeight);
    }

    /**
     * Prepares the game for a new match.
     *
     * @param winningPlayer - The winning ship from the last match.
     */
    private void endMatch(Spaceship winningPlayer) {
        Utils.logDebug("Match over! - Player " + winningPlayer.getPlayerId()
                + " has " + winningPlayer.getScore() + " points!");

        // Reset the score for each ship.
        for (Spaceship player : mPlayerList) {
            player.resetAtEndOfMatch(winningPlayer.getPlayerId());
        }

        // Set the background color to the winning player's color until the next
        // match starts.
        mBackground.flashWinningColor(winningPlayer.getColor());
    }
}
