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
    private EditText etServerHostnameUri;
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
        etServerHostnameUri = findViewById(R.id.et_main_hostname);
        tvBadAuth = findViewById(R.id.tvBadAuth);
        Button btnLoginSecure = findViewById(R.id.btnSecureLogin);
        btnLoginSecure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginMethod(etUsername.getText().toString(), etPassword.getText().toString(), etServerHostnameUri.getText().toString(),1);
            }
        });

        Button btnLoginUnsecure = findViewById(R.id.btnUnsecureLogin);
        btnLoginUnsecure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginMethod(etUsername.getText().toString(), etPassword.getText().toString(), etServerHostnameUri.getText().toString(),2);
            }
        });
        Button btnEncryptedLogin = findViewById(R.id.btnEncryptedLogin);
        btnEncryptedLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestSharedKey(etServerHostnameUri.getText().toString());
            }
        });
    }
    private void changeProgressDialogMessage(String newMessage){
        if(progressDialog != null && progressDialog.isShowing())
            progressDialog.setMessage(newMessage);
        else
            showProgressDialogWithTitle(newMessage);
    }

    /**
     * Metodo que muestra el progressDialog dado un mensaje como parametro
     * @param substring el mensaje a mostrar en el progressDialog
     */
    private void showProgressDialogWithTitle(String substring) {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(substring);
        progressDialog.show();
    }

    /**
     * Metodo que esconde el progressDialog
     */
    private void hideProgressDialogWithTitle() {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.dismiss();
    }

    /**
     * Metodo que solicita la llave publica al servidor y envia la llave publica del cliente al servidor para poder genera la llave compartida con el modulo de securityModule
     */
    private void requestSharedKey(String host) {
        ECPublicKey ecPublicKey = securityModule.getClientECPublicKey();
        JSONObject jsonObjectPublicKey = new JSONObject();
        JSONObject jsonObjectPublicKeyPoint = new JSONObject();
        try {

            jsonObjectPublicKeyPoint.put("x_coordinate", ecPublicKey.getW().getAffineX());
            jsonObjectPublicKeyPoint.put("y_coordinate", ecPublicKey.getW().getAffineY());
            jsonObjectPublicKey.put("public_key", jsonObjectPublicKeyPoint);

            Log.i(TAG, "requestSharedKey --> jsonObject: " + jsonObjectPublicKey.toString());
            Object[] params = {jsonObjectPublicKey.toString(), host};
            showProgressDialogWithTitle(getString(R.string.generating_shared_secret));
            new RequestSharedKeyAsyncTask(this).execute(params);
        }
        catch (JSONException e){
            Log.e(TAG, "ERROR --> loginMethod --> " + e.getLocalizedMessage());
        }
    }

    /**
     * Metodo de login general
     * @param username username
     * @param password password
     * @param mode 1 = secure (Con SSL Pinning), 2 = insecure
     */
    private void loginMethod(String username, String password, String host, int mode){
        if(username.trim().length() > 0 && password.trim().length() > 0){
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("username", username);
                jsonObject.put("password", password);
                Object[] params = {jsonObject.toString(), mode, host};
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

    /**
     * Metodo de login con informacion cifrada
     * @param username username
     * @param password password
     */
    private void loginEncryptedMethod(String username, String password, String host){
        if(username.trim().length() > 0 && password.trim().length() > 0){
            JSONObject loginJsonData = new JSONObject();
            JSONObject loginEncryptedData = new JSONObject();
            try {
                loginJsonData.put("username", username);
                loginJsonData.put("password", password);
                String plainText = loginJsonData.toString();
                Log.i(TAG, "loginMethod  --> jsonObject plaintext" + plainText);
                String encrypted = securityModule.encrypt(plainText);
                Log.i(TAG, "loginMethod --> Encrypted result: " + encrypted);
                loginEncryptedData.put("encrypted_data", encrypted);
                Object[] params = { loginEncryptedData.toString(), 3, host};
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

    /**
     * Metodo que procesa la informacion de respuesta del servidor para el login de los vectores 1 y 2
     * @param response la respuesta del servidor
     */
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

    /**
     * Metodo que procesa la informacion de respuesta del servidor. Aca la informacion llega cifrada.
     * @param response la respuesta del servidor
     */
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

    /**
     * Metodo que procesa la informacion recibida del servidor. Aca llega la llave publica del servidor para poder ser procesada y generar la llave compartida.
     * De ser posible crear la llave compartida entre el servidor y el cliente envia la info de login para ser encriptada y enviada al servidor.
     * @param response La respuesta del servidor que son las coordenadas del punto en la curva eliptica que representa la llave publica del servidor.
     */
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
                    loginEncryptedMethod(etUsername.getText().toString(), etPassword.getText().toString(), etServerHostnameUri.getText().toString());
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
