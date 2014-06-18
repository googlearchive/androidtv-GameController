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

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Application entry point.
 *
 * Forwards events to its GameState object.
 */
public class MainActivity extends Activity {
    /**
     * Manages the OpenGl context and all game mechanics.
     */
    private GameState mGamestate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGamestate = new GameState(this);
        // GameState is a GLSurfaceView.
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
        return mGamestate.handleMotionEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mGamestate.handleKeyEvent(event);
    }
}
