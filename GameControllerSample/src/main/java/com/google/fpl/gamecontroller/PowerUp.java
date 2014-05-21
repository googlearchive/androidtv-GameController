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

/**
 * Handles drawing, placing and detecting collisions with power-ups.
 */
public class PowerUp {

    private static final float RESPAWN_TIME = 60 * 3;
    private static final float RESPAWN_VARIANCE = 60 * 3;
    private static final float RADIUS_SQUARED = 5 * 5;
    private static final float[] SQUARE_SHAPE = {
            1, 1,
            -1, 1,
            1, -1,
            -1, -1
    };

    private float mX, mY;
    private float mRotation = 0;
    private float mRespawnCounter = RESPAWN_TIME;

    public void update(float timeFactor) {
        GameState gameState = GameState.getInstance();

        if (mRespawnCounter > 0) {
            mRespawnCounter -= timeFactor;
            if (mRespawnCounter <= 0) {
                mRespawnCounter = 0;
                pickNewLocation();
            }
            if (mRespawnCounter <= 0) {
                gameState.ringBurst(mX, mY, new Utils.Color(1.0f, 1.0f, 1.0f, 1.0f), 1, 1, 50);
            }
        }

        mRotation += timeFactor;

        if (isSpawned()) {
            gameState.ringBurst(mX, mY, new Utils.Color(1.0f, 1.0f, 1.0f, 1.0f), 0.05f, 0.25f, 1);
            for (int i = 0; i < gameState.mPlayerList.length; i++) {
                if (gameState.mPlayerList[i].isActive &&
                        ((mX - gameState.mPlayerList[i].x) * (mX - gameState.mPlayerList[i].x) +
                        (mY - gameState.mPlayerList[i].y) * (mY - gameState.mPlayerList[i].y) <
                        RADIUS_SQUARED)) {
                    gameState.mPlayerList[i].giveRandomWeapon();
                    mRespawnCounter = Utils.randInRange(RESPAWN_TIME, RESPAWN_TIME +
                            RESPAWN_VARIANCE);
                    break;
                }
            }
        }
    }

    public boolean isSpawned() {
        return mRespawnCounter == 0;
    }


    public void draw(ShapeBuffer shapeBuffer) {
        if (isSpawned()) {
            float rotationSpeed = 0.05f;

            float alpha = ((float) Math.sin(mRotation * rotationSpeed * 3) + 2) / 3f;
            shapeBuffer.addShape(mX, mY, new Utils.Color(1.0f, 1.0f, 1.0f, alpha),
                    SQUARE_SHAPE, 3, 3, (float) Math.sin(mRotation * rotationSpeed),
                    (float) Math.cos(mRotation * rotationSpeed));
        }
    }


    public void pickNewLocation() {
        GameState gameState = GameState.getInstance();

        boolean validLocation = false;
        for (int tries = 0; tries < 10; tries++) {
            mX = Utils.randInRange(-GameState.WORLD_WIDTH * 0.45f, GameState.WORLD_WIDTH * 0.45f);
            mY = Utils.randInRange(-GameState.WORLD_HEIGHT * 0.45f, GameState.WORLD_HEIGHT * 0.45f);
            // Assume it's true, until it fails...
            validLocation = true;
            // Don't spawn in a wall.
            for (int i = 0; i < gameState.mWallList.length; i++) {
                if (gameState.mWallList[i].isInWall(mX, mY)) {
                    validLocation = false;
                    break;
                }
            }
            for (int i = 0; i < gameState.mPlayerList.length; i++) {
                // Don't spawn too close to a player.
                if (gameState.mPlayerList[i].isActive &&
                        ((mX - gameState.mPlayerList[i].x) * (mX - gameState.mPlayerList[i].x) +
                        (mY - gameState.mPlayerList[i].y) * (mY - gameState.mPlayerList[i].y) <
                        10 * 10)) {
                    validLocation = false;
                    break;
                }
            }
        }
        if (!validLocation) {
            // Try again in a second.
            mRespawnCounter = 60 * 1;
        }
    }
}
