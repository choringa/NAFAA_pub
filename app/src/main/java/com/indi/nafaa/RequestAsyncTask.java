package com.indi.nafaa;

import android.os.AsyncTask;
import android.util.Log;

public class RequestAsyncTask extends AsyncTask<Object, Void, String> {

    private static final String TAG = "RequestAsyncTask";
    private MainActivity mainActivity;

    public RequestAsyncTask(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Object... params) {
        Utils utils = (Utils) params[0];
        String jsonString = (String) params[1];
        boolean secure = (boolean) params[2];
        String response = utils.makePostRequest(Utils.LOGIN_SERVICE, jsonString, secure);
        return response;
    }

    @Override
    protected void onPostExecute(String response) {
        super.onPostExecute(response);
        Log.i(TAG, "POST Response----------->" + response);
        if(response.contains(":hash:")){
            String [] respArray = response.split(":");
            Log.i(TAG, respArray.length + "");
            if(respArray.length > 2)
                mainActivity.setHash(respArray[2]);
            mainActivity.handleResponse(respArray[0]);
        }
        else
            mainActivity.handleResponse(response);
    }
}
