package com.duckduckgo.app.kahf_guard.api;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Api {
    //api urls
    public static final String URL_TOTAL_BLACKLIST_HOSTS = "https://api.kahfdns.com/totalBlacklistHosts";
    public static final String URL_BANNERS = "https://adm.kahfdns.com/Public/App_Banners/banners.json";

    public void getTotalBlacklistHosts(ApiCallback apiCallback){
        //create thread
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        //execute thread
        executorService.execute(() -> {
            try {
                URL url = new URL(URL_TOTAL_BLACKLIST_HOSTS);

                //get response
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String jsonString = stringBuilder.toString();
                JSONObject jsonObject = new JSONObject(jsonString);

                //get total
                String totalString = jsonObject.getString("total");
                String lastUpdated = jsonObject.getString("lastUpdated");

                DecimalFormat formatter = new DecimalFormat("#,###");
                Number totalNumber = formatter.parse(totalString);
                String totalFormatted = formatter.format(totalNumber);

                handler.post(() -> {
                    TotalBlacklistHosts totalBlacklistHosts = new TotalBlacklistHosts(totalNumber, totalFormatted, lastUpdated);
                    apiCallback.onResponse(totalBlacklistHosts);
                });
            } catch (Exception e){
                e.printStackTrace();
                handler.post(() -> {
                    // Handle the error
                    apiCallback.onError(e.getMessage());
                });
            }
        });
    }

    public void getBottomBanner(ApiCallback apiCallback){
        //create thread
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        //execute thread
        executorService.execute(() -> {
            try {
                URL url = new URL(URL_BANNERS);

                //get response
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String jsonString = stringBuilder.toString();
                JSONObject jsonObject = new JSONObject(jsonString);

                //get banner image and link
                String image = jsonObject.getJSONObject("androidBottom").getString("image");
                String link = jsonObject.getJSONObject("androidBottom").getString("link");

                handler.post(() -> {
                    BottomBanner bottomBanner = new BottomBanner(image, link);
                    apiCallback.onResponse(bottomBanner);
                });
            } catch (Exception e){
                e.printStackTrace();
                handler.post(() -> {
                    // Handle the error
                    apiCallback.onError(e.getMessage());
                });
            }
        });
    }
}
