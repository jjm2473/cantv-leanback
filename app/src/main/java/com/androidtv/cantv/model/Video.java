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

package com.androidtv.cantv.model;

import android.media.MediaDescription;
import android.os.Parcel;
import android.os.Parcelable;

import com.androidtv.cantv.R;

/**
 * Video is an immutable object that holds the various metadata associated with a single video.
 */
public final class Video implements Parcelable {
    public final long id;
    public final String category;
    public final String title;
    public final String cardImageUrl;
    public final String videoUrl;

    private Video(
            final long id,
            final String category,
            final String title,
            final String videoUrl,
            final String cardImageUrl
            ) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.videoUrl = videoUrl;
        this.cardImageUrl = cardImageUrl;
    }

    protected Video(Parcel in) {
        id = in.readLong();
        category = in.readString();
        title = in.readString();
        cardImageUrl = in.readString();
        videoUrl = in.readString();
    }

    public static final Creator<Video> CREATOR = new Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel in) {
            return new Video(in);
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };

    @Override
    public boolean equals(Object m) {
        return m instanceof Video && id == ((Video) m).id;
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(category);
        dest.writeString(title);
        dest.writeString(cardImageUrl);
        dest.writeString(videoUrl);
    }

    @Override
    public String toString() {
        String s = "Video{";
        s += "id=" + id;
        s += ", category='" + category + "'";
        s += ", title='" + title + "'";
        s += ", videoUrl='" + videoUrl + "'";
        s += ", cardImageUrl='" + cardImageUrl + "'";
        s += "}";
        return s;
    }

    // Builder for Video object.
    public static class VideoBuilder {
        private long id;
        private String category;
        private String title;
        private String cardImageUrl;
        private String videoUrl;

        public VideoBuilder id(long id) {
            this.id = id;
            return this;
        }

        public VideoBuilder category(String category) {
            this.category = category;
            return this;
        }

        public VideoBuilder title(String title) {
            this.title = title;
            return this;
        }

        public VideoBuilder videoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
            return this;
        }

        public VideoBuilder cardImageUrl(String cardImageUrl) {
            this.cardImageUrl = cardImageUrl;
            return this;
        }

        public Video buildFromMediaDesc(MediaDescription desc) {
            return new Video(
                    Long.parseLong(desc.getMediaId()),
                    "", // Category - not provided by MediaDescription.
                    String.valueOf(desc.getTitle()),
                    "", // Media URI - not provided by MediaDescription.
                    String.valueOf(desc.getIconUri())
            );
        }

        public static Video buildFromSuburl(Video mVideo,String subtitle,String suburlid) {
            return new Video(
                    mVideo.id,
                    mVideo.category,
                    subtitle,
                    mVideo.videoUrl+"-"+suburlid, // Media URI - provided by mVideo page.
                    ""
            );
        }

        public static Video buildFromSearchResult(String title,String videourl,String cardimageurl) {
            return new Video(
                    0,
                    "SEARCH",
                    title,
                    videourl,
                    cardimageurl
            );
        }

        public Video build() {
            return new Video(
                    id,
                    category,
                    title,
                    videoUrl,
                    cardImageUrl
            );
        }
    }
}
