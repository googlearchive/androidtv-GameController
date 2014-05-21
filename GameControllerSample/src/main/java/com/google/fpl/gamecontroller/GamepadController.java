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

import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Handles input events from game pad controllers.
 */
public class GamepadController {

    public static final int BUTTON_A = 0;
    public static final int BUTTON_B = 1;
    public static final int BUTTON_X = 2;
    public static final int BUTTON_Y = 3;
    public static final int BUTTON_COUNT = 4;
    public static final int AXIS_X = 0;
    public static final int AXIS_Y = 1;
    public static final int AXIS_COUNT = 2;
    public static final int JOYSTICK_1 = 0;
    public static final int JOYSTICK_2 = 1;
    public static final int JOYSTICK_COUNT = 2;

    protected static final int FRAME_INDEX_CURRENT = 0;
    protected static final int FRAME_INDEX_PREVIOUS = 1;
    protected static final int FRAME_INDEX_COUNT = 2;

    // Position of twin sticks.
    protected float mJoystickPositions[][];
    // The device that we are tuned to.
    protected int mDeviceId = -1;
    // The button states for the current and previous frames.
    protected boolean mButtonState[][];

    public GamepadController() {
        mButtonState = new boolean[BUTTON_COUNT][FRAME_INDEX_COUNT];
        for (int button = 0; button < BUTTON_COUNT; ++button) {
            for (int frame = 0; frame < FRAME_INDEX_COUNT; ++frame) {
                mButtonState[button][frame] = false;
            }
        }
        mJoystickPositions = new float[JOYSTICK_COUNT][AXIS_COUNT];
        for (int joystick = 0; joystick < JOYSTICK_COUNT; ++joystick) {
            for (int axis = 0; axis < AXIS_COUNT; ++axis) {
                mJoystickPositions[joystick][axis] = 0.0f;
            }
        }
    }

    public int getDeviceID() {
        return mDeviceId;
    }

    public void setDeviceID(int newID) {
        mDeviceId = newID;
    }

    public float[] getJoystickPosition(int joystickIndex) {
        return mJoystickPositions[joystickIndex];
    }

    public boolean isButtonDown(int id) {
        return mButtonState[id][FRAME_INDEX_CURRENT];
    }

    public boolean wasButtonPressed(int id) {
        // Returns true if it's down now, but wasn't last frame.
        return mButtonState[id][FRAME_INDEX_CURRENT] &&
                !mButtonState[id][FRAME_INDEX_PREVIOUS];
    }

    public boolean wasButtonReleased(int id) {
        // Returns true if it's up now, but wasn't last frame.
        return !mButtonState[id][FRAME_INDEX_CURRENT] &&
                mButtonState[id][FRAME_INDEX_PREVIOUS];
    }

    public void advanceFrame() {
        // Copy the current button state to the previous frame.
        // We can't just toggle between both buffers because the buttons only update
        // when an event occurs (press or release), and not every frame.
        for (int i = 0; i < BUTTON_COUNT; i++) {
            mButtonState[i][FRAME_INDEX_PREVIOUS] = mButtonState[i][FRAME_INDEX_CURRENT];
        }
    }

    public boolean isActive() {
        return mDeviceId != -1;
    }

    public void handleInputEvent(InputEvent e) {
        if (e instanceof MotionEvent) {
            MotionEvent me = (MotionEvent) e;

            mJoystickPositions[JOYSTICK_1][AXIS_X] = me.getAxisValue(MotionEvent.AXIS_X);
            mJoystickPositions[JOYSTICK_1][AXIS_Y] = me.getAxisValue(MotionEvent.AXIS_Y);

            mJoystickPositions[JOYSTICK_2][AXIS_X] = me.getAxisValue(MotionEvent.AXIS_Z);
            mJoystickPositions[JOYSTICK_2][AXIS_Y] = me.getAxisValue(MotionEvent.AXIS_RZ);
        } else if (e instanceof KeyEvent) {
            KeyEvent ke = (KeyEvent) e;
            boolean keyState = ke.getAction() == KeyEvent.ACTION_DOWN;

            if (ke.getKeyCode() == KeyEvent.KEYCODE_BUTTON_A) {
                mButtonState[BUTTON_A][FRAME_INDEX_CURRENT] = keyState;
            }
            if (ke.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B) {
                mButtonState[BUTTON_B][FRAME_INDEX_CURRENT] = keyState;
            }
            if (ke.getKeyCode() == KeyEvent.KEYCODE_BUTTON_X) {
                mButtonState[BUTTON_X][FRAME_INDEX_CURRENT] = keyState;
            }
            if (ke.getKeyCode() == KeyEvent.KEYCODE_BUTTON_Y) {
                mButtonState[BUTTON_Y][FRAME_INDEX_CURRENT] = keyState;
            }
        } else {
            Log.d("GameControllerSample", "Unhandled input event.");
        }
    }
}
