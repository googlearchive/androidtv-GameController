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
 * Handles positioning, control, and spawning of user-controlled space ships.
 */
public class Spaceship {

    private static final int WEAPON_BASEGUN = 0;
    private static final int WEAPON_MACHINEGUN = 1;
    private static final int WEAPON_SHOTGUN = 2;
    private static final int WEAPON_ARROWHEADS = 3;
    private static final int WEAPON_SCATTERGUN = 4;
    private static final int WEAPON_ROCKET = 5;
    private static final int WEAPON_COUNT = 6;
    private static final float[] SHIP_SHAPE = {
            -1, 0.5f,
            -1, -0.5f,
            1, 0
    };
    private static final float[] SQUARE_SHAPE = {
            1, 1,
            -1, 1,
            1, -1,
            -1, -1
    };
    public static final float CAMERA_SPEED = 0.5f;
    public static final float DRAG = 0.95f;
    public static final float SPEED = 1;
    public static final float BULLET_SPEED = 2.5f;
    public static final float SHIP_SIZE = 5f;
    public static final float RESPAWN_TIME = 120;
    private static final float INVINCIBILITY_TIME = 120;
    private static final float GUN_FIREDELAY_BASEGUN = 0.25f * 60f;
    private static final float GUN_FIREDELAY_SHOTGUN = 1f * 60f;
    private static final float GUN_FIREDELAY_MACHINEGUN = 2f;
    private static final float GUN_FIREDELAY_ARROWHEAD = 0.5f * 60f;
    private static final float GUN_FIREDELAY_ROCKET = 0.75f * 60f;
    private static final float GUN_FIREDELAY_SCATTERGUN = 8;

    public float x, y;
    public float cameraX, cameraY;
    public int shipIndex = -1;
    public int deviceId = -1;
    public boolean isActive = false;
    public int pointsEarned = 0;

    private GameState mGameState;

    private final Utils.Color mColor = new Utils.Color();
    private final Utils.Color mCurrentColor = new Utils.Color();
    private float deltaX, deltaY;
    private float headingX, headingY;
    private int mCurrentGun = WEAPON_BASEGUN;
    private float mRespawnTimer = 1;
    private float mInvincibilityTimer = 0;
    private float mFireCounter = 0;
    private float gunRechargeTime = 0;
    private GamepadController controller = null;

    public Spaceship(GameState gameState, Utils.Color color) {
        mGameState = gameState;
        headingX = 0;
        headingY = 1;
        mColor.set(color);
        setController(new GamepadController());
    }

    public Utils.Color getColor() {
        return mColor;
    }

    public void makeActiveIfNotActive() {
        isActive = true;
    }

    public void deactivateShip() {
        isActive = false;
        deviceId = -1;
    }

    public void update(float timeFactor) {
        // Waiting to respawn.
        if (mRespawnTimer > 0) {
            mRespawnTimer -= timeFactor;
            if (mRespawnTimer <= 0) {
                mRespawnTimer = 0;
                x = (float) (Math.random() * 200 - 100);
                y = (float) (Math.random() * 200 - 100);
                cameraX = x;
                cameraY = y;
                ringBurst(1, 1);
                mInvincibilityTimer = INVINCIBILITY_TIME;
                mCurrentGun = WEAPON_BASEGUN;
            } else {
                return;
            }
        }

        if (mInvincibilityTimer > 0) {
            mInvincibilityTimer -= timeFactor;
            if (mInvincibilityTimer < 0) {
                mInvincibilityTimer = 0;
            }
        }

        gunRechargeTime -= timeFactor;

        float newX = controller.getJoystickPosition(controller.JOYSTICK_1)[controller.AXIS_X];
        float newY = controller.getJoystickPosition(controller.JOYSTICK_1)[controller.AXIS_Y];

        float mag = (float) Math.sqrt(newX * newX + newY * newY);
        if (mag > 0.1) {
            headingX = newX / mag;
            headingY = -newY / mag;

            deltaX = newX * SPEED;
            deltaY = -newY * SPEED;

            if (mag > 1) {
                deltaX /= mag;
                deltaY /= mag;
            }

            BaseParticle mySquare = mGameState.mExplosions.addParticle(
                    x - headingX * 2, y - headingY * 2,                    // position
                    -headingX / 2f + (float) Math.random() * 0.2f - 0.1f,  // dx
                    -headingY / 2f + (float) Math.random() * 0.2f - 0.1f,  // dy
                    mColor,                                                // color
                    (float) Math.random() * 45 + 15,                       // fuse
                    (float) Math.random() * 1 + 1);                        // size
            if (mySquare != null) {
                mySquare.maxAlpha = 0.25f;
            }
        }

        x += deltaX * timeFactor;
        y += deltaY * timeFactor;
        deltaX *= DRAG;
        deltaY *= DRAG;
        if (Math.sqrt(deltaX * deltaX + deltaY * deltaY) < 0.05f) {
            deltaX = 0;
            deltaY = 0;
        }

        if (x > GameState.WORLD_WIDTH / 2) {
            x = GameState.WORLD_WIDTH / 2;
        }
        if (x < -GameState.WORLD_WIDTH / 2) {
            x = -GameState.WORLD_WIDTH / 2;
        }
        if (y > GameState.WORLD_HEIGHT / 2) {
            y = GameState.WORLD_HEIGHT / 2;
        }
        if (y < -GameState.WORLD_HEIGHT / 2) {
            y = -GameState.WORLD_HEIGHT / 2;
        }

        cameraX = x * (1f - CAMERA_SPEED) + cameraX * CAMERA_SPEED;
        cameraY = y * (1f - CAMERA_SPEED) + cameraY * CAMERA_SPEED;

        if (controller.wasButtonPressed(controller.BUTTON_Y)) {
            mGameState.mBackground.transitionColorTo(mColor, 60 * 3);
        }

        float aimX = controller.getJoystickPosition(controller.JOYSTICK_2)[controller.AXIS_X];
        float aimY = -controller.getJoystickPosition(controller.JOYSTICK_2)[controller.AXIS_Y];
        if (aimX * aimX + aimY * aimY > 0.15f && gunRechargeTime <= 0) {
            fireGun();
        }
        if (controller.isButtonDown(controller.BUTTON_X)
                || controller.isButtonDown(controller.BUTTON_Y)) {
        }

        checkCollisions();
    }

    public void giveRandomWeapon() {
        mCurrentGun = Utils.randIntInRange(1, WEAPON_COUNT - 1);
        ringBurst(1, 1);
    }

    protected void fireGun() {
        if (mCurrentGun == WEAPON_BASEGUN) {
            fireBullets(1, 0, 0, BULLET_SPEED, GUN_FIREDELAY_BASEGUN);
        } else if (mCurrentGun == WEAPON_ARROWHEADS) {
            fireBullets(2, 9, 0, BULLET_SPEED * 0.85f, GUN_FIREDELAY_ARROWHEAD);
            fireBullets(2, 6, 0, BULLET_SPEED * 0.90f, GUN_FIREDELAY_ARROWHEAD);
            fireBullets(2, 3, 0, BULLET_SPEED * 0.95f, GUN_FIREDELAY_ARROWHEAD);
            fireBullets(1, 0, 0, BULLET_SPEED * 1.0f, GUN_FIREDELAY_ARROWHEAD);
        } else if (mCurrentGun == WEAPON_SHOTGUN) {
            fireBullets(20, 20, 0, BULLET_SPEED, GUN_FIREDELAY_SHOTGUN);
        } else if (mCurrentGun == WEAPON_MACHINEGUN) {
            fireBullets(1, 0, 0, BULLET_SPEED, GUN_FIREDELAY_MACHINEGUN);
        } else if (mCurrentGun == WEAPON_SCATTERGUN) {
            fireBullets(1, 0, 0, BULLET_SPEED, GUN_FIREDELAY_SCATTERGUN);
            mFireCounter = (mFireCounter + 1) % 2;
            if (mFireCounter == 0) {
                fireBullets(2, 15, 0, BULLET_SPEED * 0.95f, GUN_FIREDELAY_SCATTERGUN);
            }
        } else if (mCurrentGun == WEAPON_ROCKET) {
            fireRocket();
        }
    }

    protected void fireRocket() {
        float aimX = controller.getJoystickPosition(controller.JOYSTICK_2)[controller.AXIS_X];
        float aimY = -controller.getJoystickPosition(controller.JOYSTICK_2)[controller.AXIS_Y];

        gunRechargeTime = GUN_FIREDELAY_ROCKET;

        float mag = (float) Math.sqrt(aimX * aimX + aimY * aimY);
        if (mag == 0) {
            mag = 1;
            aimX = 0;
            aimY = 1;
        }
        aimX /= mag;
        aimY /= mag;

        BaseParticle myShot = mGameState.mShots.addParticle(
                x, y,                        // position
                aimX * 2, aimY * 2,          // dx/dy
                mColor,                      // color
                500,                         // fuse
                0.75f);                      // size
        if (myShot != null) {
            myShot.x += myShot.deltaX * 3;
            myShot.y += myShot.deltaY * 3;
            myShot.size = 2;
            myShot.aspectRatio = 2f;
            myShot.rotateToHeading = true;
            myShot.ownerIndex = shipIndex;
            myShot.particleType = BaseParticle.PARTICLETYPE_ROCKET;
        }
    }

    protected void checkCollisions() {
        BaseParticle bullet = mGameState.mShots.checkForCollision(x, y, SHIP_SIZE);
        if (bullet != null && bullet.ownerIndex != shipIndex) {
            bullet.collision();
            if (!isInvincible()) {
                ringBurst(1, 1);
                ringBurst(0.5f, 2f);
                mRespawnTimer = RESPAWN_TIME;
                mGameState.scorePoint(bullet.ownerIndex);
                pointsEarned--;
                if (pointsEarned < 0) {
                    pointsEarned = 0;
                }
            }
        }
    }

    public void explodeForMatchEnd() {
        ringBurst(1, 1);
        ringBurst(0.5f, 2f);
        mRespawnTimer = 60 * 5;
        pointsEarned = 0;
    }

    protected void ringBurst(float minSpeed, float maxSpeed) {
        mGameState.ringBurst(x, y, mColor, minSpeed, maxSpeed, 100);
    }

    protected void fireBullets(int bulletCount, float spread, float angleOffset, float speed,
                               float recharge) {
        float aimX = controller.getJoystickPosition(controller.JOYSTICK_2)[controller.AXIS_X];
        float aimY = -controller.getJoystickPosition(controller.JOYSTICK_2)[controller.AXIS_Y];

        gunRechargeTime = recharge;

        float mag = (float) Math.sqrt(aimX * aimX + aimY * aimY);
        if (mag == 0) {
            mag = 1;
            aimX = 0;
            aimY = 1;
        }
        aimX /= mag;
        aimY /= mag;

        for (int i = 0; i < bulletCount; i++) {
            float a;
            if (bulletCount > 1) {
                a = -spread / 2f + (float) i * spread / ((float) bulletCount - 1f);
            } else {
                a = 0;
            }
            a += angleOffset;
            float angle = (float) (a * Math.PI / 180f);
            float angleSin = (float) Math.sin(angle);
            float angleCos = (float) Math.cos(angle);

            float shotDx = aimX * angleCos - aimY * angleSin;
            float shotDy = aimX * angleSin + aimY * angleCos;
            BaseParticle myShot = mGameState.mShots.addParticle(
                    x, y,                              // position
                    shotDx * speed, shotDy * speed,    // dx/dy
                    mColor,                            // color
                    500,                               // fuse
                    0.75f                              // size
            );
            if (myShot != null) {
                myShot.x += myShot.deltaX * 3;
                myShot.y += myShot.deltaY * 3;
                myShot.aspectRatio = 3f;
                myShot.rotateToHeading = true;
                myShot.ownerIndex = shipIndex;
            }
        }
    }

    protected void fireDeathCircle() {
        for (int i = 0; i < 500; i++) {
            float xx, yy;
            xx = Utils.randInRange(-1, 1);
            yy = Utils.randInRange(-1, 1);
            float mag = (float) Math.sqrt(xx * xx + yy * yy);
            if (mag == 0) {
                mag = 1;
                xx = 1;
            }
            float speed = 4f / mag;

            BaseParticle myShot = mGameState.mShots.addParticle(
                    x, y,                        // position
                    xx * speed, yy * speed,      // dx/dy
                    mColor,                      // color
                    200,                         // fuse
                    0.75f);                      // size
            if (myShot != null) {
                myShot.aspectRatio = 3f;
                myShot.rotateToHeading = true;
                myShot.ownerIndex = shipIndex;
            }
        }
    }

    public GamepadController getController() {
        return controller;
    }

    public void setController(GamepadController newController) {
        controller = newController;
        pointsEarned = 0;
    }

    public boolean isSpawned() {
        return (mRespawnTimer <= 0);
    }

    public boolean isInvincible() {
        return (mInvincibilityTimer > 0);
    }

    public void draw(ShapeBuffer sb) {
        if (!isSpawned()) {
            // No drawing if we're not alive yet.
            return;
        }

        mCurrentColor.set(mColor);

        if (isInvincible() && ((int) (mInvincibilityTimer / 10) % 2 == 0)) {
            mCurrentColor.scale(0.3f, 0.3f, 0.3f, 1.0f);
        }
        sb.addShape(x, y, mCurrentColor, SHIP_SHAPE, SHIP_SIZE, SHIP_SIZE, headingX, headingY);

        if (pointsEarned >= 1) {
            sb.addShape(x - 5, y + 5, mColor, SQUARE_SHAPE, 1, 1, 0, 1);
        }
        if (pointsEarned >= 2) {
            sb.addShape(x + 5, y + 5, mColor, SQUARE_SHAPE, 1, 1, 0, 1);
        }
        if (pointsEarned >= 3) {
            sb.addShape(x - 5, y - 5, mColor, SQUARE_SHAPE, 1, 1, 0, 1);
        }
        if (pointsEarned >= 4) {
            sb.addShape(x + 5, y - 5, mColor, SQUARE_SHAPE, 1, 1, 0, 1);
        }
    }
}
