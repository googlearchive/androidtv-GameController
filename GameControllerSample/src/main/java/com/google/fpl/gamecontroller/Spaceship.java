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
 * Handles positioning, control, and spawning of user-controlled space ships.
 */
public class Spaceship {

    // The types of weapons available.
    private static final int WEAPON_BASEGUN = 0;
    private static final int WEAPON_MACHINEGUN = 1;
    private static final int WEAPON_SHOTGUN = 2;
    private static final int WEAPON_ARROWHEADS = 3;
    private static final int WEAPON_SCATTERGUN = 4;
    private static final int WEAPON_ROCKET = 5;
    private static final int WEAPON_COUNT = 6;

    // The 2D vertex positions used to render the ship.
    // The ship is shaped as a single triangle, with a total width of 2 units along the x-axis
    // and a height of 1 unit along the y-axis.
    private static final float[] SHIP_SHAPE = {
            -1.0f,  0.5f,
            -1.0f, -0.5f,
            1.0f,   0.0f
    };
    // Scales the above vertices into world-space units.
    private static final float SHIP_SIZE = 5.0f;

    // When the motion controller is released, the ship coasts to a stop.
    // The drag is expressed as a percentage of the ship's current velocity, so 0.05 drag
    // means that the ship will get 5% shower each frame.
    private static final float DRAG = 0.05f;

    // To prevent constant movement, the ship will stop moving if it's velocity drops below
    // this minimum velocity.
    private static final float MINIMUM_VELOCITY = 0.05f;

    // The minimum distance a joystick must move before it is considered to have moved.
    private static final float JOYSTICK_MOVEMENT_THRESHOLD = 0.1f;

    // The amount of time to wait after a ship has been destroyed before the ship
    // is spawned again.
    private static final float RESPAWN_FRAME_COUNT = GameState.millisToFrameDelta(2000);
    // When a ship spawns, it can not be killed while it is invincible.
    private static final float INVINCIBILITY_FRAME_COUNT = GameState.millisToFrameDelta(2000);
    // The amount of time to wait after the end of a match before starting a new match.
    private static final float NEW_MATCH_RESPAWN_FRAME_COUNT = GameState.millisToFrameDelta(6000);

    // The speed at which the various weapons can fire.
    private static final float GUN_FIREDELAY_BASEGUN = GameState.millisToFrameDelta(250);
    private static final float GUN_FIREDELAY_SHOTGUN = GameState.millisToFrameDelta(1000);
    private static final float GUN_FIREDELAY_MACHINEGUN = GameState.millisToFrameDelta(33);
    private static final float GUN_FIREDELAY_ARROWHEAD = GameState.millisToFrameDelta(500);
    private static final float GUN_FIREDELAY_ROCKET = GameState.millisToFrameDelta(750);
    private static final float GUN_FIREDELAY_SCATTERGUN = GameState.millisToFrameDelta(133);

    // Speeds for the various types of bullets.  All speeds are in world-space units per frame.
    private static final float BULLET_SPEED_BASEGUN = 2.5f;
    private static final float BULLET_SPEED_MACHINEGUN = BULLET_SPEED_BASEGUN;
    private static final float BULLET_SPEED_SHOTGUN = BULLET_SPEED_BASEGUN;
    private static final float BULLET_SPEED_ARROWHEAD_CENTER = BULLET_SPEED_BASEGUN;
    private static final float BULLET_SPEED_SCATTERGUN = BULLET_SPEED_BASEGUN;
    private static final float BULLET_SPEED_SCATTERGUN_SECONDARY = 0.95f * BULLET_SPEED_SCATTERGUN;
    private static final float BULLET_SPEED_ROCKET = 2.0f;

    // The shotgun fires a volley of bullets along an arc.
    private static final int SHOTGUN_BULLET_COUNT = 20;
    private static final float SHOTGUN_BULLET_SPREAD_ARC_DEGREES = 20.0f;

    // Every other burst from the scatter gun fires a secondary set of bullets offset
    // from the central aiming direction.
    private static final int SCATTERGUN_SECONDARY_BULLET_COUNT = 2;
    private static final float SCATTERGUN_SECONDARY_BULLET_SPREAD_ARC_DEGREES = 15.0f;

    // The arrowhead weapon fires a triangular shaped volley of bullets.  Each step
    // behind the center of the arrow has two bullets, one on either side of the center.
    private static final int ARROWHEAD_STEP_COUNT = 3;
    private static final int ARROWHEAD_STEP_BULLET_COUNT = 2;
    // Each step behind the central bullet travels slower than the one ahead of it.
    private static final float ARROWHEAD_STEP_SPEED_DECREMENT = 0.05f;
    // Each step behind the central bullet is offset further from the center.
    private static final float ARROWHEAD_STEP_SPREAD_INCREMENT = 3.0f;

    // The maximum lifetime for bullet particles.  This is longer than it takes any bullet
    // to travel across the screen, so they will always hit something before timeing out.
    private static final float BULLET_LIFETIME_IN_SECONDS = 8.0f;

    // Attributes for bullet particles.
    private static final float BULLET_PARTICLE_SIZE = 0.75f;
    private static final float BULLET_PARTICLE_ASPECT_RATIO = 3.0f;
    private static final float BULLET_PARTICLE_INITIAL_POSITION_INCREMENT = 3.0f;

    // Attributes for rocket particles.
    private static final float ROCKET_PARTICLE_SIZE = 2.0f;
    private static final float ROCKET_PARTICLE_ASPECT_RATIO = 2.0f;
    private static final float ROCKET_PARTICLE_INITIAL_POSITION_INCREMENT = 3.0f;

    // The number of particles to spawn when creating a ring-burst around the ship.
    private static final int RINGBURST_PARTICLE_COUNT = 100;

    // Primary ring bursts have particles all moving at the same speed.
    private static final float RINGBURST_PRIMARY_MIN_SPEED = 1.5f;
    private static final float RINGBURST_PRIMARY_MAX_SPEED = 1.5f;

    // The secondary ring burst has particles moving at different speeds.
    private static final float RINGBURST_SECONDARY_MIN_SPEED = 0.75f;
    private static final float RINGBURST_SECONDARY_MAX_SPEED = 3.0f;

    // When a ship is invincible, it will flash a darker color.
    private static final float INVINCIBILITY_COLOR_DARKEN_FACTOR = 0.3f;
    // The number of frames between alternate colors while in invincible mode.
    private static final float INVINCIBILITY_COLOR_BLINK_RATE = GameState.millisToFrameDelta(200);

    private GameState mGameState;

    private float mPositionX, mPositionY;
    private int mPlayerId = GameState.INVALID_PLAYER_ID;
    private int mScore = 0;

    // The permanent color of the ship.
    private final Utils.Color mColor = new Utils.Color();
    // The current color is updated each frame.  It is the same as the permanent color,
    // unless the ship is invincible.
    private final Utils.Color mCurrentColor = new Utils.Color();
    // Handles input events for this ship.
    private final GamepadController mController = new GamepadController();
    // The vector describing this ship's speed and direction.  If the ship is not moving,
    // it's velocity will be 0, but it's heading will point in the direction it was last moving.
    private float mVelocityX, mVelocityY;
    // The normalized direction of this ship.
    private float mHeadingX, mHeadingY;
    // The normalized direction vector along which bullets are fired.
    private float mAimX, mAimY;
    // If true, the aim direction was set by the secondary joystick.
    private boolean mJoystickAiming;
    // One of the available weapons.
    private int mCurrentGun;
    // The number of frames to wait before spawing this ship again.  0 when the ship is spawned.
    private float mRespawnTimer;
    // The number of frames this ship will remain invincible.
    private float mInvincibilityTimer;
    // The number of frames to wait before the gun can fire again.
    private float mGunRechargeTimer;

    // Used by the SCATTERGUN to determine how many shots to fire (every other shot fires
    // additional bullets).
    private int mFireCounter;

    public Spaceship(GameState gameState, int playerId, Utils.Color color) {
        resetPlayer();
        this.mGameState = gameState;
        this.mHeadingX = 0.0f;
        this.mHeadingY = 1.0f;
        this.mColor.set(color);
        this.mPlayerId = playerId;

        // Set the respawn timer to something non-zero, so that a respawn event will trigger
        // in the next update.
        mRespawnTimer = 1.0f;
    }

    public float getPositionX() {
        return mPositionX;
    }
    public void setPositionX(float positionX) {
        this.mPositionX = Utils.clamp(positionX, GameState.MAP_LEFT_COORDINATE,
                GameState.MAP_RIGHT_COORDINATE);
    }
    public float getPositionY() {
        return mPositionY;
    }
    public void setPositionY(float positionY) {
        this.mPositionY = Utils.clamp(positionY, GameState.MAP_BOTTOM_COORDINATE,
                GameState.MAP_TOP_COORDINATE);
    }
    public int getPlayerId() {
        return mPlayerId;
    }
    public boolean isActive() {
        return mController.isActive();
    }
    public int getScore() {
        return mScore;
    }
    public void changeScore(int pointDelta) {
        this.mScore = Math.max(0, mScore + pointDelta);
    }

    /**
     * Sets the player's score, weapon, etc. back to their default state.
     */
    private void resetPlayer() {
        mScore = 0;

        mCurrentGun = WEAPON_BASEGUN;
        mRespawnTimer = 0.0f;
        mInvincibilityTimer = 0.0f;
        mFireCounter = 0;
        mGunRechargeTimer = 0.0f;
    }

    public Utils.Color getColor() {
        return mColor;
    }

    public void deactivateShip() {
        mController.setDeviceId(-1);
    }

    public void update(float frameDelta) {
        if (!updateStatus(frameDelta)) {
            // The ship is not active, so bail out now.
            return;
        }

        updateShipPosition(frameDelta);

        handleKeyPressesAndFiring(frameDelta);

        checkBulletCollisions();

        // Tell the controller to start a new frame.  This needs to be done after we're done
        // reading the controller's state for this frame.
        mController.advanceFrame();
    }

    /**
     * Sets the current weapon to one of the power-up weapons (i.e. the new weapon will be any
     * of the weapons except WEAPON_BASEGUN).
     */
    public void giveRandomWeapon() {
        mCurrentGun = Utils.randIntInRange(1, WEAPON_COUNT);
        spawnRingBurstAroundShip(RINGBURST_PRIMARY_MIN_SPEED, RINGBURST_PRIMARY_MAX_SPEED);
    }

    /**
     * Prepares a player for the start of a new match.
     *
     * @param winningPlayerId the Id of the winning player.
     */
    public void resetAtEndOfMatch(int winningPlayerId) {
        resetPlayer();
        if (isActive()) {
            if (mPlayerId != winningPlayerId) {
                // Explode the losing ships.
                spawnRingBurstAroundShip(
                        RINGBURST_PRIMARY_MIN_SPEED,
                        RINGBURST_PRIMARY_MAX_SPEED);
                spawnRingBurstAroundShip(
                        RINGBURST_SECONDARY_MIN_SPEED,
                        RINGBURST_SECONDARY_MAX_SPEED);
            }
            // Wait before starting next match.
            mRespawnTimer = NEW_MATCH_RESPAWN_FRAME_COUNT;
        }
    }

    public GamepadController getController() {
        return mController;
    }

    public boolean isSpawned() {
        return (mRespawnTimer <= 0);
    }

    public boolean isInvincible() {
        return (mInvincibilityTimer > 0);
    }

    /**
     * Draws the player's ship and 0 to 4 to indicate the player's score.
     */
    public void draw(ShapeBuffer sb) {
        if (!isSpawned()) {
            // No drawing if we're not alive yet.
            return;
        }

        sb.add2DShape(mPositionX, mPositionY, mCurrentColor, SHIP_SHAPE, SHIP_SIZE, SHIP_SIZE,
                mHeadingX, mHeadingY);

        // Draw squares around the ship to indicate the score.
        for (int i = 0; i < Math.min(mScore, 4); ++i) {
            // Places the dots at the edges of a square the same size as the ship.
            sb.add2DShape(
                    mPositionX + SHIP_SIZE * Utils.SQUARE_SHAPE[i * 2 + 0],
                    mPositionY + SHIP_SIZE * Utils.SQUARE_SHAPE[i * 2 + 1],
                    mColor, Utils.SQUARE_SHAPE, 1.0f, 1.0f, 0.0f, 1.0f);
        }
        if (mScore > 4) {
            // TODO: Implement a method to display more than 4 points per player.
            // For example, space the dots at equal intervals on a circle around the ship.
            Utils.logDebug("Scores higher than 4 are not displayed.");
        }
    }

    /**
     * Checks the aiming joystick position and computes the player's aim direction.
     */
    protected void calculateAimDirection() {
        mAimX = mController.getJoystickPosition(GamepadController.JOYSTICK_2,
                GamepadController.AXIS_X);
        mAimY = -mController.getJoystickPosition(GamepadController.JOYSTICK_2,
                GamepadController.AXIS_Y);
        float magnitude = Utils.vector2DLength(mAimX, mAimY);

        if (magnitude > JOYSTICK_MOVEMENT_THRESHOLD) {
            // Normalize the direction vector.
            mAimX /= magnitude;
            mAimY /= magnitude;
            mJoystickAiming = true;
        } else {
            // The firing joystick is not being used, so fire any shots in the direction
            // the player is currently traveling.
            mAimX = mHeadingX;
            mAimY = mHeadingY;
            mJoystickAiming = false;
        }
    }

    /**
     * Fires one burst of the current weapon.
     */
    protected void fireGun() {
        switch (mCurrentGun) {
            case WEAPON_BASEGUN:
                // Single bullet straight ahead.
                fireBullets(1, 0, BULLET_SPEED_BASEGUN, GUN_FIREDELAY_BASEGUN);
                break;
            case WEAPON_ARROWHEADS:
                // The center bullet of the arrowhead.
                fireBullets(1, 0, BULLET_SPEED_ARROWHEAD_CENTER, GUN_FIREDELAY_ARROWHEAD);

                // Fire the bullets that make up the steps behind the center bullet of the
                // arrowhead.
                for (int i = 1; i <= ARROWHEAD_STEP_COUNT; ++i) {
                    // The bullets farther from the center go slower.
                    float speedScale = 1.0f - i * ARROWHEAD_STEP_SPEED_DECREMENT;
                    // Each step in the arrowhead has the bullets spread farther apart.
                    float spread = i * ARROWHEAD_STEP_SPREAD_INCREMENT;
                    fireBullets(
                            ARROWHEAD_STEP_BULLET_COUNT,
                            spread,
                            speedScale * BULLET_SPEED_ARROWHEAD_CENTER,
                            GUN_FIREDELAY_ARROWHEAD);
                }
                break;
            case WEAPON_SHOTGUN:
                // The shotgun fires a volley of bullets along an arc.
                fireBullets(
                        SHOTGUN_BULLET_COUNT,
                        SHOTGUN_BULLET_SPREAD_ARC_DEGREES,
                        BULLET_SPEED_SHOTGUN,
                        GUN_FIREDELAY_SHOTGUN);
                break;
            case WEAPON_MACHINEGUN:
                // Fire a single bullet straight ahead.
                fireBullets(1, 0, BULLET_SPEED_MACHINEGUN, GUN_FIREDELAY_MACHINEGUN);
                break;
            case WEAPON_SCATTERGUN:
                // Fire the first bullet straight ahead.
                fireBullets(1, 0, BULLET_SPEED_SCATTERGUN, GUN_FIREDELAY_SCATTERGUN);
                mFireCounter = (mFireCounter + 1) % 2;
                if (mFireCounter == 0) {
                    // Every other burst from the scatter gun will have 2 extra bullets.
                    fireBullets(
                            SCATTERGUN_SECONDARY_BULLET_COUNT,
                            SCATTERGUN_SECONDARY_BULLET_SPREAD_ARC_DEGREES,
                            BULLET_SPEED_SCATTERGUN_SECONDARY,
                            GUN_FIREDELAY_SCATTERGUN);
                }
                break;
            case WEAPON_ROCKET:
                fireRocket();
                break;
            default:
                Utils.logDebug("Unhandled weapon type: " + mCurrentGun);
                break;
        }
    }

    /**
     * Creates a rocket "particle".
     */
    protected void fireRocket() {
        mGunRechargeTimer = GUN_FIREDELAY_ROCKET;

        BaseParticle myShot = mGameState.getShots().spawnParticle(BULLET_LIFETIME_IN_SECONDS);
        if (myShot != null) {
            myShot.setPosition(mPositionX, mPositionY);
            myShot.setSpeed(mAimX * BULLET_SPEED_ROCKET, mAimY * BULLET_SPEED_ROCKET);
            myShot.setColor(mColor);
            myShot.setSize(ROCKET_PARTICLE_SIZE);
            myShot.setAspectRatio(ROCKET_PARTICLE_ASPECT_RATIO);
            myShot.setOwnerId(mPlayerId);
            myShot.setParticleType(BaseParticle.PARTICLE_TYPE_ROCKET);

            // Offset the rocket's starting position a few steps ahead of our position.
            myShot.incrementPosition(ROCKET_PARTICLE_INITIAL_POSITION_INCREMENT);
        }
    }

    /**
     * Checks to see if any bullets have collided with this ship.
     */
    protected void checkBulletCollisions() {
        BaseParticle bullet = mGameState.getShots().checkForCollision(mPositionX, mPositionY,
                SHIP_SIZE);
        if (bullet != null && bullet.getOwnerId() != mPlayerId) {
            bullet.handleCollision();
            if (!isInvincible()) {
                spawnRingBurstAroundShip(
                        RINGBURST_PRIMARY_MIN_SPEED,
                        RINGBURST_PRIMARY_MAX_SPEED);
                spawnRingBurstAroundShip(
                        RINGBURST_SECONDARY_MIN_SPEED,
                        RINGBURST_SECONDARY_MAX_SPEED);
                mRespawnTimer = RESPAWN_FRAME_COUNT;
                mGameState.scorePoint(bullet.getOwnerId());
                changeScore(-1);
            }
        }
    }

    /**
     * Spawns a ring of particles radiating from the ship's current position.
     */
    protected void spawnRingBurstAroundShip(float minSpeed, float maxSpeed) {
        mGameState.getExplosions().spawnRingBurst(mPositionX, mPositionY, mColor, minSpeed,
                maxSpeed, RINGBURST_PARTICLE_COUNT);
    }

    /**
     * Fires one or more bullets from the ship's current location.
     * @param bulletCount the number of bullets to fire.
     * @param spreadArc for multiple bullets, the arc, in degrees, over which the bullets
     *               are spread out.
     * @param speed the speed of the bullets.
     * @param recharge the number of frames delay before the next shot can be fired.
     */
    protected void fireBullets(int bulletCount, float spreadArc, float speed,
                               float recharge) {
        mGunRechargeTimer = recharge;

        for (int i = 0; i < bulletCount; ++i) {
            float angleDegrees;
            if (bulletCount > 1) {
                // Compute this bullet's position along the spread arc.
                angleDegrees =
                        -spreadArc / 2.0f + (float) i * spreadArc / ((float) bulletCount - 1.0f);
            } else {
                // Single bullets are always fired along the aiming direction.
                angleDegrees = 0;
            }
            float angleRadians = (float) Math.toRadians(angleDegrees);
            float angleSin = (float) Math.sin(angleRadians);
            float angleCos = (float) Math.cos(angleRadians);

            float shotDx = mAimX * angleCos - mAimY * angleSin;
            float shotDy = mAimX * angleSin + mAimY * angleCos;
            BaseParticle myShot = mGameState.getShots().spawnParticle(BULLET_LIFETIME_IN_SECONDS);
            if (myShot != null) {
                myShot.setPosition(mPositionX, mPositionY);
                myShot.setSpeed(shotDx * speed, shotDy * speed);
                myShot.setColor(mColor);
                myShot.setSize(BULLET_PARTICLE_SIZE);
                myShot.setAspectRatio(BULLET_PARTICLE_ASPECT_RATIO);
                myShot.setOwnerId(mPlayerId);

                // Offset the bullet's starting position a few steps ahead of our position.
                myShot.incrementPosition(BULLET_PARTICLE_INITIAL_POSITION_INCREMENT);
            }
        }
    }

    /**
     * Updates the ship's spawning state and invincibility state.
     */
    private boolean updateStatus(float frameDelta) {
        updateSpawningStatus(frameDelta);
        if (!isSpawned()) {
            return false;
        }
        updateInvincibilityStatus(frameDelta);
        return true;
    }

    /**
     * Picks a new starting location when the ship is spawned.
     */
    private void updateSpawningStatus(float frameDelta) {
        // Are we waiting to respawn.
        if (mRespawnTimer > 0.0f) {
            mRespawnTimer -= frameDelta;
            if (mRespawnTimer <= 0.0f) {
                // Time to respawn.
                mRespawnTimer = 0.0f;

                // Pick a new location.
                setPositionX(Utils.randFloatInRange(GameState.MAP_LEFT_COORDINATE,
                        GameState.MAP_RIGHT_COORDINATE));
                setPositionY(Utils.randFloatInRange(GameState.MAP_BOTTOM_COORDINATE,
                        GameState.MAP_TOP_COORDINATE));

                spawnRingBurstAroundShip(RINGBURST_PRIMARY_MIN_SPEED, RINGBURST_PRIMARY_MAX_SPEED);
                mInvincibilityTimer = INVINCIBILITY_FRAME_COUNT;

                // Newly spawned ships don't have any powerup weapons.
                mCurrentGun = WEAPON_BASEGUN;
            }
        }
    }

    /**
     * Keeps track of this ship's invincibility status.
     */
    private void updateInvincibilityStatus(float frameDelta) {
        if (mInvincibilityTimer > 0.0f) {
            mInvincibilityTimer -= frameDelta;
            if (mInvincibilityTimer < 0.0f) {
                mInvincibilityTimer = 0.0f;
            }
        }

        mCurrentColor.set(mColor);

        // Flash the ship while it is invincible.
        if (isInvincible()
                && ((int) (mInvincibilityTimer / INVINCIBILITY_COLOR_BLINK_RATE) % 2 == 0)) {
            mCurrentColor.darken(INVINCIBILITY_COLOR_DARKEN_FACTOR);
        }
    }

    /**
     * Reads the movement joystick and updates the ship's position.
     */
    private void updateShipPosition(float frameDelta) {
        float newHeadingX = mController.getJoystickPosition(GamepadController.JOYSTICK_1,
                GamepadController.AXIS_X);
        float newHeadingY = mController.getJoystickPosition(GamepadController.JOYSTICK_1,
                GamepadController.AXIS_Y);

        float magnitude = Utils.vector2DLength(newHeadingX, newHeadingY);
        if (magnitude > JOYSTICK_MOVEMENT_THRESHOLD) {
            // Normalize the direction vector.
            mHeadingX = newHeadingX / magnitude;
            mHeadingY = -newHeadingY / magnitude;

            // Compute the new speed.
            mVelocityX = newHeadingX;
            mVelocityY = -newHeadingY;

            if (magnitude > 1.0f) {
                // Limit the max speed to "1".  If the movement joystick is moved less than
                // 1 unit from the center, the ship will move less than it's maximum speed.
                // If the joystick moves more than 1 unit from the center, dividing by
                // magnitude will limit the speed of the ship, but keep the direction of moment
                // correct.
                mVelocityX /= magnitude;
                mVelocityY /= magnitude;
            }

            // Create a particle trail (exhaust) behind the ship.
            GameState.getInstance().getExplosions().spawnExhaustTrail(
                    mPositionX, mPositionY,
                    mVelocityX, mVelocityY,
                    mColor, 1);
        }

        setPositionX(mPositionX + mVelocityX * frameDelta);
        setPositionY(mPositionY + mVelocityY * frameDelta);

        // Use drag so that the ship will coast to a stop after the movement controller
        // is released.
        mVelocityX *= 1.0f - frameDelta * DRAG;
        mVelocityY *= 1.0f - frameDelta * DRAG;
        if (Utils.vector2DLength(mVelocityX, mVelocityY) < MINIMUM_VELOCITY) {
            mVelocityX = 0.0f;
            mVelocityY = 0.0f;
        }
    }

    /**
     * Checks for controller key presses and fires the gun.
     */
    private void handleKeyPressesAndFiring(float frameDelta) {
        mGunRechargeTimer -= frameDelta;
        if (mGunRechargeTimer <= 0) {
            // The gun is ready to fire, so calculate the aim direction.
            calculateAimDirection();
            if (mJoystickAiming || mController.isButtonDown(GamepadController.BUTTON_X)) {
                fireGun();
            }
        }
    }

}
