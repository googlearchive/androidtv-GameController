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

/**
 * Handles drawing, placing and detecting collisions with power-ups.
 */
public class PowerUp {

    // The number of frames to wait to wait after a powerup has been collected before
    // it respawns.
    private static final float RESPAWN_MIN_FRAME_COUNT = GameState.secondsToFrameDelta(3.0f);
    private static final float RESPAWN_MAX_FRAME_COUNT = GameState.secondsToFrameDelta(6.0f);

    private static final float ROTATION_RADIANS_PER_FRAME = 0.05f;

    // In radians, the amount the powerup's alpha value varies per frame.
    private static final float ALPHA_PULSE_RATE = 3.0f * ROTATION_RADIANS_PER_FRAME;
    // The minimum alpha value in the pulsing animation.
    private static final float ALPHA_PULSE_MIN_OPACITY = 2.0f / 3.0f;

    // When a ship gets within the powerup's COLLISION_RADIUS, it will collect the powerup.
    private static final float COLLISION_RADIUS = 5.0f;

    // Newly spawned powerups will not be placed too close to players.
    private static final float MIN_DISTANCE_TO_PLAYER = 10.0f;

    // The size of the powerup on screen.
    private static final float POWERUP_SCALE = 3.0f;

    // Parameters for the ring burst that is created when the power up spawns.
    private static final int RESPAWN_BURST_PARTICLE_COUNT = 50;
    private static final float RESPAWN_BURST_PARTICLE_SPEED = 1.5f;

    // Parameters for the steady stream of "steam" that comes from the powerup.
    private static final int STEAM_PARTICLE_COUNT = 1;
    private static final float STEAM_PARTICLE_MIN_SPEED = 0.075f;
    private static final float STEAM_PARTICLE_MAX_SPEED = 0.375f;

    // Don't try more than 10 times to find a valid location for the powerup.
    private static final int MAX_POWERUP_PLACEMENT_ATTEMPTS = 10;

    // The powerup should not be placed too close to the edge of the map.  The number below
    // scales the map size to limit how close the power can get to the edge.
    private static final float USABLE_MAP_PERCENT = 0.95f;

    private float mPositionX, mPositionY;
    private float mTotalFrameCount = 0;

    // The respawn counter begins as a small positive number so that the first update will
    // trigger a respawn.
    private float mRespawnCounter = 1.0f;

    private final Utils.Color mColor = new Utils.Color(1.0f, 1.0f, 1.0f, 1.0f);
    private float mHeadingX;
    private float mHeadingY;

    public void update(float timeFactor) {
        GameState gameState = GameState.getInstance();

        // See if it's time to respawn the powerup.
        if (mRespawnCounter > 0.0f) {
            mRespawnCounter -= timeFactor;
            if (mRespawnCounter <= 0.0f) {
                mRespawnCounter = 0.0f;
                pickNewLocation();
            }
            if (mRespawnCounter <= 0.0f) {
                gameState.getExplosions().spawnRingBurst(
                        mPositionX, mPositionY,
                        Utils.Color.WHITE,
                        RESPAWN_BURST_PARTICLE_SPEED, RESPAWN_BURST_PARTICLE_SPEED,
                        RESPAWN_BURST_PARTICLE_COUNT);
            }
        }

        // Keep track of the total elapsed frame count for updating our cyclical animations
        // (spinning and pulsing).
        mTotalFrameCount += timeFactor;

        // Compute an alpha value that varies between fully opaque (1.0)
        // and ALPHA_PULSE_MIN_OPACITY (0.66).
        float alpha = (float) Math.sin(mTotalFrameCount * ALPHA_PULSE_RATE);
        alpha = alpha * (1.0f - ALPHA_PULSE_MIN_OPACITY) + ALPHA_PULSE_MIN_OPACITY;
        mColor.setAlpha(alpha);

        // Compute the current direction of the powerup.
        mHeadingX = (float) Math.sin(mTotalFrameCount * ROTATION_RADIANS_PER_FRAME);
        mHeadingY = (float) Math.cos(mTotalFrameCount * ROTATION_RADIANS_PER_FRAME);

        if (isSpawned()) {
            // The powerup throws off a steady stream of "steam" particles.
            // One particle is added each frame.
            gameState.getExplosions().spawnRingBurst(
                    mPositionX, mPositionY,
                    Utils.Color.WHITE,
                    STEAM_PARTICLE_MIN_SPEED, STEAM_PARTICLE_MAX_SPEED,
                    STEAM_PARTICLE_COUNT);
            // Check to see if any player is close enough to pick up the powerup.
            for (Spaceship player : gameState.getPlayerList()) {
                if (player.isActive()) {
                    float distanceToPlayer = getDistanceToPoint(player.getPositionX(),
                            player.getPositionY());
                    if (distanceToPlayer < COLLISION_RADIUS) {
                        player.giveRandomWeapon();
                        mRespawnCounter = Utils.randFloatInRange(
                                RESPAWN_MIN_FRAME_COUNT,
                                RESPAWN_MAX_FRAME_COUNT);
                        break;
                    }
                }
            }
        }
    }

    public boolean isSpawned() {
        return mRespawnCounter == 0.0f;
    }


    public void draw(ShapeBuffer shapeBuffer) {
        if (isSpawned()) {
            shapeBuffer.add2DShape(mPositionX, mPositionY, mColor,
                    Utils.SQUARE_SHAPE, POWERUP_SCALE, POWERUP_SCALE,
                    mHeadingX, mHeadingY);
        }
    }


    public void pickNewLocation() {
        GameState gameState = GameState.getInstance();

        boolean validLocation = false;
        // Tries to find a location that is not within a wall or too close to one of the players.
        // Gives up if it can't find a valid location after 10 tries.
        for (int tries = 0; tries < MAX_POWERUP_PLACEMENT_ATTEMPTS; ++tries) {
            mPositionX = Utils.randFloatInRange(
                    GameState.MAP_LEFT_COORDINATE * USABLE_MAP_PERCENT,
                    GameState.MAP_RIGHT_COORDINATE * USABLE_MAP_PERCENT);
            mPositionY = Utils.randFloatInRange(
                    GameState.MAP_BOTTOM_COORDINATE * USABLE_MAP_PERCENT,
                    GameState.MAP_TOP_COORDINATE * USABLE_MAP_PERCENT);
            // Assume it's true, until it fails...
            validLocation = true;
            // Don't spawn in a wall.
            for (WallSegment wall : gameState.getWallList()) {
                if (wall.isInWall(mPositionX, mPositionY)) {
                    validLocation = false;
                    break;
                }
            }
            for (Spaceship player : gameState.getPlayerList()) {
                // Don't spawn too close to a player.
                if (player.isActive()) {
                    float distanceToPlayer = getDistanceToPoint(player.getPositionX(),
                            player.getPositionY());
                    if (distanceToPlayer < MIN_DISTANCE_TO_PLAYER) {
                        validLocation = false;
                        break;
                    }
                }
            }
        }

        if (!validLocation) {
            // Try again in a second.
            mRespawnCounter = GameState.secondsToFrameDelta(1.0f);
        }
    }

    /**
     * Convenience function to compute the distance between this powerup and the given point.
     */
    private float getDistanceToPoint(float x, float y) {
        return Utils.distanceBetweenPoints(mPositionX, mPositionY, x, y);
    }
}
