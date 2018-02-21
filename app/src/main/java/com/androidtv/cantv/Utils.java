/*
 * Copyright (c) 2015 The Android Open Source Project
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

package com.androidtv.cantv;

import com.androidtv.cantv.R;
import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.min;

/**
 * A collection of utility methods, all static.
 */
public class Utils {

    /*
     * Making sure public utility methods remain static
     */
    private Utils() {
    }

    private static String getStringResourceByName(Context ctx,String aString) {
        String pkgName = ctx.getPackageName();
        int resId = ctx.getResources().getIdentifier(aString, "string", pkgName);
        return ctx.getString(resId);
    }

    /**
     * Get Youtube VideoID if contains from a given relative URL.
     *
     * @return the vid representation of the response
     * @throws IOException
     */
    public static String getYoutubeVid(Context ctx, String rurlString) throws IOException {
        //
        String vid = "";
        try {
            String html= fetchHTMLString(ctx,ctx.getString(R.string.provider_host)+rurlString);
            String[] p1 = html.split("id=\"videoplay\" src=\"https://www.youtube.com/embed/");
            String[] p2 = p1[1].split("\\?");
            vid = p2[0];
        } catch (IOException e) {
            Log.e("GetYoutubeVid", "Failed");
        } finally {
            return vid;
        }
    }

    /**
     * Get Video URL from a given page relative URL.
     *
     * @return the vurl representation of the response
     * @throws IOException
     */
    public static String getVideoUrl(Context ctx, String rurlString) throws IOException {
        //
        String vurl = "";
        try {
            String html= fetchHTMLString(ctx,ctx.getString(R.string.provider_host)+rurlString);
            String[] p1 = html.split("\\{vjsPlay\\(\"");
            String[] p2 = p1[1].split("\"\\)\\}\\<\\/script\\>");
            vurl = p2[0];
        } catch (IOException e) {
            Log.e("GetVideoUrl", "Failed");
        } finally {
            return vurl;
        }
    }

    /**
     * Get Video episode json from a given page relative URL.
     *
     * @return the epijson representation of the response
     * @throws IOException
     */
    public static String getVideoEps(Context ctx, String rurlString) throws IOException {
        //
        String epijson = "{";
        try {
            String html= fetchHTMLString(ctx,ctx.getString(R.string.provider_host)+rurlString);
            Document body = Jsoup.parse(html);
            Elements ep_pages = body.select("ul.partInfo");
            for (Element ep_page:ep_pages) {
                Elements eps_per_page = ep_page.select("li");
                for (Element ep:eps_per_page) {
                    Element a = ep.select("a").first();
                    epijson += "\""+a.attr("onclick").split(",")[1]+"\":\""+a.text()+"\",";
                }
            }
            epijson = epijson.substring(0,epijson.length()-1)+"}";
            //Log.d("ep",epijson);
        } catch (IOException e) {
            Log.e("GetVideoEps", "Failed");
        } finally {
            return epijson;
        }
    }

    /**
     * Get Video episode json from a given page relative URL.
     *
     * @return the epijson representation of the response
     * @throws IOException
     */
    public static String getAllVideos(Context ctx) throws IOException {
        List<String> categories = Arrays.asList("movie_0_0_0_hot","child_0_0_0_hot","anime_0_0_0_hot","variety_0_0_0_hot",
                "series_0_0_0_hot","documentary_0_0_0_hot","movie_10038_0_0_hot","movie_0_0_10023_hot","series_10038_0_0_hot",
                "series_10202_0_0_hot","series_10130_0_0_hot","anime_10038_0_0_hot","anime_10130_0_0_hot");
        //List<String> categories = Arrays.asList("movie_0_0_0_hot");
        StringBuilder avjson = new StringBuilder("{\"allvideos\":[");
        try {
            for (String catelem:categories) {
                avjson.append(getVideoByCategory(ctx,catelem));
                avjson.append(",");
            }
            //String avjs = avjson.substring(0,avjson.length()-1)+"]}";
            //Log.d("ep",epijson);
        } catch (IOException e) {
            Log.e("GetAllVideos", "Failed");
        } finally {
            return avjson.substring(0,avjson.length()-1)+"]}";
        }
    }

    /**
     * Get category video list json from a given category list name and sort rule.
     *
     * @return the categoryjson representation of the response
     * @throws IOException
     */
    public static String getVideoByCategory(Context ctx, String categoryName) throws IOException {
        //
        StringBuilder epijson = new StringBuilder("{\"category\":\""+getStringResourceByName(ctx,categoryName)+"\",\"videos\":[");
        try {
            String html= fetchHTMLString(ctx,ctx.getString(R.string.provider_host)+"/filter-"+categoryName.replaceAll("_","-"));
            Document body = Jsoup.parse(html);
            Elements video_list_of_page_n = body.select("ul.cfix").first().select("li");
            Integer pagemax = Integer.valueOf(body.select("a.tcdNumber").last().text());
            Integer pgmax = min(pagemax,3);
            for (Integer pgcnt=1;pgcnt<=pgmax;pgcnt++) {
                for(Element video:video_list_of_page_n) {
                    epijson.append("{\"title\":\"");
                    epijson.append(video.select("p").text().replaceAll(" 第","第").split(" ")[0]);
                    epijson.append("\",\"card\":\"");
                    epijson.append(video.select("img.cover").attr("src"));
                    epijson.append("\",\"sources\":[\"");
                    epijson.append(video.select("a.mtg-index-list-pic").attr("href"));
                    epijson.append("\"]},");
                }
                if (pgcnt==pgmax) {
                    break;
                }
                html= fetchHTMLString(ctx,ctx.getString(R.string.provider_host)+"/filter-"+categoryName.replaceAll("_","-")+"-p"+String.valueOf(pgcnt+1));
                body = Jsoup.parse(html);
                video_list_of_page_n = body.select("ul.cfix").first().select("li");
            }
            //return epijson.toString().substring(0,epijson.length()-1)+"]}";
            //Log.d("ep",epijson);
        } catch (IOException e) {
            Log.e("GetVideoByCategory", "Failed");
        } finally {
            return epijson.toString().substring(0,epijson.length()-1)+"]}";
        }
    }

    /**
     * Fetch HTML source code from a given URL.
     *
     * @return the html representation of the response
     * @throws IOException
     */

    private static String fetchHTMLString(Context ctx, String urlString) throws IOException {
        BufferedReader reader = null;
        String ua = ctx.getString(R.string.ua);
        String html = "";
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent",ua);
        urlConnection.connect();
        try {
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(),
                    "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            html = sb.toString();
            return html;
        } finally {
            urlConnection.disconnect();
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e("FetchHTML", "Fetch HTML failed");
                }
            }
        }
    }

    /**
     * Fetch JSON object from a given URL.
     *
     * @return the json representation of the response
     * @throws IOException
     */

    public static String fetchJSONString(String urlString) throws IOException {
        BufferedReader reader = null;
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.connect();
        try {
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(),
                    "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            return json;//new JSONObject(json);
        } finally {
            urlConnection.disconnect();
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e("FetchJSON", "JSON feed closed");
                }
            }
        }
    }
}
