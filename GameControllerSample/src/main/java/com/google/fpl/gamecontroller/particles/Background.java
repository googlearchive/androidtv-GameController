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
 * Manages particles that makeup the background map.
 */
public class Background extends ParticleLayer {

    protected final Utils.Color mCurrentColor = new Utils.Color();
    protected final Utils.Color mOriginalColor = new Utils.Color();
    protected final Utils.Color mTargetColor = new Utils.Color();

    protected float transitionTotalTime = 0.0f;
    protected float transitionTimeSoFar = 0.0f;


    public Background() {
        super(1000, false);
        mCurrentColor.set(0.0f, 0.0f, 0.5f, 1.0f);
    }


    public void update(float timeFactor) {
        super.update(timeFactor);
        addSquare();

        for (int i = 0; i < mBackgroundSquareList.length; i++) {
            if (mBackgroundSquareList[i].isActive()) {
                mBackgroundSquareList[i].color.set(mCurrentColor);
            }
        }

        if (transitionTotalTime != 0) {
            transitionTimeSoFar += timeFactor;
            if (transitionTimeSoFar >= transitionTotalTime) {
                mCurrentColor.set(mTargetColor);
                transitionTimeSoFar = 0.0f;
                transitionTotalTime = 0.0f;
            } else {
                float transitionRatio = transitionTimeSoFar / transitionTotalTime;
                mCurrentColor.setToLerp(mTargetColor, mOriginalColor, transitionRatio);
            }
        }
    }

    public void addSquare() {
        int slot = getNextOpenIndex();
        if (slot != -1) {
            mBackgroundSquareList[slot].x = (float) (Math.random() * 400 - 200);
            mBackgroundSquareList[slot].y = (float) (Math.random() * -400);
            mBackgroundSquareList[slot].deltaX = 0;
            mBackgroundSquareList[slot].deltaY = (float) (Math.random() * 1 + 0.5);
            mBackgroundSquareList[slot].color.set(mCurrentColor);
            // Set initial alpha value to 0, so that the square starts invisible and fades
            // in over time.
            mBackgroundSquareList[slot].color.setAlpha(0.0f);
            mBackgroundSquareList[slot].fuse = (int) (Math.random() * 200 + 120);
            mBackgroundSquareList[slot].size = (float) (Math.random() * 25 + 5);
            mBackgroundSquareList[slot].dieOffscreen = false;
            mBackgroundSquareList[slot].maxAlpha = 0.25f;
        }
    }

    public void transitionColorTo(Utils.Color targetColor, float time) {
        mOriginalColor.set(mCurrentColor);
        mTargetColor.set(targetColor);
        transitionTotalTime = time;
        transitionTimeSoFar = 0.0f;
    }
}
