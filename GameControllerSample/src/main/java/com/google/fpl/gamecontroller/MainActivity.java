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

import android.app.Activity;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Application entry point.
 */
public class MainActivity extends Activity {
    /**
     * Our own OpenGL View overridden
     */
    private GameState mGamestate;

    private static boolean isJoystick(int source) {
        return (source & InputDevice.SOURCE_JOYSTICK) != 0;
    }

    /**
     * Initiate our @see Lesson09.java,
     * which is GLSurfaceView and Renderer
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Initiate our Lesson with this Activity Context handed over
        mGamestate = new GameState(this);
        //Set the lesson as View to the Activity
        setContentView(mGamestate);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGamestate.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGamestate.onPause();
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return mGamestate.handleInputEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // We only care about game controllers.
        if (!isJoystick(event.getSource())) {
            return super.dispatchKeyEvent(event);
        }
        mGamestate.handleInputEvent(event);
        return true;
    }
}
