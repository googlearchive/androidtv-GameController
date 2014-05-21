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

import android.util.Log;


/**
 * Quick and dirty class for doing spatial partitioning to make collision tracking easier.
 * Assumes that the origin is in the center of the screen.
*/
public class CollisionGrid {
    protected static final int MAX_ENTITIES_PER_ZONE = 100;
    protected static final int MAX_RETURNED_VALUES = 4000;
    protected float mColumnWidth, mRowHeight;
    protected int mColumnMax, mRowMax;
    protected int mZoneCount;
    protected float mMaxObjectRadius;
    protected float mWorldWidth, mWorldHeight;
    protected BaseParticle[][] mZoneArray;
    protected int[] mZonePopulation;
    protected BaseParticle[] mReturnValues;

    public CollisionGrid(float width, float height, float zoneSize) {
        mWorldWidth = width;
        mWorldHeight = height;
        mColumnMax = (int) Math.ceil(width / zoneSize);
        mRowMax = (int) Math.ceil(height / zoneSize);
        mColumnWidth = zoneSize;
        mRowHeight = zoneSize;
        mZoneCount = mColumnMax * mRowMax;

        mZoneArray = new BaseParticle[mZoneCount][MAX_ENTITIES_PER_ZONE];
        mZonePopulation = new int[mZoneCount];

        mReturnValues = new BaseParticle[MAX_RETURNED_VALUES];
    }


    protected void addToZone(BaseParticle particle, int zone) {
        if (mZonePopulation[zone] < MAX_ENTITIES_PER_ZONE) {
            mZoneArray[zone][mZonePopulation[zone]] = particle;
            mZonePopulation[zone]++;
        } else {
            Log.w("GameControllerSample", "Ran out of space in zone " + zone + "/" + mZoneCount);
        }
    }

    protected void clearZone(int zone) {
        mZonePopulation[zone] = 0;
        // This next part is just to make sure garbage collection can happen.  Not that it should.
        for (int i = 0; i < MAX_ENTITIES_PER_ZONE; i++) {
            mZoneArray[zone][i] = null;
        }
    }

    public void clear() {
        for (int i = 0; i < mZoneCount; i++) {
            clearZone(i);
        }
    }

    public void addObject(BaseParticle o, float x, float y, float radius) {
        addObjectHelper(o, x + mWorldWidth / 2, y + mWorldHeight / 2);
    }

    protected void addObjectHelper(BaseParticle o, float x, float y) {
        int zone = getZoneOnGrid(x, y);
        if (zone != -1) {
            addToZone(o, zone);
        }
    }

    protected int getZoneOnGrid(float x, float y) {
        int gridX = (int) Math.floor(x / mColumnWidth);
        int gridY = (int) Math.floor(y / mRowHeight);

        if (gridX < 0) {
            gridX = 0;
        }
        if (gridY < 0) {
            gridY = 0;
        }
        if (gridX >= mColumnMax) {
            gridX = mColumnMax - 1;
        }
        if (gridY >= mRowMax) {
            gridY = mRowMax - 1;
        }
        return (gridX + gridY * mColumnMax);
    }

    public BaseParticle[] getRectPopulation(float x1, float y1, float x2, float y2) {
        int leftSlot, rightSlot, topSlot, bottomSlot;
        leftSlot = (int) Math.floor((x1 + mWorldWidth / 2) / mColumnWidth) - 1;
        if (leftSlot < 0) {
            leftSlot = 0;
        }
        rightSlot = (int) Math.floor((x2 + mWorldWidth / 2) / mColumnWidth) + 1;
        if (rightSlot >= mColumnMax) {
            rightSlot = mColumnMax - 1;
        }
        topSlot = (int) Math.floor((y1 + mWorldHeight / 2) / mRowHeight) - 1;
        if (topSlot < 0) {
            topSlot = 0;
        }
        bottomSlot = (int) Math.floor((y2 + mWorldHeight / 2) / mRowHeight) + 1;
        if (bottomSlot >= mRowMax) {
            bottomSlot = mRowMax - 1;
        }

        int returnedValueCount = 0;

        for (int x = leftSlot; x <= rightSlot; x++) {
            for (int y = topSlot; y <= bottomSlot; y++) {
                int currentZone = x + y * mColumnMax;
                for (int i = 0; i < mZonePopulation[currentZone]; i++) {
                    mReturnValues[returnedValueCount] = mZoneArray[currentZone][i];
                    returnedValueCount++;
                    if (returnedValueCount >= MAX_RETURNED_VALUES) {
                        Log.w("GameControllerSample", "Ran out of space in return array.");
                        break;
                    }
                }
            }
        }

        for (int i = returnedValueCount; i < MAX_RETURNED_VALUES; i++) {
            mReturnValues[i] = null;
        }

        return mReturnValues;
    }
}
