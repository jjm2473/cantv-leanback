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

package com.androidtv.cantv.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v17.leanback.app.VideoFragment;
import android.support.v17.leanback.app.VideoFragmentGlueHost;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.util.Log;

import com.androidtv.cantv.R;
import com.androidtv.cantv.model.Playlist;
import com.androidtv.cantv.model.Video;
import com.androidtv.cantv.player.VideoPlayerGlue;
import com.androidtv.cantv.Utils;
import com.androidtv.cantv.presenter.EpisodePresenter;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONObject;
import java.util.Iterator;


/**
 * Plays selected video, loads playlist and related videos, and delegates playback to {@link
 * VideoPlayerGlue}.
 */
public class PlaybackFragment extends VideoFragment {

    private static final int UPDATE_DELAY = 16;

    private VideoPlayerGlue mPlayerGlue;
    private LeanbackPlayerAdapter mPlayerAdapter;
    private SimpleExoPlayer mPlayer;
    private TrackSelector mTrackSelector;
    private PlaylistActionListener mPlaylistActionListener;
    private Context mContext;

    private Video mVideo;
    private Playlist mPlaylist;
    private SparseArrayObjectAdapter mEpisodeActionAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
        mVideo = getActivity().getIntent().getParcelableExtra("Video");
        mPlaylist = new Playlist();
        //get playlist if any
        new Thread(netGetEpisode).start();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String vu = data.getString("videourl");
            //Log.d("vu",vu);
            String js = data.getString("jsonstring");
            //Log.d("js",js);
            if (js!=null) {
                Log.d("js",js);
                try {
                    JSONObject jo = new JSONObject(js);
                    Iterator<String> epkeys = jo.keys();
                    //mPlaylist.add(Video.VideoBuilder.buildFromSuburl(mVideo, jo.get(epkeys.toString()).toString(), epkeys.toString()));
                    while (epkeys.hasNext()) {
                        String epkey = epkeys.next();
                        String epsubtitle = jo.get(epkey).toString();
                        mPlaylist.add(Video.VideoBuilder.buildFromSuburl(mVideo, epsubtitle, epkey));
                        //mEpisodeActionAdapter.set(mPlaylist.size(), new Action(mPlaylist.size(), epsubtitle));
                    }
                    mEpisodeActionAdapter = setupEpisodesVideos();
                    ArrayObjectAdapter mRowsAdapter = initializeEpisodesRow();
                    setAdapter(mRowsAdapter);
                } catch (Exception e) {
                    Log.e("FetchEpisodeFailed", "Get episodes failed");
                    e.printStackTrace();
                }
            }
            if (vu!=null) {
                Log.d("vu",vu);
                prepareMediaForPlaying(Uri.parse(vu));
                mPlayerGlue.play();
            }
        }
    };

    Runnable netGetEpisode = new Runnable() {
        @Override
        public void run() {
            //String epVideoUrl = getString(R.string.videoplayback_url_prefix)+"/"+mVideo.videoUrl.substring(1).replace('/','_')+".json";
            try {
                //String js = Utils.fetchJSONString(epVideoUrl);
                String js = Utils.getVideoEps(mContext, mVideo.videoUrl);
                Message msg = new Message();
                Bundle data = new Bundle();
                data.putString("jsonstring",js);
                msg.setData(data);
                handler.sendMessage(msg);
                //Log.d("fetchResult",js.toString());
            } catch (Exception e) {
                Log.e("FetchEpisodeFailed", "Get episodes failed");
                e.printStackTrace();
            }
        }
    };

    class netGetVideoUrl implements Runnable{
        Video video;
        netGetVideoUrl(Video v) {video=v;}
        public void run() {
            //String mVideoUrl = getString(R.string.videoplayback_url_prefix)+"/isyt"+video.videoUrl;
            try {
                String vurl = Utils.getVideoUrl(mContext,video.videoUrl);
                Message msg = new Message();
                Bundle data = new Bundle();
                data.putString("videourl",vurl);
                msg.setData(data);
                handler.sendMessage(msg);
                //Log.d("fetchResult",js.toString());
            } catch (Exception e) {
                Log.e("GetVideoUrl", "Failed");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || mPlayer == null)) {
            initializePlayer();
        }
    }

    /** Pauses the player. */
    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onPause() {
        super.onPause();

        if (mPlayerGlue != null && mPlayerGlue.isPlaying()) {
            mPlayerGlue.pause();
        }
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void initializePlayer() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        mTrackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        mPlayer = ExoPlayerFactory.newSimpleInstance(getActivity(), mTrackSelector);
        mPlayerAdapter = new LeanbackPlayerAdapter(getActivity(), mPlayer, UPDATE_DELAY);
        mPlaylistActionListener = new PlaylistActionListener(mPlaylist);
        mPlayerGlue = new VideoPlayerGlue(getActivity(), mPlayerAdapter, mPlaylistActionListener);
        mPlayerGlue.setHost(new VideoFragmentGlueHost(this));
        mPlayerGlue.playWhenPrepared();

        play(mVideo);

    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            mTrackSelector = null;
            mPlayerGlue = null;
            mPlayerAdapter = null;
            mPlaylistActionListener = null;
        }
    }

    private void play(Video video) {
        try {
            new Thread(new netGetVideoUrl(video)).start();
            mPlayerGlue.setTitle(video.title);
            //prepareMediaForPlaying(Uri.parse(Utils.getVideoUrl(mContext,video.videoUrl)));
            //prepareMediaForPlaying(Uri.parse(getResources().getString(R.string.videoplayback_url_prefix) + video.videoUrl));
            //mPlayerGlue.play();
        } catch (Exception e) {
            mPlayerGlue.pause();
        }
    }

    private void prepareMediaForPlaying(Uri mediaSourceUri) {
        String userAgent = Util.getUserAgent(getActivity(), "VideoPlayerGlue");
        MediaSource mediaSource =
                new HlsMediaSource(
                        mediaSourceUri,
                        new DefaultDataSourceFactory(getActivity(), userAgent),
                        //new DefaultExtractorsFactory(),
                        null,
                        null);

        mPlayer.prepare(mediaSource);
    }

    private ArrayObjectAdapter initializeEpisodesRow() {
        /*
         * To add a new row to the mPlayerAdapter and not lose the controls row that is provided by the
         * glue, we need to compose a new row with the controls row and our related videos row.
         *
         * We start by creating a new {@link ClassPresenterSelector}. Then add the controls row from
         * the media player glue, then add the related videos row.
         */
        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenter(
                mPlayerGlue.getControlsRow().getClass(), mPlayerGlue.getPlaybackRowPresenter());
        presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(presenterSelector);
        rowsAdapter.add(mPlayerGlue.getControlsRow());
        HeaderItem header = new HeaderItem(getString(R.string.episodes));
        ListRow row = new ListRow(header, mEpisodeActionAdapter);
        rowsAdapter.add(row);
        setOnItemViewClickedListener(new ItemViewClickedListener());

        return rowsAdapter;
    }

    private SparseArrayObjectAdapter setupEpisodesVideos() {
        int playlistPositionBackup = mPlaylist.getCurrentPosition();
        SparseArrayObjectAdapter episodeVideosAdapter = new SparseArrayObjectAdapter(new EpisodePresenter());
        episodeVideosAdapter.set(0, mPlaylist.getFirstVideo());
        mPlaylist.setCurrentPosition(0);
        for (int i=1;i < (mPlaylist.size());i++) {
            episodeVideosAdapter.set(i, mPlaylist.next());
        }
        mPlaylist.setCurrentPosition(playlistPositionBackup);
        return episodeVideosAdapter;
    };

    public void skipToNext() {
        mPlayerGlue.next();
    }

    public void skipToPrevious() {
        mPlayerGlue.previous();
    }

    public void rewind() {
        mPlayerGlue.rewind();
    }

    public void fastForward() {
        mPlayerGlue.fastForward();
    }

    /** Opens the video details page when a related video has been clicked. */
    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(
                Presenter.ViewHolder itemViewHolder,
                Object item,
                RowPresenter.ViewHolder rowViewHolder,
                Row row) {

            if (item instanceof Video) {
                Video video = (Video) item;
                mPlaylist.setCurrentPosition(mPlaylist.getVideoPosition(video));
                play(video);
            }
        }
    }

    class PlaylistActionListener implements VideoPlayerGlue.OnActionClickedListener {

        private Playlist mPlaylist;

        PlaylistActionListener(Playlist playlist) {
            this.mPlaylist = playlist;
        }

        @Override
        public void onPrevious() {
            play(mPlaylist.previous());
        }

        @Override
        public void onNext() {
            play(mPlaylist.next());
        }

        public void onReverse() {
            mPlaylist.reverse();
            mEpisodeActionAdapter = setupEpisodesVideos();
            ArrayObjectAdapter mRowsAdapter = initializeEpisodesRow();
            setAdapter(mRowsAdapter);
            //play(mPlaylist.getFirstVideo());
        }
    }
}
