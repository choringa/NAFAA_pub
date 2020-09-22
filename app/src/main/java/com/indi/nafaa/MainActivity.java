package com.indi.nafaa;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
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

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    private TextView tvBadAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Not today...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        final EditText etUsername = findViewById(R.id.et_main_username);
        final EditText etPassword = findViewById(R.id.et_main_password);
        tvBadAuth = findViewById(R.id.tvBadAuth);
        Button btnLoginSecure = findViewById(R.id.btnSecureLogin);
        btnLoginSecure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginMethod(etUsername.getText().toString(), etPassword.getText().toString(), true);
            }
        });

        Button btnLoginUnsecure = findViewById(R.id.btnUnsecureLogin);
        btnLoginUnsecure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginMethod(etUsername.getText().toString(), etPassword.getText().toString(), false);
            }
        });
    }

    public void loginMethod(String username, String password, boolean secure){
        Utils utils = new Utils();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", username);
            jsonObject.put("password", password);
            Object[] params = {utils, jsonObject.toString(), secure};
            new RequestAsyncTask(this).execute(params);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void handleResponse(String response){
        try {
            JSONObject responseJson = new JSONObject(response);
            int responseCode =  Integer.parseInt(responseJson.get("code").toString());
            Log.i(TAG, "handleResponse: " + response);
            if(responseCode == 200){
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
    }
}
