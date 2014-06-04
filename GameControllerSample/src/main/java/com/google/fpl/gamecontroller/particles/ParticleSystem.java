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

import android.graphics.PointF;

import com.google.fpl.gamecontroller.GameState;
import com.google.fpl.gamecontroller.ShapeBuffer;
import com.google.fpl.gamecontroller.Utils;

/**
 * Manages a group of particles.
 */
public class ParticleSystem {
    // Constants used to create the "ring burst" effect.
    private static final float RING_BURST_INITIAL_POSITION_INCREMENT = 0.0f;
    private static final float RING_BURST_MAX_ALPHA = 0.25f;
    private static final float RING_BURST_MIN_SIZE = 0.5f;
    private static final float RING_BURST_MAX_SIZE = 2.0f;
    private static final float RING_BURST_ASPECT_RATIO = 1.0f;
    private static final float RING_BURST_MIN_LIFETIME = 0.25f;
    private static final float RING_BURST_MAX_LIFETIME = 0.75f;
    private static final int RING_BURST_OWNER_ID = GameState.INVALID_PLAYER_ID;

    // Constants used to create the shrapnel effect.
    private static final float SHRAPNEL_INITIAL_POSITION_INCREMENT = 3.0f;
    private static final float SHRAPNEL_MAX_ALPHA = 1.0f;
    private static final float SHRAPNEL_MIN_SIZE = 0.75f;
    private static final float SHRAPNEL_MAX_SIZE = 0.75f;
    private static final float SHRAPNEL_ASPECT_RATIO = 4.0f;
    private static final float SHRAPNEL_MIN_LIFETIME = 0.08f;
    private static final float SHRAPNEL_MAX_LIFETIME = 0.75f;

    // Constants used to create exhaust particles.
    private static final float EXHAUST_TRAIL_MIN_LIFETIME = 0.25f;
    private static final float EXHAUST_TRAIL_MAX_LIFETIME = 1.0f;
    private static final float EXHAUST_TRAIL_SOURCE_VELOCITY_SCALE = -0.5f;
    private static final float EXHAUST_TRAIL_VELOCITY_VARIANCE = 0.1f;
    private static final float EXHAUST_TRAIL_SOURCE_OFFSET_STEPS = 2.0f;
    private static final float EXHAUST_TRAIL_MIN_SIZE = 1.0f;
    private static final float EXHAUST_TRAIL_MAX_SIZE = 2.0f;
    private static final float EXHAUST_TRAIL_MAX_ALPHA = 0.25f;

    private static final int COLLISION_GRID_ZONE_SIZE = 10;

    protected final BaseParticle[] mParticles;
    protected final ParticleCollisionGrid mCollisionGrid;
    protected int mLastOpenIndex = 0;

    /**
     * Constructs a new particle system.
     *
     * @param maxActiveParticles the most particles that will ever be active at once.
     * @param generateCollisionGrid - true to create a ParticleCollisionGrid structure
     *                              for intersection and proximity queries.
     */
    public ParticleSystem(int maxActiveParticles, boolean generateCollisionGrid) {
        mParticles = new BaseParticle[maxActiveParticles];
        for (int i = 0; i < mParticles.length; i++) {
            mParticles[i] = new BaseParticle();
        }

        if (generateCollisionGrid) {
            mCollisionGrid = new ParticleCollisionGrid(
                    GameState.WORLD_WIDTH,
                    GameState.WORLD_HEIGHT,
                    COLLISION_GRID_ZONE_SIZE);
        } else {
            mCollisionGrid = null;
        }
    }

    /**
     * Updates all the particles in the system.
     *
     * @param frameDelta the number of frames that have elapsed since the last update.
     */
    public void update(float frameDelta) {
        if (mCollisionGrid != null) {
            mCollisionGrid.clear();
            for (BaseParticle particle : mParticles) {
                particle.update(frameDelta);
                if (particle.isActive()) {
                    mCollisionGrid.addParticle(particle);
                }
            }
        } else {
            for (BaseParticle particle : mParticles) {
                particle.update(frameDelta);
            }
        }
    }

    /**
     * Draws the system to the given shape buffer.
     */
    public void draw(ShapeBuffer sb) {
        for (BaseParticle particle : mParticles) {
            if (particle.isActive()) {
                particle.draw(sb);
            }
        }
    }

    /**
     * Spawns a new particle.
     *
     * @param lifetimeInSeconds the number of seconds this particle will be active.
     * @return the new particle, or null if too many particles have already been spawned.
     */
    public BaseParticle spawnParticle(float lifetimeInSeconds) {
        int slot = getNextOpenIndex();

        if (slot != -1) {
            mParticles[slot].reset(GameState.secondsToFrameDelta(lifetimeInSeconds));
            return mParticles[slot];
        }
        return null;
    }

    /**
     * Spawns a ring of particles around the given point.
     *
     * Useful for smoke and other effects that don't interact with the players' ships.
     *
     * @param centerX x center of the ring.
     * @param centerY y center of the ring.
     * @param color color of the ring particles.
     * @param minSpeed minimum particle speed.
     * @param maxSpeed maximum particle speed.
     * @param particleCount number of particles to create.
     */
    public void spawnRingBurst(float centerX, float centerY, Utils.Color color, float minSpeed,
                               float maxSpeed, int particleCount) {
        spawnGroupFromPoint(
                centerX, centerY, RING_BURST_INITIAL_POSITION_INCREMENT,
                color, RING_BURST_MAX_ALPHA,
                minSpeed, maxSpeed,
                RING_BURST_MIN_SIZE, RING_BURST_MAX_SIZE, RING_BURST_ASPECT_RATIO,
                RING_BURST_MIN_LIFETIME, RING_BURST_MAX_LIFETIME,
                RING_BURST_OWNER_ID,
                particleCount);
    }

    /**
     * Creates an explosion centered around the given point.
     *
     * Useful for creating fragments that can potentially collide with players' ships.
     *
     * @param centerX x center of the explosion.
     * @param centerY y center of the explosion.
     * @param color color of explosion particles.
     * @param minSpeed minimum particle speed.
     * @param maxSpeed maximum particle speed.
     * @param ownerId the owner of the new particles.
     * @param particleCount the number of particles to create.
     */
    public void spawnShrapnelExplosion(float centerX, float centerY, Utils.Color color,
                                       float minSpeed, float maxSpeed, int ownerId,
                                       int particleCount) {
        spawnGroupFromPoint(centerX, centerY, SHRAPNEL_INITIAL_POSITION_INCREMENT,
                color, SHRAPNEL_MAX_ALPHA,
                minSpeed, maxSpeed,
                SHRAPNEL_MIN_SIZE, SHRAPNEL_MAX_SIZE, SHRAPNEL_ASPECT_RATIO,
                SHRAPNEL_MIN_LIFETIME, SHRAPNEL_MAX_LIFETIME,
                ownerId,
                particleCount);
    }

    /**
     * Creates a trail of particles behind the given source point.
     *
     * Useful for creating ship or rocket exhaust.
     *
     * @param sourceX x source of the exhaust.
     * @param sourceY y source of the exhaust.
     * @param sourceVelocityX the x velocity of the object creating exhaust.
     * @param sourceVelocityY the y velocity of the object creating exhaust.
     * @param color the color of the new particles.
     * @param particleCount the number of particles to create.
     */
    public void spawnExhaustTrail(float sourceX, float sourceY, float sourceVelocityX,
                                  float sourceVelocityY, Utils.Color color, int particleCount) {
        for (int i = 0; i < particleCount; ++i) {
            float lifetime = Utils.randFloatInRange(
                    EXHAUST_TRAIL_MIN_LIFETIME,
                    EXHAUST_TRAIL_MAX_LIFETIME);
            BaseParticle trailParticle = spawnParticle(lifetime);
            if (trailParticle != null) {
                float velocityX = sourceVelocityX * EXHAUST_TRAIL_SOURCE_VELOCITY_SCALE;
                velocityX += Utils.randFloatInRange(-EXHAUST_TRAIL_VELOCITY_VARIANCE,
                                                    EXHAUST_TRAIL_VELOCITY_VARIANCE);

                float velocityY = sourceVelocityY * EXHAUST_TRAIL_SOURCE_VELOCITY_SCALE;
                velocityY += Utils.randFloatInRange(-EXHAUST_TRAIL_VELOCITY_VARIANCE,
                                                    EXHAUST_TRAIL_VELOCITY_VARIANCE);

                trailParticle.setPosition(
                        sourceX - sourceVelocityX * EXHAUST_TRAIL_SOURCE_OFFSET_STEPS,
                        sourceY - sourceVelocityY * EXHAUST_TRAIL_SOURCE_OFFSET_STEPS);
                trailParticle.setSpeed(velocityX, velocityY);
                trailParticle.setColor(color);
                trailParticle.setSize(
                        Utils.randFloatInRange(EXHAUST_TRAIL_MIN_SIZE, EXHAUST_TRAIL_MAX_SIZE));
                trailParticle.setMaxAlpha(EXHAUST_TRAIL_MAX_ALPHA);
            }
        }
    }

    /**
     * Treats the particle array as a circular list, and returns the next index after the given one.
     */
    private int getNextIndex(int i) {
        return (i + 1) % mParticles.length;
    }

    /**
     * Returns the next available particle index, or -1 if all slots are in use.
     */
    protected int getNextOpenIndex() {
        for (int i = getNextIndex(mLastOpenIndex); i != mLastOpenIndex; i = getNextIndex(i)) {
            if (!mParticles[i].isActive()) {
                mLastOpenIndex = i;
                return mLastOpenIndex;
            }
        }
        Utils.logDebug("Too many active particles: " + this.toString());
        return -1;
    }

    /**
     * Returns the first particle that lies within the given circle.
     *
     * If more than one particle are in the circle, only one is returned.  The returned
     * particle is not necessarily the one closest to the center of the circle.
     *
     * Returns null if no particle is in the given circle or if this system does not have
     * collision detection enabled.
     *
     * @param x x center of the circle.
     * @param y y center of the circle.
     * @param radius the radius of the circle to test.
     * @return the first particle that lies within the given circle.
     */
    public BaseParticle checkForCollision(float x, float y, float radius) {
        if (mCollisionGrid == null) {
            return null;
        }
        // Get the list of all possible hits.
        BaseParticle[] possibleHits =
                mCollisionGrid.getRectPopulation(x - radius, y - radius, x + radius, y + radius);

        BaseParticle currentParticle;
        final float radiusSquared = radius * radius;
        // Look for the first particle that meets our criteria (within the given circle).
        for (int i = 0; possibleHits[i] != null; i++) {
            currentParticle = possibleHits[i];
            float xx = x - currentParticle.getPositionX();
            float yy = y - currentParticle.getPositionY();
            if (Utils.vector2DLengthSquared(xx, yy) <= radiusSquared) {
                return currentParticle;
            }
        }

        return null;
    }

    /**
     * Returns a list of particles that might fall within the given rectangle.
     *
     * The list of particles returned is a super-set of the particles in the given
     * rectangle.  Finer-grained checking is needed to know exactly which particles are
     * in the rectangle.
     *
     * @param x the center of the rectangle.
     * @param y the center of the rectangle.
     * @param width the width of the rectangle.
     * @param height the height of the rectangle.
     * @return All the particles that may be within the given rectangle.
     */
    public BaseParticle[] getPotentialCollisions(float x, float y, float width, float height) {
        if (mCollisionGrid == null) {
            return null;
        }
        final float left = x - width / 2.0f;
        final float right = x + width / 2.0f;
        final float bottom = y - height / 2.0f;
        final float top = y + height / 2.0f;
        return mCollisionGrid.getRectPopulation(left, bottom, right, top);
    }

    /**
     * Helper function for spawning a group of particles at or near a given point.
     *
     * @param centerX x center of the group.
     * @param centerY y center of the group.
     * @param initialPositionIncrement the number of frame increments to move the particle
     *                                 from its initial position.
     * @param color color of the particles.
     * @param maxAlpha maximum alpha value for new particles.
     * @param minSpeed minimum particle speed.
     * @param maxSpeed maximum particle speed.
     * @param minSize minimum particle size.
     * @param maxSize maximum particle size.
     * @param aspectRatio aspect ration for new particles.
     * @param minLifetime minimum lifetime for new particles.
     * @param maxLifetime maximum lifetime for new particles.
     * @param ownerId the owner of the new particles.
     * @param particleCount the number of particles to create.
     */
    private void spawnGroupFromPoint(float centerX, float centerY, float initialPositionIncrement,
                                     Utils.Color color, float maxAlpha,
                                     float minSpeed, float maxSpeed,
                                     float minSize, float maxSize, float aspectRatio,
                                     float minLifetime, float maxLifetime,
                                     int ownerId,
                                     int particleCount) {
        for (int i = 0; i < particleCount; i++) {
            PointF direction = Utils.randDirectionVector();
            float speed = Utils.randFloatInRange(minSpeed, maxSpeed);
            float size = Utils.randFloatInRange(minSize, maxSize);
            float lifetime = Utils.randFloatInRange(minLifetime, maxLifetime);

            BaseParticle particle = spawnParticle(lifetime);
            if (particle != null) {
                particle.setPosition(centerX, centerY);
                particle.setSpeed(direction.x * speed, direction.y * speed);
                particle.setColor(color);
                particle.setMaxAlpha(maxAlpha);
                particle.setSize(size);
                particle.setAspectRatio(aspectRatio);
                particle.setOwnerId(ownerId);

                // Potentially offset the particle so it does not start exactly on the
                // source location.
                particle.incrementPosition(initialPositionIncrement);
            }
        }
    }
}
