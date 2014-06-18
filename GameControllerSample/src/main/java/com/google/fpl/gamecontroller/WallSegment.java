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

import com.google.fpl.gamecontroller.particles.BaseParticle;

/**
 * Handles drawing and collision-detection with map walls.
 */
public class WallSegment {
    private static final Utils.Color WALL_COLOR = Utils.Color.WHITE;
    private static final int EDGE_COUNT = 4;

    private final float mCenterX, mCenterY;
    private final float mWidth, mHeight;

    // The center points (x, y) for each edge of the wall.
    private final float[] mEdgeCenters = new float[EDGE_COUNT * 2];
    // The scale values (x, y) for each edge of the wall.
    private final float[] mEdgeScales = new float[EDGE_COUNT * 2];

    public WallSegment(float x, float y, float width, float height) {
        mCenterX = x;
        mCenterY = y;
        mWidth = width;
        mHeight = height;

        // Compute the center and scale needed to draw lines along each edge of the wall.
        int edgeIndex = 0;

        // Left edge.
        mEdgeCenters[edgeIndex + 0] = mCenterX - mWidth / 2.0f;
        mEdgeCenters[edgeIndex + 1] = mCenterY;
        mEdgeScales[edgeIndex + 0] = 1.0f + mHeight / 2.0f;
        mEdgeScales[edgeIndex + 1] = 1.0f;
        edgeIndex += 2;

        // Right edge.
        mEdgeCenters[edgeIndex + 0] = mCenterX + mWidth / 2.0f;
        mEdgeCenters[edgeIndex + 1] = mCenterY;
        mEdgeScales[edgeIndex + 0] = 1.0f + mHeight / 2.0f;
        mEdgeScales[edgeIndex + 1] = 1.0f;
        edgeIndex += 2;

        // Bottom edge.
        mEdgeCenters[edgeIndex + 0] = mCenterX;
        mEdgeCenters[edgeIndex + 1] = mCenterY - mHeight / 2.0f;
        mEdgeScales[edgeIndex + 0] = 1.0f;
        mEdgeScales[edgeIndex + 1] = 1.0f + mWidth / 2.0f;
        edgeIndex += 2;

        // Top edge.
        mEdgeCenters[edgeIndex + 0] = mCenterX;
        mEdgeCenters[edgeIndex + 1] = mCenterY + mHeight / 2.0f;
        mEdgeScales[edgeIndex + 0] = 1.0f;
        mEdgeScales[edgeIndex + 1] = 1.0f + mWidth / 2.0f;
    }

    public void draw(ShapeBuffer sb) {
        // Draw a line along each edge of the wall.
        for (int i = 0; i < EDGE_COUNT; ++i) {
            final int edgeIndex = i * 2;
            sb.add2DShape(
                    mEdgeCenters[edgeIndex + 0], mEdgeCenters[edgeIndex + 1],   // position
                    WALL_COLOR,                                                 // color
                    Utils.SQUARE_SHAPE,                                         // vertices
                    mEdgeScales[edgeIndex + 0], mEdgeScales[edgeIndex + 1],     // scale
                    0.0f, 1.0f);                                                // heading
        }
    }

    /**
     * Returns true if the given point is inside this wall.
     */
    public boolean isInWall(float x, float y) {
        return x >= mCenterX - mWidth / 2
                && x <= mCenterX + mWidth / 2
                && y >= mCenterY - mHeight / 2
                && y <= mCenterY + mHeight / 2;
    }

    /**
     * Checks for collisions with this wall.
     */
    public void update(float timeFactor) {
        GameState gameState = GameState.getInstance();

        // First, check for collisions with bullets.
        BaseParticle[] possibleHits = gameState.getShots().getPotentialCollisions(mCenterX,
                mCenterY, mWidth, mHeight);

        BaseParticle currentBullet;
        // The possibleHits array will likely be longer than the number of entries returned.
        // Look for a null entry to indicate the end of the list.
        for (int i = 0; possibleHits[i] != null; i++) {
            currentBullet = possibleHits[i];
            if (isInWall(currentBullet.getPositionX(), currentBullet.getPositionY())) {
                // Semi-hacky way to zero in on the collision point.
                // When we detect a bullet is inside the wall, iterate backwards and forwards
                // along the trajectory of the bullet by smaller and smaller steps.
                // This should get us fairly close to the intersection point.
                //
                // For better collision detection, we could compute the actual line
                // intersection point, instead of just approximating it.
                float stepSize = -0.5f;
                for (int j = 0; j < 3; ++j) {
                    currentBullet.incrementPosition(stepSize);
                    if (isInWall(currentBullet.getPositionX(), currentBullet.getPositionY())) {
                        stepSize = -Math.abs(stepSize) * 0.5f;
                    } else {
                        stepSize = Math.abs(stepSize) * 0.5f;
                    }
                }
                currentBullet.handleCollision();
            }
        }

        // Now check for ship-wall collisions.
        for (Spaceship currentPlayer : gameState.getPlayerList()) {
            if (currentPlayer.isActive()
                    && isInWall(currentPlayer.getPositionX(), currentPlayer.getPositionY())) {
                float xx = (currentPlayer.getPositionX() - mCenterX) * mHeight;
                float yy = (currentPlayer.getPositionY() - mCenterY) * mWidth;

                float epsilon = 0.1f;

                if (Math.abs(xx) > Math.abs(yy)) {
                    if (xx >= 0) {
                        currentPlayer.setPositionX(mCenterX + mWidth / 2 + epsilon);
                    } else {
                        currentPlayer.setPositionX(mCenterX - mWidth / 2 - epsilon);
                    }
                } else {
                    if (yy >= 0) {
                        currentPlayer.setPositionY(mCenterY + mHeight / 2 + epsilon);
                    } else {
                        currentPlayer.setPositionY(mCenterY - mHeight / 2 - epsilon);
                    }
                }
            }
        }
    }
}
