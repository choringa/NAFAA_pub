package com.indi.nafaa.asynctasks;

import android.os.AsyncTask;
import android.util.Log;

import com.indi.nafaa.MainActivity;
import com.indi.nafaa.Utils;

public class RequestSharedKeyAsyncTask extends AsyncTask<Object, Void, String> {

    private static final String TAG = "RequestSharedKeyAsyncT";
    private MainActivity mainActivity;

    public RequestSharedKeyAsyncTask(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Object... params) {
        Utils utils = new Utils();
        String jsonString = (String) params[0];
        return utils.makePostRequest(Utils.ECDH_SERVICE, jsonString, false);
    }

    @Override
    protected void onPostExecute(String response) {
        super.onPostExecute(response);
        Log.i(TAG, "POST Response----------->" + response);
        mainActivity.handleECDHResponse(response);
    }
}
