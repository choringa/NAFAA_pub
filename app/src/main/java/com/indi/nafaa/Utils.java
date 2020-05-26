package com.indi.nafaa;

import android.util.Log;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Utils {

    private static final String TAG = "Utils";
    private static final String BASE_URL = "http://172.17.0.1:5000/";
    public static final String LOGIN_SERVICE = "login";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client;

    public Utils(){
        client = new OkHttpClient();
    }

    public String makePostRequest(String service, String json) {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + service)
                .post(body)
                .build();
        Log.i(TAG, "request--->" + request.toString());
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
        catch (IOException e){
            Log.e(TAG, "Boom-->" + e.getLocalizedMessage());
            return "Algo malio sal";
        }
    }
}
