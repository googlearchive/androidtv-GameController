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

package com.google.fpl.gamecontroller.particles;

import com.google.fpl.gamecontroller.GameState;
import com.google.fpl.gamecontroller.Utils;

/**
 * Manages particles that makeup the background of the map.
 */
public class BackgroundParticleSystem extends ParticleSystem {

    // Parameters used for creating the animated background particles.
    private static final float BACKGROUND_SQUARE_MIN_LIFETIME = 2.0f; // seconds
    private static final float BACKGROUND_SQUARE_MAX_LIFETIME = 5.0f; // seconds
    private static final float BACKGROUND_SQUARE_MIN_SIZE = 5.0f;
    private static final float BACKGROUND_SQUARE_MAX_SIZE = 30.0f;
    private static final float BACKGROUND_SQUARE_MIN_VELOCITY_Y = 0.5f;
    private static final float BACKGROUND_SQUARE_MAX_VELOCITY_Y = 1.5f;
    private static final float BACKGROUND_SQUARE_MAX_ALPHA = 0.25f;

    // The number of frames it takes to transition to the winning player's color.
    private static final float WINNING_ANIMATION_TRANSITION_FRAME_DELTA
            = GameState.secondsToFrameDelta(1.5f);
    // The number of frames the background will show the winning player's color.
    private static final float WINNING_ANIMATION_PAUSE_FRAME_DELTA
            = GameState.secondsToFrameDelta(4.0f);

    // Default background color is dark blue.
    private static final Utils.Color DEFAULT_COLOR = new Utils.Color(0.0f, 0.0f, 0.5f, 1.0f);

    // The color for this frame.
    private final Utils.Color mCurrentColor = new Utils.Color(DEFAULT_COLOR);
    // The background color before the animation started.
    private final Utils.Color mOriginalColor = new Utils.Color(DEFAULT_COLOR);
    // The color of the background at the end of the animation.
    private final Utils.Color mTargetColor = new Utils.Color(DEFAULT_COLOR);

    // The amount of time needed to transition from the original color to target color.
    private float mTotalTransitionFrameDelta = 0.0f;
    // The current elapsed time.
    private float mCurrentTransitionFrameDelta = 0.0f;
    // After transitioning to the target color, the background will return to its original
    // color after the pause time has expired.
    private float mPauseFrameDeltaRemaining = 0.0f;


    public BackgroundParticleSystem(int maxActiveParticles) {
        super(maxActiveParticles, false);
    }

    public void update(float frameDelta) {
        // Add a new square every frame (the particles time out after a few seconds, so this
        // replaces them).
        addRandomSquare();

        if (mTotalTransitionFrameDelta > 0.0f
                && mCurrentTransitionFrameDelta < mTotalTransitionFrameDelta) {
            // We are transitioning from one color to another.
            mCurrentTransitionFrameDelta += frameDelta;
            if (mCurrentTransitionFrameDelta >= mTotalTransitionFrameDelta) {
                // The transition is complete, so clamp to the target color.
                mCurrentColor.set(mTargetColor);
                if (mPauseFrameDeltaRemaining <= 0.0f) {
                    // We will not be returning to the original color, so mark the transition
                    // as complete.
                    mCurrentTransitionFrameDelta = 0.0f;
                    mTotalTransitionFrameDelta = 0.0f;
                }
            } else {
                // Compute the interpolated color between the original and final colors.
                float transitionRatio = mCurrentTransitionFrameDelta / mTotalTransitionFrameDelta;
                mCurrentColor.setToLerp(mTargetColor, mOriginalColor, transitionRatio);
            }
        } else if (mPauseFrameDeltaRemaining > 0.0f) {
            // We have completed a transition and are pausing before switching back to our original
            // color.
            mPauseFrameDeltaRemaining -= frameDelta;
            if (mPauseFrameDeltaRemaining <= 0.0f) {
                // The pause is over, so kick off a transition back to our original color.
                // Use "0" for the pause duration so that the transition will stop after
                // the target color is reached.
                flashColorTo(mOriginalColor, mTotalTransitionFrameDelta, 0.0f);
            }
        }

        // Update all our particles to the current background color.
        for (BaseParticle square : mParticles) {
            if (square.isActive()) {
                square.getColor().set(mCurrentColor);
            }
        }

        super.update(frameDelta);
    }

    /**
     * Changes the color of the background particles over time.
     *
     * Calling this function while an animated transition is in progress has no effect.
     *
     * @param winningColor The new color for the background particles.
     */
    public void flashWinningColor(Utils.Color winningColor) {
        // Make sure we're not already in the middle of an animation.
        if ((mTotalTransitionFrameDelta == 0.0f) && (mPauseFrameDeltaRemaining == 0.0f)) {
            flashColorTo(winningColor, WINNING_ANIMATION_TRANSITION_FRAME_DELTA,
                    WINNING_ANIMATION_PAUSE_FRAME_DELTA);
        } else {
            Utils.logDebug("flashWinningColor() already in progress.");
        }
    }

    /**
     * Creates a new square particle.
     */
    private void addRandomSquare() {
        float lifetimeInSeconds = Utils.randFloatInRange(
                BACKGROUND_SQUARE_MIN_LIFETIME,
                BACKGROUND_SQUARE_MAX_LIFETIME);
        BaseParticle square = spawnParticle(lifetimeInSeconds);

        if (square != null) {
            // The particles start towards the bottom of the map.
            float x = Utils.randFloatInRange(GameState.MAP_LEFT_COORDINATE,
                    GameState.MAP_RIGHT_COORDINATE);
            float y = Utils.randFloatInRange(
                    GameState.WORLD_BOTTOM_COORDINATE - BACKGROUND_SQUARE_MAX_SIZE, 0.0f);
            square.setPosition(x, y);

            // The particles move from the bottom of the screen towards the top of the screen.
            float velocityY = Utils.randFloatInRange(
                    BACKGROUND_SQUARE_MIN_VELOCITY_Y,
                    BACKGROUND_SQUARE_MAX_VELOCITY_Y);
            square.setSpeed(0.0f, velocityY);

            square.setColor(mCurrentColor);
            square.setMaxAlpha(BACKGROUND_SQUARE_MAX_ALPHA);
            square.setSize(Utils.randFloatInRange(BACKGROUND_SQUARE_MIN_SIZE,
                    BACKGROUND_SQUARE_MAX_SIZE));
            square.setDieOffscreen(false);
        }
    }

    /**
     * Changes the color of the background particles over time.
     *
     * @param targetColor The new color for the background particles.
     * @param transitionFrameDelta The number of frames over which the color change
     *                           will occur.
     * @param pauseFrameDelta If greater than zero, the number of frames to draw
     *                      before transitioning the background particles back to their
     *                      original color.  If zero, the background particles will continue to
     *                      use targetColor after the transition is complete.
     */
    private void flashColorTo(Utils.Color targetColor, float transitionFrameDelta,
                                float pauseFrameDelta) {
        mTargetColor.set(targetColor);
        mOriginalColor.set(mCurrentColor);
        mTotalTransitionFrameDelta = Math.max(transitionFrameDelta, 0.0f);
        mCurrentTransitionFrameDelta = 0.0f;
        if (mTotalTransitionFrameDelta <= 0.0f) {
            // Make the transition instantaneous.
            mCurrentColor.set(mTargetColor);
            mTotalTransitionFrameDelta = 0.0f;
        }
        mPauseFrameDeltaRemaining = Math.max(pauseFrameDelta, 0.0f);
    }
}
