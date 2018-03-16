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
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.androidtv.cantv.R;
import com.androidtv.cantv.Utils;
import com.androidtv.cantv.model.Video;
import com.androidtv.cantv.presenter.CardPresenter;

import org.json.JSONArray;
import org.json.JSONObject;


/*
 * in-app search
 */
public class SearchFragment extends android.support.v17.leanback.app.SearchFragment
        implements android.support.v17.leanback.app.SearchFragment.SearchResultProvider {
    private static final String TAG = "SearchFragment";

    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private String mQuery;

    private boolean mResultsFound = false;
    private Context mContext;
    private SparseArrayObjectAdapter mResultActionAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

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
                    showSearchResult(ja);
                } catch (Exception e) {
                    Log.e("Get Search Result", "Failed");
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
                    getView().findViewById(R.id.lb_search_text_editor).requestFocus();
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
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mRowsAdapter.clear();
        loadQuery(query);
        return true;
    }

    public boolean hasResults() {
        return mRowsAdapter.size() > 0 && mResultsFound;
    }

    private void loadQuery(String query) {
        if (!TextUtils.isEmpty(query) && !query.equals("nil")) {
            mQuery = query;
            new Thread(new netSearchByKeyword(mQuery)).start();
        }
    }

    public void focusOnSearch() {
        getView().findViewById(R.id.lb_search_text_editor).requestFocus();
    }

    class netSearchByKeyword implements Runnable{
        String keyword;
        netSearchByKeyword(String kw) {keyword=kw;}
        public void run() {
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

    public void showSearchResult(JSONArray searchResultJSONArray) {
        int titleRes;
        if (searchResultJSONArray.length() != 0) {
            mResultsFound = true;
            titleRes = R.string.search_results;
        } else {
            mResultsFound = false;
            titleRes = R.string.no_search_results;
        }
        mResultActionAdapter = setupResultVideos(searchResultJSONArray);
        HeaderItem header = new HeaderItem(getString(titleRes, mQuery));
        ListRow row = new ListRow(header, mResultActionAdapter);
        mRowsAdapter.add(row);
    }

    private SparseArrayObjectAdapter setupResultVideos(JSONArray searchResultJsonArray) {
        SparseArrayObjectAdapter resultVideosAdapter = new SparseArrayObjectAdapter(new CardPresenter());
        for (Integer i = 0; i < searchResultJsonArray.length(); i++) {
            try {
                JSONObject _jo = searchResultJsonArray.getJSONObject(i);
                resultVideosAdapter.set(i,Video.VideoBuilder.buildFromSearchResult(_jo.getString("title"),_jo.getJSONArray("sources").getString(0), _jo.getString("card")));
            } catch (Exception e) {
                //
            }
        }
        return resultVideosAdapter;
    };

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
                Video video = (Video) item;
                Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                intent.putExtra("Video", video);
                //rowViewHolder.view.clearFocus();
                getActivity().startActivity(intent);
            } else {
                Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
            }
        }
    }

}