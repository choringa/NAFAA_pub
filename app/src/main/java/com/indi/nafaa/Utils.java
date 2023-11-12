package com.indi.nafaa;

import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.List;
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

import com.indi.nafaa.libsignalclient.MyIdentityKeyStore;
import com.indi.nafaa.libsignalclient.MyPreKeyStore;
import com.indi.nafaa.libsignalclient.MySessionStore;
import com.indi.nafaa.libsignalclient.MySignedPreKeyStore;

public class Utils {

    public static final String LOGIN_SERVICE = "login";
    public static final String LOGIN_ENCRYPTED_SERVICE = "login_encrypted";
    public static final String ECDH_SERVICE = "ecdh";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String TAG = "Utils";
    //private static final String BASE_URL = "https://34.196.140.186:5000/"; //AWS 1
    //private static final String BASE_URL = "https://3.138.221.39:5000/"; //AWS 2
    private static String BASE_URL = ""; //Local
    private OkHttpClient clientSecure;
    private OkHttpClient clientUnsecure;

    public Utils(String host){
        if (host != null || !host.equals("")){
            Log.i(TAG,"Using host: " + host);
            BASE_URL = host;
        }
        else {
            Log.i(TAG, "Using default host");
            BASE_URL = "https://192.168.0.1:5000/";
        }
        ValidateURI();
        ConstructSecureClient();
        ConstructUnsecureClient();
    }

    private void ValidateURI(){
        if(!BASE_URL.contains("http://") && !BASE_URL.contains("https://")){
            BASE_URL = "https://" + BASE_URL;
        }
        if(!BASE_URL.substring(BASE_URL.length() - 1).equals("/")){
            BASE_URL = BASE_URL + "/";
        }
        Log.i(TAG, "Validated BASE URI: " + BASE_URL);
    }

    private void ConstructSecureClient(){
        // Create certificate pinner with de hash of the host to pin
        CertificatePinner certificatePinner = new CertificatePinner.Builder()
                //.add("NAFWS", "sha256/ixl1/kHiBPoA/5+rfpC8KNQ+LqhMcIJYX/TmxlVa0OQ=") //BURP PIN CERT
                .add("NAFWS", "sha256/UQk1OS/TekX9Cz/i+YHNPPQOU4Ux6MtiNXH9jEuatBo=") //NAFWS PIN CERT
                .build();
        try{
            Log.i(TAG, "ConstructSecureClient-->1");
            TrustManager[] trustPinCert = secureTrustedCerts();
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustPinCert, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustPinCert[0]);
            builder.hostnameVerifier(verifier());
            builder.certificatePinner(certificatePinner);
            clientSecure = builder.build();
        }
        catch (NoClassDefFoundError defFoundError){
            //HOOKED
            clientSecure = new OkHttpClient();
        }
        catch (Exception e){
            Log.e(TAG, "Created without cert pin..." + e.getLocalizedMessage());
            clientSecure = new OkHttpClient();
        }
    }

    private void ConstructUnsecureClient() {
        try{
            TrustManager[] unsecureTrustManager = unsecureTrustedCerts();
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, unsecureTrustManager, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)unsecureTrustManager[0]);
            builder.hostnameVerifier(unsecureVerifier());
            clientUnsecure = builder.build();
        }
        catch (Exception e){
            Log.e(TAG, "Creating default client." + e.getLocalizedMessage());
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
                catch (Exception e){
                    Log.e(TAG, "reboom: " + e.getLocalizedMessage());
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

    private TrustManager[] secureTrustedCerts(){
        // Create a trust manager that does not validate certificate chains
        return new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        Log.i(TAG, "Client trusted check--->" + chain[0] + " -- " + authType);
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        Log.i(TAG, "secureTrustManager-->: VALIDATE INIT");
                        try {
                            Log.i(TAG, "UnsecureTrustManager--> Incomming cert hash:" + chain[0]);
                            MessageDigest md = MessageDigest.getInstance("SHA-256");
                            X509Certificate cert = chain[0];
                            byte[] publicKey = cert.getPublicKey().getEncoded();
                            md.update(publicKey,0,publicKey.length);
                            String pin = Base64.encodeToString(md.digest(),
                                    Base64.NO_WRAP);
                            Log.i(TAG, "TrustManager --> Certificate hash recived PIN------------->" + pin);

                            String pinned = clientSecure.certificatePinner().getPins().toArray()[0].toString();
                            Log.i(TAG,"TrustManager --> Pinned Certificate ---------->" + pinned);

                            if(pinned.contains(pin)){
                                Log.i(TAG, "TrustManager --> Pinned hash and recived hash are equal!!");
                            }
                            else{
                                throw new CertificateException("TrustManager --> Certificates hashes are not equal. The certificate hash recived from endpoint is: " + pin + ".\n The Certificate hash pinned is: " + pinned);
                            }
                        } catch (NoSuchAlgorithmException e) {
                            Log.e(TAG, Objects.requireNonNull(e.getLocalizedMessage()));
                        }
                        catch (CertificateException e){
                            Log.e(TAG, e.getLocalizedMessage());
                        }
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        //Log.i(TAG, "1 <<-----------" + certificates.length);
                        return new X509Certificate[]{};
                    }
                }
        };
    }

    private TrustManager[] unsecureTrustedCerts() {
        // Create a trust manager that does not validate certificate chains
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        Log.i(TAG, "Client trusted check--->" + chain[0] + " -- " + authType);
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                       Log.i(TAG, "UnsecureTrustManager-->: NOTHING TO VALIDATE --> Incomming cert hash:" + chain[0]);
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        //Log.i(TAG, "1 <<-----------");
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
        Log.i(TAG, "makePostRequest --> Request --> " + request.toString());
        if(secure){
            Log.i(TAG, "request is Secured with SSLPinning!!");
            try (Response response = clientSecure.newCall(request).execute()) {
                return response.body().string();
            }
            catch (IOException e){
                Log.e(TAG, "makePostRequest --> Error --> " + e.getLocalizedMessage() + e.getMessage());
                return "Connection Error";
            }
        }
        else{
            Log.i(TAG, "makePostRequest --> Request is Unsecure without SSL Pinning!!");
            try (Response response = clientUnsecure.newCall(request).execute()) {
                return response.body().string();
            }
            catch (IOException e){
                Log.e(TAG, "makePostRequest --> Error --> " + e.getLocalizedMessage() + e.getMessage());
                return "Connection Error";
            }
        }
    }
}
