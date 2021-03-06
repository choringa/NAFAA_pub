package com.indi.nafaa.cryptography;

import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class SecurityModule {

    private final static String TAG = "SecurityModule";

    private final static String ENCRYPTION_ALGORITHM = "AES";
    private final static String KEY_PAIR_ALGORITHM = "EC"; //Elliptic curves
    private final static String KEY_AGREEMENT_ALGORITHM = "ECDH"; //Elliptic curves Diffie-Hellman

    private ECPublicKey clientECPublicKey;
    private ECPrivateKey clientECPrivateKey;

    KeyAgreement keyAgreement;
    byte[] sharedSecret;

    /**
     * Constructor
     */
    public SecurityModule() {
        initClientKeys();
    }

    /**
     * Metodo que inicia las llaves publica y privada del cliente e inicia el KeyAgreement con la llave privada para generar posteriormente el secreo compartido.
     */
    private void initClientKeys() {
        Log.i(TAG, "initClientKeys() --> Generando llaves EC Cliente");
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM);
            ECGenParameterSpec spec = new ECGenParameterSpec("prime256v1");
            kpg.initialize(spec);
            KeyPair kp = kpg.generateKeyPair();
            clientECPrivateKey = (ECPrivateKey) kp.getPrivate();
            clientECPublicKey = (ECPublicKey) kp.getPublic();
            Log.i(TAG, "makeKeyExchangeParams() --> ecPublicKey --> X: " + clientECPublicKey.getW().getAffineX() + ", Y: " + clientECPublicKey.getW().getAffineY());
            Log.i(TAG, "makeKeyExchangeParams() --> Llaves cliente generadas; publica: " + kp.getPublic().toString() + "; privada: " + kp.getPrivate().toString());
            keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM);
            keyAgreement.init(kp.getPrivate());
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            Log.e(TAG, "ERROR --> makeKeyExchangeParams() --> inicializando llaves: " + e.getLocalizedMessage());
        }
    }

    /**
     * Metodo que recibe la llave publica del servidor y completa el proceso de KeyAgreement para generar la llave compartida.
     * @param x Coordenada X de la llave publica del Servidor
     * @param y Coordenada Y de la llave publica del Servidor
     * @return true si es posible genrar la llave compartida; false caso contrario.
     */
    public boolean setReceiverPublicKey(BigInteger x, BigInteger y) {
        try {
            ECParameterSpec ecParameterSpec = clientECPublicKey.getParams();
            ECPoint ecPoint = new ECPoint(x, y);
            ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec (ecPoint, ecParameterSpec);
            KeyFactory kfa = KeyFactory.getInstance(KEY_PAIR_ALGORITHM);
            ECPublicKey serverPublicKey = (ECPublicKey) kfa.generatePublic(ecPublicKeySpec);
            keyAgreement.doPhase(serverPublicKey, true);
            sharedSecret = keyAgreement.generateSecret();

            Log.i(TAG, "setReceiverPublicKey --> Se recibe llave publica del servidor X: " + serverPublicKey.getW().getAffineX() + "; Y: " + serverPublicKey.getW().getAffineY());
            Log.i(TAG, "setReceiverPublicKey --> EXP: " + byteKeyToHex(keyAgreement.generateSecret(ENCRYPTION_ALGORITHM).getEncoded()));
            Log.i(TAG, "setReceiverPublicKey --> Se recibe llave publica del servidor, generando secreto compartido: " + sharedSecret.toString());
            Log.i(TAG, "setReceiverPublicKey --> Generando llave con secreto compartido: " + sharedSecret.toString() + "; encoded:" + Base64.encodeToString(sharedSecret, Base64.DEFAULT) + "; lenght: " + sharedSecret.length);
            Log.i(TAG, "setReceiverPublicKey --> SHARED KEY HEX =========>: " + byteKeyToHex(sharedSecret));

            return true;
        } catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "ERROR --> setReceiverPublicKey() --> generando sharedSecret: " + e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * Metodo que encrypta un texto plano que recibe como parametro
     * @param msg el texto plano a encriptar
     * @return el texto cifrado en un String
     */
    public String encrypt(String msg) {
        try {
            Log.i(TAG, "encrypt2() --> Encriptando texto:" + msg);
            Key key = new SecretKeySpec(sharedSecret, ENCRYPTION_ALGORITHM);
            Log.i(TAG, "encrypt2() --> key data: ALGO: "+  key.getAlgorithm() + "; Format: " + key.getFormat() + "; keyEncoded: " + key.getEncoded());
            Cipher c = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            Log.i(TAG, "encrypt2() --> blocksize: " +  c.getBlockSize() + "; KEY: " + byteKeyToHex(key.getEncoded()) + "; KEY BYTES length: " +  key.getEncoded().length);
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] original = Base64.encode(c.doFinal(msg.getBytes("ascii")), Base64.DEFAULT);
            return new String(original);
        } catch (BadPaddingException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException e) {
            Log.e(TAG, "ERROR --> encrypt(s) --> encriptando datos: " + e.getLocalizedMessage());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return msg;
    }

    /**
     * Metodo que se usa para desencriptar un texto cifrado
     * @param encryptedData el texto cifrado a desencriptar
     * @return el texto plano en un String
     */
    public String decrypt(String encryptedData) {
        try {
            Log.i(TAG, "decrypt() --> Desencriptando texto:" + encryptedData);
            Key key = new SecretKeySpec(sharedSecret, ENCRYPTION_ALGORITHM);
            Cipher c = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, key);
            byte[] x = encryptedData.getBytes();
            Log.i(TAG, "decrypt() --> x: " + x);
            byte[] decodedValue = Base64.decode(encryptedData, Base64.DEFAULT);
            Log.i(TAG, "decrypt() --> decodedValue: " + decodedValue);
            byte[] decValue = c.doFinal(decodedValue);
            Log.i(TAG, "decrypt() --> decrypted: " + new String(decValue));
            return new String(decValue);
        } catch (BadPaddingException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException e) {
            Log.e(TAG, "ERROR --> decrypt() --> decifrando mensaje encriptado: " + e.getLocalizedMessage());
        }
        return encryptedData;
    }

    /**
     * Metodo que devuelve la llave publica del servidor
     * @return la llave publica del servidor
     */
    public ECPublicKey getClientECPublicKey() {
        return clientECPublicKey;
    }

    /**
     * Metodo que convierte de byte array a hexadecimal
     * @param bytes el byte array a convertir
     * @return el hex en un string
     */
    private static String byteKeyToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
