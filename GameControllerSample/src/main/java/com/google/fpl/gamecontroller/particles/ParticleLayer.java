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

package com.google.fpl.gamecontroller.particles;

import com.google.fpl.gamecontroller.GameState;
import com.google.fpl.gamecontroller.ShapeBuffer;

/**
 * Controls a particle system.
 */
public class ParticleLayer {
    public CollisionGrid collisionGrid = null;
    protected BaseParticle[] mBackgroundSquareList;
    protected int mParticleMax = 1000;
    protected int mLastOpenIndex = 0;

    public ParticleLayer(int particleMax, boolean generateCollisionGrid) {
        mParticleMax = particleMax;
        mBackgroundSquareList = new BaseParticle[mParticleMax];
        for (int i = 0; i < mParticleMax; i++) {
            mBackgroundSquareList[i] = new BaseParticle();
        }

        if (generateCollisionGrid) {
            collisionGrid = new CollisionGrid(GameState.WORLD_WIDTH, GameState.WORLD_HEIGHT, 10);
        }
    }


    public void update(float timeFactor) {
        for (int i = 0; i < mParticleMax; i++) {
            if (mBackgroundSquareList[i].isActive()) {
                mBackgroundSquareList[i].update(timeFactor);
            }
        }

        if (collisionGrid != null) {
            collisionGrid.clear();
            for (int i = 0; i < mParticleMax; i++) {
                BaseParticle square = mBackgroundSquareList[i];
                if (square.isActive()) {
                    collisionGrid.addObject(square, square.x, square.y, square.size);
                }
            }
        }
    }

    public void draw(ShapeBuffer sb) {
        for (int i = 0; i < mParticleMax; i++) {
            if (mBackgroundSquareList[i].isActive()) {
                mBackgroundSquareList[i].draw(sb);
            }
        }
    }

    protected int getNextOpenIndex() {
        for (int i = 0; i < mParticleMax; i++) {
            if (!mBackgroundSquareList[(mLastOpenIndex + i) % mParticleMax].isActive()) {
                mLastOpenIndex = (mLastOpenIndex + i) % mParticleMax;
                return mLastOpenIndex;
            }
        }
        return -1;
    }

    public BaseParticle checkForCollision(float x, float y, float radius) {
        if (collisionGrid == null) {
            return null;
        }
        BaseParticle[] possibleHits =
                collisionGrid.getRectPopulation(x - radius, y - radius, x + radius, y + radius);

        BaseParticle currentSquare;
        for (int i = 0; possibleHits[i] != null; i++) {
            currentSquare = possibleHits[i];
            float xx = x - currentSquare.x;
            float yy = y - currentSquare.y;
            if (xx * xx + yy * yy <= radius * radius) {
                return currentSquare;
            }
        }

        return null;
    }

    // Gives you back a list, so you can do better collision detection yourself.
    public BaseParticle[] getPotentialCollisions(float x, float y, float radius) {
        if (collisionGrid == null) {
            return null;
        }
        return collisionGrid.getRectPopulation(x - radius, y - radius, x + radius, y + radius);
    }

    // Gives you back a list, so you can do better collision detection yourself.
    public BaseParticle[] getPotentialCollisions(float x, float y, float width, float height) {
        if (collisionGrid == null) {
            return null;
        }
        return collisionGrid.getRectPopulation(x - width / 2, y - height / 2,
                x + width / 2, y + height / 2);
    }
}
