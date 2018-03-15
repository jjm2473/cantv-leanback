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

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.CursorObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.androidtv.cantv.R;
import com.androidtv.cantv.Utils;
import com.androidtv.cantv.data.FetchVideoService;
import com.androidtv.cantv.data.VideoContract;
import com.androidtv.cantv.model.Video;
import com.androidtv.cantv.model.VideoCursorMapper;
import com.androidtv.cantv.presenter.CardPresenter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
 * in-app search
 */
public class SearchFragment extends android.support.v17.leanback.app.SearchFragment
        implements android.support.v17.leanback.app.SearchFragment.SearchResultProvider,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "SearchFragment";

    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private String mQuery;
    private final CursorObjectAdapter mVideoCursorAdapter =
            new CursorObjectAdapter(new CardPresenter());

    private int mSearchLoaderId = 1;
    private boolean mResultsFound = false;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        mVideoCursorAdapter.setMapper(new VideoCursorMapper());

        setSearchResultProvider(this);
        setOnItemViewClickedListener(new ItemViewClickedListener());

    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String res = data.getString("searchresult");
            //Log.d("res",res);
            if (res!=null) {
                try {
                    JSONObject jo = new JSONObject(res);
                    JSONArray ja = jo.getJSONArray("result");
                    for (Integer i = 0; i < ja.length(); i++) {

                    }
                        mPlaylist.add(Video.VideoBuilder.buildFromSuburl(mVideo, epsubtitle, epkey));
                    mEpisodeActionAdapter = setupEpisodesVideos();
                    ArrayObjectAdapter mRowsAdapter = initializeEpisodesRow();
                    setAdapter(mRowsAdapter);
                } catch (Exception e) {
                    Log.e("FetchEpisodeFailed", "Get episodes failed");
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                setSearchQuery(data, true);
                break;
            default:
                if (!hasResults()) {
                    getView().findViewById(R.id.lb_search_bar_speech_orb).requestFocus();
                }
                break;
        }
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        //loadQuery(newQuery);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        loadQuery(query);
        return true;
    }

    public boolean hasResults() {
        return mRowsAdapter.size() > 0 && mResultsFound;
    }

    private void loadQuery(String query) {
        if (!TextUtils.isEmpty(query) && !query.equals("nil")) {
            mQuery = query;
            getLoaderManager().initLoader(mSearchLoaderId++, null, this);
        }
    }

    public void focusOnSearch() {
        getView().findViewById(R.id.lb_search_bar).requestFocus();
    }

    class netSearchByKeyword implements Runnable{
        String keyword;
        netSearchByKeyword(String kw) {keyword=kw;}
        public void run() {
            //String mVideoUrl = getString(R.string.videoplayback_url_prefix)+"/isyt"+video.videoUrl;
            try {
                String vlst = Utils.searchVideosByKeyword(mContext,keyword);
                Message msg = new Message();
                Bundle data = new Bundle();
                data.putString("searchresult",vlst);
                msg.setData(data);
                handler.sendMessage(msg);
                //Log.d("fetchResult",js.toString());
            } catch (Exception e) {
                Log.e("Search By Keyword", "Failed");
                e.printStackTrace();
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String query = mQuery;
        return new CursorLoader(
                getActivity(),
                VideoContract.VideoEntry.CONTENT_URI,
                null, // Return all fields.
                VideoContract.VideoEntry.COLUMN_NAME + " LIKE ? OR " +
                        VideoContract.VideoEntry.COLUMN_DESC + " LIKE ?",
                new String[]{"%" + query + "%", "%" + query + "%"},
                null // Default sort order
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        int titleRes;
        if (cursor != null && cursor.moveToFirst()) {
            mResultsFound = true;
            titleRes = R.string.search_results;
        } else {
            mResultsFound = false;
            titleRes = R.string.no_search_results;
        }
        mVideoCursorAdapter.changeCursor(cursor);
        HeaderItem header = new HeaderItem(getString(titleRes, mQuery));
        mRowsAdapter.clear();
        ListRow row = new ListRow(header, mVideoCursorAdapter);
        mRowsAdapter.add(row);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mVideoCursorAdapter.changeCursor(null);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
                Video video = (Video) item;
                Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                intent.putExtra("Video", video);

                getActivity().startActivity(intent);
            } else {
                Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
            }
        }
    }

}