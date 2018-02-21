/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.androidtv.cantv.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages a playlist of videos.
 */
public class Playlist {

    private List<Video> playlist;
    private int currentPosition;

    public Playlist() {
        playlist = new ArrayList<>();
        currentPosition = 0;
    }

    /**
     * Clears the videos from the playlist.
     */
    public void clear() {
        playlist.clear();
    }

    /**
     * Adds a video to the end of the playlist.
     *
     * @param video to be added to the playlist.
     */
    public void add(Video video) {
        playlist.add(video);
    }

    /**
     * Sets current position in the playlist.
     *
     * @param currentPosition
     */
    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    /**
     * Returns the size of the playlist.
     *
     * @return The size of the playlist.
     */
    public int size() {
        return playlist.size();
    }

    /**
     * Moves to the next video in the playlist. If already at the end of the playlist, null will
     * be returned and the position will not change.
     *
     * @return The next video in the playlist.
     */
    public Video next() {
        if ((currentPosition + 1) < size()) {
            currentPosition++;
            return playlist.get(currentPosition);
        }
        return null;
    }

    /**
     * Moves to the previous video in the playlist. If the playlist is already at the beginning,
     * null will be returned and the position will not change.
     *
     * @return The previous video in the playlist.
     */
    public Video previous() {
        if (currentPosition - 1 >= 0) {
            currentPosition--;
            return playlist.get(currentPosition);
        }
        return null;
    }

    /**
     * Reverse order of the playlist.
     */
    public void reverse() {
        Collections.reverse(playlist);
        currentPosition = size()-currentPosition-1;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public Video getFirstVideo() {return playlist.get(0);}

    public Video getCurrentVideo() {
        return playlist.get(currentPosition);
    }

    public int getVideoPosition(Video video){
        for (int i=0;i<playlist.size();i++) {
            if (playlist.get(i).videoUrl==video.videoUrl) {
                return i;
            }
        }
        return 0;
    }
}