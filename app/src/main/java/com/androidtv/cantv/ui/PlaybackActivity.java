/*
 * Copyright (c) 2014 The Android Open Source Project
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

package com.androidtv.cantv.ui;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.androidtv.cantv.R;
import com.androidtv.cantv.Utils;
import com.androidtv.cantv.model.Video;

import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;


/**
 * Loads PlaybackFragment and delegates input from a game controller.
 * <br>
 * For more information on game controller capabilities with leanback, review the
 * <a href="https://developer.android.com/training/game-controllers/controller-input.html">docs</href>.
 */
public class PlaybackActivity extends LeanbackActivity {
    //private String YOUTUBE_API_KEY = getString(R.string.youtube_api_key);//"AIzaSyB33K1PUH3-yhBS4kDG3NdT-Ig_9X21N-4";
    private static final float GAMEPAD_TRIGGER_INTENSITY_ON = 0.5f;
    // Off-condition slightly smaller for button debouncing.
    private static final float GAMEPAD_TRIGGER_INTENSITY_OFF = 0.45f;
    private boolean gamepadTriggerPressed = false;
    private Video mVideo;
    private PlaybackFragment mPlaybackFragment;
    private YTPlaybackFragment ytmPlaybackFragment;
    private Context mContext;

    private YouTubePlayer mPlayer;
    private YouTubePlayerView mPlayerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        mVideo = getIntent().getParcelableExtra("Video");

        new Thread(netCheckIfYoutube).start();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String isyt = data.getString("isyt");
            Log.d("isyt",isyt);
            // if not youtube
            if (isyt=="") {
                setContentView(R.layout.activity_playback);
                Fragment fragment =
                        getFragmentManager().findFragmentByTag(getString(R.string.playback_tag));
                if (fragment instanceof PlaybackFragment) {
                    mPlaybackFragment = (PlaybackFragment) fragment;
                    mPlaybackFragment.getView().setBackgroundColor(Color.BLACK);
                }
            } else {//if youtube
                setContentView(R.layout.youtube_playback);
                Fragment ytfragment =
                        getFragmentManager().findFragmentById(R.id.ytplayback_controls_fragment);
                if (ytfragment instanceof YTPlaybackFragment) {
                    ytmPlaybackFragment = (YTPlaybackFragment) ytfragment;
                    ytmPlaybackFragment.getView().setBackgroundColor(Color.BLACK);
                }
            }
        }
    };

    Runnable netCheckIfYoutube = new Runnable() {
        @Override
        public void run() {
            //String mVideoUrl = getString(R.string.videoplayback_url_prefix)+"/isyt"+mVideo.videoUrl;
            try {
                //String isyt = Utils.fetchJSONString(mVideoUrl);
                String isyt = Utils.getYoutubeVid(mContext,mVideo.videoUrl);
                Message msg = new Message();
                Bundle data = new Bundle();
                data.putString("isyt",isyt);
                msg.setData(data);
                handler.sendMessage(msg);
                //Log.d("fetchResult",js.toString());
            } catch (Exception e) {
                Log.e("FetchIsYoutubeFailed", "Get isyoutube failed");
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_R1) {
            mPlaybackFragment.skipToNext();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_L1) {
            mPlaybackFragment.skipToPrevious();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
            mPlaybackFragment.rewind();
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
            mPlaybackFragment.fastForward();
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        // This method will handle gamepad events.
        if (event.getAxisValue(MotionEvent.AXIS_LTRIGGER) > GAMEPAD_TRIGGER_INTENSITY_ON
                && !gamepadTriggerPressed) {
            mPlaybackFragment.rewind();
            gamepadTriggerPressed = true;
        } else if (event.getAxisValue(MotionEvent.AXIS_RTRIGGER) > GAMEPAD_TRIGGER_INTENSITY_ON
                && !gamepadTriggerPressed) {
            mPlaybackFragment.fastForward();
            gamepadTriggerPressed = true;
        } else if (event.getAxisValue(MotionEvent.AXIS_LTRIGGER) < GAMEPAD_TRIGGER_INTENSITY_OFF
                && event.getAxisValue(MotionEvent.AXIS_RTRIGGER) < GAMEPAD_TRIGGER_INTENSITY_OFF) {
            gamepadTriggerPressed = false;
        }
        return super.onGenericMotionEvent(event);
    }
}
