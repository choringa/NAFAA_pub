package com.indi.nafaa;

import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.CertificatePinner;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Utils {

    public static final String LOGIN_SERVICE = "login";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String TAG = "Utils";
    private static final String BASE_URL = "https://192.168.0.115:5000/";
    private OkHttpClient clientSecure;
    private OkHttpClient clientUnsecure;

    public Utils(){
        ConstructSecureClient();
        ConstructUnsecureClient();
    }

    private void ConstructSecureClient(){
        // Create certificate pinner with de hash of the host to pin
        CertificatePinner certificatePinner = new CertificatePinner.Builder()
                //BURP CERT: sha256/ixl1/kHiBPoA/5+rfpC8KNQ+LqhMcIJYX/TmxlVa0OQ=
                //NAFWS CERT: sha256/UQk1OS/TekX9Cz/i+YHNPPQOU4Ux6MtiNXH9jEuatBo=
                .add("NAFWS", "sha256/UQk1OS/TekX9Cz/i+YHNPPQOU4Ux6MtiNXH9jEuatBo=")
                .build();
        try{
            TrustManager[] trustPinCert = trustedCerts();
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustPinCert, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            //Comentar la siguiente linea para deshabilitar el checkeo manual y que se haga directo por el de okhttp3
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustPinCert[0]);
            builder.hostnameVerifier(verifier());
            builder.certificatePinner(certificatePinner);
            clientSecure = builder.build();
        }
        catch (Exception e){
            Log.e(TAG, "Created without cert pin..." + e.getLocalizedMessage());
            clientSecure = new OkHttpClient();
        }
    }

    private void ConstructUnsecureClient() {
        try{
            TrustManager[] trustPinCert = trustedCerts();
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustPinCert, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            //Comentar la siguiente linea para deshabilitar el checkeo manual y que se haga directo por el de okhttp3
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustPinCert[0]);
            builder.hostnameVerifier(unsecureVerifier());
            clientUnsecure = builder.build();
        }
        catch (Exception e){
            Log.e(TAG, "Created without cert pin..." + e.getLocalizedMessage());
            clientUnsecure = new OkHttpClient();
        }
    }

    private HostnameVerifier verifier(){
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                Log.i(TAG, "Secure Verifier --> Verifying hostname: " + hostname);
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    javax.security.cert.X509Certificate cert = session.getPeerCertificateChain()[0];
                    byte[] publicKey = cert.getPublicKey().getEncoded();
                    md.update(publicKey,0,publicKey.length);
                    String pin = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
                    Log.i(TAG,"2 >> PIN------------->" + pin);
                    String pinned = clientSecure.certificatePinner().getPins().toArray()[0].toString();
                    Log.i(TAG,"3 >> Pinned---------->" + pinned);
                    return pinned.contains(pin);
                } catch (SSLPeerUnverifiedException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        };
    }

    private HostnameVerifier unsecureVerifier(){
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                Log.i(TAG, "unsecureVerifier--> " + hostname);
                return true;
            }
        };
    }

    private TrustManager[] trustedCerts(){
        // Create a trust manager that does not validate certificate chains
        return new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        Log.i(TAG, "Client trusted check--->" + chain[0] + " -- " + authType);
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        try {
                            MessageDigest md = MessageDigest.getInstance("SHA-256");
                            X509Certificate cert = chain[0];
                            byte[] publicKey = cert.getPublicKey().getEncoded();
                            md.update(publicKey,0,publicKey.length);
                            String pin = Base64.encodeToString(md.digest(),
                                    Base64.NO_WRAP);
                            Log.i(TAG, "First time check server certificate PIN------------->" + pin);
                        } catch (NoSuchAlgorithmException e) {
                            Log.e(TAG, Objects.requireNonNull(e.getLocalizedMessage()));
                        }
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        Log.i(TAG, "1 <<-----------");
                        return new X509Certificate[]{};
                    }
                }
        };
    }

    public String makePostRequest(String service, String json, boolean secure) {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + service)
                .post(body)
                .build();
        Log.i(TAG, "Request --> " + request.toString());
        if(secure){
            Log.i(TAG, "request is Secure!!");
            try (Response response = clientSecure.newCall(request).execute()) {
                return response.body().string();
            }
            catch (IOException e){
                Log.e(TAG, "Error --> " + e.getLocalizedMessage() + e.getMessage());
                return "Connection Error";
            }
        }
        else{
            Log.i(TAG, "Request is Unsecure!!");
            try (Response response = clientUnsecure.newCall(request).execute()) {
                return response.body().string();
            }
            catch (IOException e){
                Log.e(TAG, "Error --> " + e.getLocalizedMessage() + e.getMessage());
                return "Connection Error";
            }
        }
    }

}
