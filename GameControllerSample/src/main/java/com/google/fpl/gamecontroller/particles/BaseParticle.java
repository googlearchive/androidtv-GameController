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
import com.google.fpl.gamecontroller.Utils;

/**
 * Base class used to handle particle effects.
 */
public class BaseParticle {
    public static final int PARTICLETYPE_NORMAL = 0;
    public static final int PARTICLETYPE_ROCKET = 1;
    private static final float[] SQUARE_SHAPE = {
            1, 1,
            -1, 1,
            1, -1,
            -1, -1
    };
    public static final float FADE_TIME = 60f;
    public static final float FADE_DELTA = 1f / FADE_TIME;
    public float rotation = 0;
    public float deltaRotation = 0;
    public float x, y;
    public float deltaX, deltaY;
    public float size;
    public final Utils.Color color = new Utils.Color();
    public float maxAlpha = 1;
    public float aspectRatio = 1f;
    public int ownerIndex = -1;
    public boolean dieOffscreen = true;
    public float fuse = 0;
    public boolean rotateToHeading = false;
    public int particleType;

    private final Utils.Color mCurrentColor = new Utils.Color();

    public void update(float timeFactor) {
        rotation += deltaRotation;
        x += deltaX * timeFactor;
        y += deltaY * timeFactor;
        fuse -= timeFactor;

        float newAlpha = color.alpha();
        if (fuse < FADE_TIME) {
            newAlpha -= FADE_DELTA * timeFactor;
        } else {
            newAlpha += FADE_DELTA * timeFactor;
        }
        color.setAlpha(Utils.clamp(newAlpha, 0.0f, 1.0f));

        if (dieOffscreen) {
            if (Math.abs(x) > 250 || Math.abs(y) > 150) {
                fuse = 0;
            }
        }
        if (particleType == PARTICLETYPE_ROCKET) {
            handleRocketUpdate();
        }
    }

    protected void handleRocketUpdate() {
        if (Math.abs(x) > GameState.WORLD_WIDTH / 2 || Math.abs(y) > GameState.WORLD_HEIGHT / 2) {
            collision();
        }
        float currentSpeedSquared = deltaX * deltaX + deltaY * deltaY;
        if (currentSpeedSquared <= 6 * 6) {
            deltaX *= 1.05f;
            deltaY *= 1.05f;
        }

        BaseParticle mySquare = GameState.getInstance().getExplosions().addParticle(
                x - deltaX * 2,
                y - deltaY * 2,
                -deltaX / 2f + (float) Math.random() * 0.2f - 0.1f,
                -deltaY / 2f + (float) Math.random() * 0.2f - 0.1f,
                color,                           // color
                (float) Math.random() * 45 + 15, // fuse
                (float) Math.random() * 1 + 1    // size
        );
        if (mySquare != null) {
            mySquare.maxAlpha = 0.25f;
        }
    }

    public void draw(ShapeBuffer sb) {
        float hx = 0, hy = 0;
        if (rotateToHeading) {
            hx = deltaX;
            hy = deltaY;
        }

        mCurrentColor.set(color);
        mCurrentColor.setAlpha(mCurrentColor.alpha() * maxAlpha);
        sb.addShape(x, y, mCurrentColor, SQUARE_SHAPE, size * aspectRatio, size, hx, hy);
    }

    public void collision() {
        fuse = 0;
        if (particleType == PARTICLETYPE_ROCKET) {
            for (int i = 0; i < 100; i++) {
                float xx, yy;
                xx = Utils.randInRange(-1, 1);
                yy = Utils.randInRange(-1, 1);
                float mag = (float) Math.sqrt(xx * xx + yy * yy);
                if (mag == 0) {
                    mag = 1;
                    xx = 1;
                }
                float speed = Utils.randInRange(0.5f, 1.5f) / mag;

                BaseParticle myShot = GameState.getInstance().getShots().addParticle(
                        x, y,                        // position
                        xx * speed, yy * speed,      // dx/dy
                        color,                       // color
                        Utils.randIntInRange(5, 45), //fuse
                        0.75f                        //size
                );
                if (myShot != null) {
                    if (myShot != null) {
                        myShot.x += myShot.deltaX * 3;
                        myShot.y += myShot.deltaY * 3;
                        myShot.aspectRatio = 3f;
                        myShot.rotateToHeading = true;
                        myShot.ownerIndex = ownerIndex;
                    }
                }
            }
        } else {
            for (int i = 0; i < 5; i++) {
                float xx, yy;
                xx = Utils.randInRange(-1, 1);
                yy = Utils.randInRange(-1, 1);
                float mag = (float) Math.sqrt(xx * xx + yy * yy);
                if (mag == 0) {
                    mag = 1;
                    xx = 1;
                }
                float speed = Utils.randInRange(0.1f, 0.5f) / mag;

                BaseParticle mySquare = GameState.getInstance().getExplosions().addParticle(
                        x, y,                                  //position
                        1.5f * xx * speed, 1.5f * yy * speed,  //dx/dy
                        color,                                 //color
                        Utils.randInRange(10, 20),             //fuse
                        Utils.randInRange(0.5f, 2)             //size
                );
                if (mySquare != null) {
                    mySquare.maxAlpha = 0.25f;
                }
            }
        }
    }

    public boolean isActive() {
        return fuse > 0;
    }
}
