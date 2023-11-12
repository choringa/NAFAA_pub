package com.indi.nafaa.asynctasks;

import android.os.AsyncTask;
import android.util.Log;

import com.indi.nafaa.MainActivity;
import com.indi.nafaa.Utils;

public class RequestEncryptedLoginAsyncTask extends AsyncTask<Object, Void, String> {

    private static final String TAG = "RequestLoginEncryptAT";
    private MainActivity mainActivity;
    private int mode;
    private String host;

    public RequestEncryptedLoginAsyncTask(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Object... params) {
        String jsonString = (String) params[0];
        mode = (int) params[1];
        host = (String) params[2];
        Utils utils = new Utils(host);
        String response;
        if(mode == 1)
            response = utils.makePostRequest(Utils.LOGIN_SERVICE, jsonString, true);
        else if(mode == 2)
            response = utils.makePostRequest(Utils.LOGIN_SERVICE, jsonString, false);
        else
            response = utils.makePostRequest(Utils.LOGIN_ENCRYPTED_SERVICE, jsonString, false);
        return response;
    }

    @Override
    protected void onPostExecute(String response) {
        super.onPostExecute(response);
        Log.i(TAG, "POST Response----------->" + response);
        mainActivity.handleLoginResponse(response);
    }
}
