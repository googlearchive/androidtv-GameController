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

import com.google.fpl.gamecontroller.Utils;

/**
 * A particle system with up to 1000 particles.
 */
public class ParticleGlitter extends ParticleLayer {
    public ParticleGlitter() {
        super(1000, false);
    }

    public ParticleGlitter(boolean trackCollision) {
        super(1000, trackCollision);
    }

    public BaseParticle addParticle(float x, float y, float dx, float dy,
                                    Utils.Color color, float fuse, float size) {
        int slot = getNextOpenIndex();

        if (slot != -1) {
            mBackgroundSquareList[slot].x = x;
            mBackgroundSquareList[slot].y = y;
            mBackgroundSquareList[slot].deltaX = dx;
            mBackgroundSquareList[slot].deltaY = dy;
            mBackgroundSquareList[slot].color.set(color);
            mBackgroundSquareList[slot].fuse = fuse;
            mBackgroundSquareList[slot].size = size;
            mBackgroundSquareList[slot].particleType = BaseParticle.PARTICLETYPE_NORMAL;
            return mBackgroundSquareList[slot];
        }
        return null;
    }
}
