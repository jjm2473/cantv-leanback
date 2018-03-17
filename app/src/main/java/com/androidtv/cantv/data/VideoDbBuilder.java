/*
 * Copyright (c) 2016 The Android Open Source Project
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
package com.androidtv.cantv.data;

import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;

import com.androidtv.cantv.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The VideoDbBuilder is used to grab a JSON file from a server and parse the data
 * to be placed into a local database
 */
public class VideoDbBuilder {
    public static final String TAG_MEDIA = "videos";
    public static final String TAG_All_VIDEOS = "allvideos";
    public static final String TAG_CATEGORY = "category";
    public static final String TAG_SOURCES = "sources";
    public static final String TAG_CARD_THUMB = "card";
    public static final String TAG_TITLE = "title";

    private static final String TAG = "VideoDbBuilder";

    private Context mContext;

    /**
     * Default constructor that can be used for tests
     */
    public VideoDbBuilder() {

    }

    public VideoDbBuilder(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * Fetches JSON data representing videos from a server and populates that in a database
     * @param url The location of the video list
     */
    public @NonNull List<ContentValues> fetch()
            throws IOException, JSONException {
        JSONObject videoData = fetchJSON();
        return buildMedia(videoData);
    }

    /**
     * Takes the contents of a JSON object and populates the database
     * @param jsonObj The JSON object of videos
     * @throws JSONException if the JSON object is invalid
     */
    public List<ContentValues> buildMedia(JSONObject jsonObj) throws JSONException {

        JSONArray categoryArray = jsonObj.getJSONArray(TAG_All_VIDEOS);
        List<ContentValues> videosToInsert = new ArrayList<>();

        for (int i = 0; i < categoryArray.length(); i++) {
            JSONArray videoArray;

            JSONObject category = categoryArray.getJSONObject(categoryArray.length()-1-i);
            String categoryName = category.getString(TAG_CATEGORY);
            videoArray = category.getJSONArray(TAG_MEDIA);

            for (int j = 0; j < videoArray.length(); j++) {
                JSONObject video = videoArray.getJSONObject(videoArray.length()-1-j);

                // If there are no URLs, skip this video entry.
                JSONArray urls = video.optJSONArray(TAG_SOURCES);
                if (urls == null || urls.length() == 0) {
                    continue;
                }

                String title = video.optString(TAG_TITLE);
                String videoUrl = (String) urls.get(0); // Get the first video only.
                String cardImageUrl = video.optString(TAG_CARD_THUMB);

                ContentValues videoValues = new ContentValues();
                videoValues.put(VideoContract.VideoEntry.COLUMN_CATEGORY, categoryName);
                videoValues.put(VideoContract.VideoEntry.COLUMN_NAME, title);
                videoValues.put(VideoContract.VideoEntry.COLUMN_VIDEO_URL, videoUrl);
                videoValues.put(VideoContract.VideoEntry.COLUMN_CARD_IMG, cardImageUrl);
                videosToInsert.add(videoValues);
            }
        }
        return videosToInsert;
    }

    /**
     * Fetch JSON object from a given URL.
     *
     * @return the JSONObject representation of the response
     * @throws JSONException
     * @throws IOException
     */
    private JSONObject fetchJSON() throws JSONException, IOException {
        try {
            String json = Utils.getAllVideos(mContext);
            return new JSONObject(json);
        } finally {
            //
        }
    }
}
