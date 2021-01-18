package com.indi.nafaa;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.indi.nafaa.asynctasks.RequestLoginAsyncTask;
import com.indi.nafaa.asynctasks.RequestSharedKeyAsyncTask;
import com.indi.nafaa.cryptography.SecurityModule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    private TextView tvBadAuth;
    private SecurityModule securityModule;
    private EditText etUsername;
    private EditText etPassword;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        securityModule = new SecurityModule();
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Not today...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        progressDialog = new ProgressDialog(this);
        etUsername = findViewById(R.id.et_main_username);
        etPassword = findViewById(R.id.et_main_password);
        tvBadAuth = findViewById(R.id.tvBadAuth);
        Button btnLoginSecure = findViewById(R.id.btnSecureLogin);
        btnLoginSecure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginMethod(etUsername.getText().toString(), etPassword.getText().toString(), 1);
            }
        });

        Button btnLoginUnsecure = findViewById(R.id.btnUnsecureLogin);
        btnLoginUnsecure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginMethod(etUsername.getText().toString(), etPassword.getText().toString(), 2);
            }
        });
        Button btnEncryptedLogin = findViewById(R.id.btnEncryptedLogin);
        btnEncryptedLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestSharedKey();
            }
        });
    }
    private void changeProgressDialogMessage(String newMessage){
        if(progressDialog != null && progressDialog.isShowing())
            progressDialog.setMessage(newMessage);
        else
            showProgressDialogWithTitle(newMessage);
    }

    // Method to show Progress bar
    private void showProgressDialogWithTitle(String substring) {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        //Without this user can hide loader by tapping outside screen
        progressDialog.setCancelable(false);
        progressDialog.setMessage(substring);
        progressDialog.show();
    }

    // Method to hide/ dismiss Progress bar
    private void hideProgressDialogWithTitle() {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.dismiss();
    }

    private void requestSharedKey() {
        ECPublicKey ecPublicKey = securityModule.getClientECPublicKey();
        JSONObject jsonObjectPublicKey = new JSONObject();
        JSONObject jsonObjectPublicKeyPoint = new JSONObject();
        try {

            jsonObjectPublicKeyPoint.put("x_coordinate", ecPublicKey.getW().getAffineX());
            jsonObjectPublicKeyPoint.put("y_coordinate", ecPublicKey.getW().getAffineY());
            jsonObjectPublicKey.put("public_key", jsonObjectPublicKeyPoint);

            Log.i(TAG, "requestSharedKey --> jsonObject: " + jsonObjectPublicKey.toString());
            Object[] params = {jsonObjectPublicKey.toString()};
            showProgressDialogWithTitle(getString(R.string.generating_shared_secret));
            new RequestSharedKeyAsyncTask(this).execute(params);
        }
        catch (JSONException e){
            Log.e(TAG, "ERROR --> loginMethod --> " + e.getLocalizedMessage());
        }
    }

    private void loginMethod(String username, String password, int mode){
        if(username.trim().length() > 0 && password.trim().length() > 0){
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("username", username);
                jsonObject.put("password", password);
                Object[] params = {jsonObject.toString(), mode};
                showProgressDialogWithTitle(getString(R.string.verifying_data));
                new RequestLoginAsyncTask(this).execute(params);
            } catch (JSONException e) {
                Log.e(TAG, "ERROR --> loginMethod --> " + e.getLocalizedMessage());
            }
        }
        else{
            Toast.makeText(this, getString(R.string.empry_pass_or_username), Toast.LENGTH_SHORT);
        }
    }

    private void loginEncryptedMethod(String username, String password){
        if(username.trim().length() > 0 && password.trim().length() > 0){
            JSONObject loginJsonData = new JSONObject();
            JSONObject loginEncryptedData = new JSONObject();
            try {
                loginJsonData.put("username", username);
                loginJsonData.put("password", password);
                String plainText = loginJsonData.toString();
                Log.i(TAG, "loginMethod  --> jsonObject plaintext" + plainText);
                String encrypted = securityModule.encrypt2(plainText);
                Log.i(TAG, "loginMethod --> Encrypted result: " + encrypted);
                loginEncryptedData.put("encrypted_data", encrypted);
                Object[] params = { loginEncryptedData.toString(), 3};
                new RequestLoginAsyncTask(this).execute(params);
            } catch (JSONException e) {
                hideProgressDialogWithTitle();
                Log.e(TAG, "ERROR --> loginMethod --> " + e.getLocalizedMessage());
            }
        }
        else{
            hideProgressDialogWithTitle();
            Toast.makeText(this, getString(R.string.empry_pass_or_username), Toast.LENGTH_SHORT);
        }

    }

    public void handleLoginResponse(String response){
        hideProgressDialogWithTitle();
        try {
            JSONObject responseJson = new JSONObject(response);
            int responseCode =  Integer.parseInt(responseJson.get("code").toString());
            Log.i(TAG, "handleResponse: " + response);
            if(responseCode == 200){
                tvBadAuth.setVisibility(View.GONE);
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            }
            else if(responseCode == 401){
                tvBadAuth.setVisibility(View.VISIBLE);
            }
            else{
                Toast.makeText(this, "Ups, something went wrong...", Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            Log.e(TAG, "handleResponse: " + e.getLocalizedMessage());
            Toast.makeText(this, "Ups, something went wrong...", Toast.LENGTH_LONG).show();
        }
    }

    public void handleLoginEncryptedResponse(String response){
        hideProgressDialogWithTitle();
        try {
            JSONObject responseJson = new JSONObject(response);
            int responseCode =  Integer.parseInt(responseJson.get("code").toString());
            Log.i(TAG, "handleLoginEncryptedResponse() --> response: " + response);
            if(responseCode == 200){
                JSONObject decryptedJsonResponse = new JSONObject(securityModule.decrypt(responseJson.get("encrypted_data").toString()));
                if((boolean) decryptedJsonResponse.get("login_correct")){
                    tvBadAuth.setVisibility(View.GONE);
                    Intent intent = new Intent(this, ProfileActivity.class);
                    startActivity(intent);
                }
                else{
                    tvBadAuth.setVisibility(View.VISIBLE);
                }
            }
            else{
                Toast.makeText(this, "Ups, something went wrong...", Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            Log.e(TAG, "handleLoginEncryptedResponse() --> ERROR:" + e.getLocalizedMessage());
            Toast.makeText(this, "Ups, something went wrong...", Toast.LENGTH_LONG).show();
        }
    }

    public void handleECDHResponse(String response){
        try {
            JSONObject responseJson = new JSONObject(response);
            int responseCode =  Integer.parseInt(responseJson.get("code").toString());
            Log.i(TAG, "handleResponse: " + response);
            if(responseCode == 200){
                JSONObject responseObjectJson = (JSONObject) responseJson.get("response");
                JSONObject serverPublicKeyJson = (JSONObject) responseObjectJson.get("server_public_key");
                BigInteger xCoordinate = new BigInteger((String) serverPublicKeyJson.get("x_coordinate"));
                BigInteger yCoordinate = new BigInteger((String) serverPublicKeyJson.get("y_coordinate"));
                boolean sharedKeyCreated = securityModule.setReceiverPublicKey(xCoordinate, yCoordinate);
                if(sharedKeyCreated){
                    Log.i(TAG, "Se genero el sharedKey entre cliente y servidor de manera correcta");
                    changeProgressDialogMessage(getString(R.string.verifying_data));
                    loginEncryptedMethod(etUsername.getText().toString(), etPassword.getText().toString());
                }
                else{
                    hideProgressDialogWithTitle();
                    Toast.makeText(this , getString(R.string.error_generating_shared_key), Toast.LENGTH_LONG);
                }
            }
            else if(responseCode == 401){
                hideProgressDialogWithTitle();
                tvBadAuth.setVisibility(View.VISIBLE);
            }
            else{
                hideProgressDialogWithTitle();
                Toast.makeText(this, "Ups, something went wrong...", Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            hideProgressDialogWithTitle();
            Log.e(TAG, "handleResponse: " + e.getLocalizedMessage());
            Toast.makeText(this, "Ups, something went wrong...", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvBadAuth.setVisibility(View.GONE);
    }
}
