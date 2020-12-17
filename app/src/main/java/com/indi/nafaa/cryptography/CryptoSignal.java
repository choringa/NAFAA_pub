package com.indi.nafaa.cryptography;


import android.content.Context;
import android.util.Log;

import com.indi.nafaa.libsignalclient.MyIdentityKeyStore;
import com.indi.nafaa.libsignalclient.MyPreKeyStore;
import com.indi.nafaa.libsignalclient.MySessionStore;
import com.indi.nafaa.libsignalclient.MySignedPreKeyStore;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.SessionBuilder;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.PreKeyStore;
import org.whispersystems.libsignal.state.SessionStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyStore;
import org.whispersystems.libsignal.util.KeyHelper;

import java.io.UnsupportedEncodingException;
import java.util.List;


public class CryptoSignal {

    private static final String TAG = "CryptoSignal";
    private Context context;

    //Constructor
    public CryptoSignal(Context context){
        this.context = context;
        SignalProtocol();
    }

    private void SignalProtocol(){
        try {
            //Inicializacion
            Log.i(TAG, "SignalProtocol ---> INIT");
            IdentityKeyPair identityKeyPair = KeyHelper.generateIdentityKeyPair();
            Log.i(TAG, "identityKey --> Public: " + identityKeyPair.getPublicKey().getPublicKey().serialize());
            Log.i(TAG, "identityKey --> Private: " + identityKeyPair.getPrivateKey().serialize().toString());
            int registrationId  = KeyHelper.generateRegistrationId(false);
            Log.i(TAG, "SignalProtocol ---> identityKeyPair: " + identityKeyPair + "; registrationId: " + registrationId);


            //List<PreKeyRecord> preKeys         = KeyHelper.generatePreKeys(startId, 100);
            List<PreKeyRecord> preKeys         = KeyHelper.generatePreKeys(registrationId, 100);
            SignedPreKeyRecord signedPreKey    = KeyHelper.generateSignedPreKey(identityKeyPair, 5);

            for (int i = 0; i < preKeys.size(); i++) {
                PreKeyRecord temp = preKeys.get(i);
                Log.i(TAG, "WTF-->" + temp.toString());
            }


            //Building Session
            SessionStore sessionStore = new MySessionStore();
            PreKeyStore preKeyStore = new MyPreKeyStore(context);
            SignedPreKeyStore signedPreKeyStore = new MySignedPreKeyStore(context);
            IdentityKeyStore identityStore = new MyIdentityKeyStore(context);


            //WTF thinks
            SignalProtocolAddress signalProtocolAddress = new SignalProtocolAddress("device", 1);
            //ECPublicKey publicKey = (ECPublicKey) preKeyStore.loadPreKey(2).getKeyPair().getPublicKey();

            //Poblar bases
            Log.i(TAG, "SignalProtocol --> signedPreKey: " + signedPreKey);
            //preKeyStore.storePreKey();
            signedPreKeyStore.storeSignedPreKey(5, signedPreKey);
            identityStore.saveIdentity(signalProtocolAddress, identityKeyPair.getPublicKey());


            // Instantiate a SessionBuilder for a remote recipientId + deviceId tuple.
            //SessionBuilder sessionBuilder = new SessionBuilder(sessionStore, preKeyStore, signedPreKeyStore, identityStore, recipientId, deviceId);
            SessionBuilder sessionBuilder = new SessionBuilder(sessionStore, preKeyStore, signedPreKeyStore, identityStore, signalProtocolAddress);

            // RE WTF
            PreKeyBundle retrievedPreKey = new PreKeyBundle(registrationId, 1, 2, preKeyStore.loadPreKey(2).getKeyPair().getPublicKey(),
                    3, signedPreKeyStore.loadSignedPreKey(3).getKeyPair().getPublicKey(),
                    signedPreKeyStore.loadSignedPreKey(3).getSignature(), identityStore.getIdentity(signalProtocolAddress));

            // Build a session with a PreKey retrieved from the server.
            sessionBuilder.process(retrievedPreKey);

            //Original que no sirve para verga
            //SessionCipher sessionCipher = new SessionCipher(sessionStore, recipientId, deviceId);
            SessionCipher sessionCipher = new SessionCipher(sessionStore,preKeyStore,signedPreKeyStore,identityStore,signalProtocolAddress);
            CiphertextMessage message = sessionCipher.encrypt("Hello world!".getBytes("UTF-8"));
            Log.i(TAG, "SignalProtocol--> message:" + message);

        } catch (UntrustedIdentityException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidKeyIdException e) {
            e.printStackTrace();
        }
    }
}
