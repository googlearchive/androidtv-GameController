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

import com.google.fpl.gamecontroller.particles.BaseParticle;

/**
 * Handles drawing and collosion-detection with map walls.
 */
public class WallSegment {
    private static final float[] WALL_SHAPE = {
            1, 1,
            -1, 1,
            1, -1,
            -1, -1
    };
    private float mCenterX, mCenterY;
    private float mWidth, mHeight;

    private static final Utils.Color WALL_COLOR = new Utils.Color(1.0f, 1.0f, 1.0f, 1.0f);

    public WallSegment(float x, float y, float width, float height) {
        mCenterX = x;
        mCenterY = y;
        mWidth = width;
        mHeight = height;
    }

    public void draw(ShapeBuffer sb) {
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;

        sb.addShape(mCenterX - mWidth / 2, mCenterY,
                WALL_COLOR,
                WALL_SHAPE,
                1 + mHeight / 2, 1,
                0, 1);
        sb.addShape(mCenterX + mWidth / 2, mCenterY,
                WALL_COLOR,
                WALL_SHAPE,
                1 + mHeight / 2, 1,
                0, 1);

        sb.addShape(mCenterX, mCenterY - mHeight / 2,
                WALL_COLOR,
                WALL_SHAPE,
                1, 1 + mWidth / 2,
                0, 1);
        sb.addShape(mCenterX, mCenterY + mHeight / 2,
                WALL_COLOR,
                WALL_SHAPE,
                1, 1 + mWidth / 2,
                0, 1);
    }

    public boolean isInWall(float x, float y) {
        return x >= mCenterX - mWidth / 2
                && x <= mCenterX + mWidth / 2
                && y >= mCenterY - mHeight / 2
                && y <= mCenterY + mHeight / 2;
    }

    public void update(float timeFactor) {
        GameState gameState = GameState.getInstance();

        BaseParticle[] possibleHits = gameState.mShots.getPotentialCollisions(mCenterX, mCenterY,
                mWidth, mHeight);

        BaseParticle currentParticle;
        for (int i = 0; possibleHits[i] != null; i++) {
            currentParticle = possibleHits[i];
            if (isInWall(currentParticle.x, currentParticle.y)) {
                // Semi-hacky bit to zero in on the collision point here
                // because it's faster to write than actual line intercept math
                // and I just want to make sure it makes rocket launchers cool again.
                float stepSize = -0.5f;
                for (int j = 0; j < 3; j++) {
                    currentParticle.x += currentParticle.deltaX * stepSize;
                    currentParticle.y += currentParticle.deltaY * stepSize;
                    if (isInWall(currentParticle.x, currentParticle.y)) {
                        stepSize = -Math.abs(stepSize) * 0.5f;
                    } else {
                        stepSize = Math.abs(stepSize) * 0.5f;
                    }
                }
                currentParticle.collision();
            }
        }

        for (int i = 0; i < gameState.mPlayerList.length; i++) {
            Spaceship currentPlayer = gameState.mPlayerList[i];
            if (currentPlayer.isActive && isInWall(currentPlayer.x, currentPlayer.y)) {
                float xx = (currentPlayer.x - mCenterX) * mHeight;
                float yy = (currentPlayer.y - mCenterY) * mWidth;

                float fudge = 0.1f;

                if (Math.abs(xx) > Math.abs(yy)) {
                    if (xx >= 0) {
                        currentPlayer.x = mCenterX + mWidth / 2 + fudge;
                    } else {
                        currentPlayer.x = mCenterX - mWidth / 2 - fudge;
                    }
                } else {
                    if (yy >= 0) {
                        currentPlayer.y = mCenterY + mHeight / 2 + fudge;
                    } else {
                        currentPlayer.y = mCenterY - mHeight / 2 - fudge;
                    }
                }
            }
        }
    }
}
