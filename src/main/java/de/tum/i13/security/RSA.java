package de.tum.i13.security;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.Cipher;

public class RSA implements Serializable{
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public RSA(){
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            KeyPair pair = generator.generateKeyPair();
            privateKey = pair.getPrivate();
            publicKey = pair.getPublic();
            System.out.println(Base64.getEncoder().encodeToString(publicKey.getEncoded()));

        } catch (Exception ignored) {}
    }
    private static String encode(byte[] data){
        return Base64.getEncoder().encodeToString(data);
    }
    private static byte[] decode(String data){
        return Base64.getDecoder().decode(data);
    }
    public static String encrypt(String message, PublicKey pk) throws Exception{
        byte[] messageToBytes = message.getBytes();
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE,pk);
        byte[] encryptedBytes = cipher.doFinal(messageToBytes);
        return encode(encryptedBytes);
    }

    public static String decrypt(String encryptedMessage, PrivateKey pv) throws Exception{
        byte[] encryptedBytes = decode(encryptedMessage);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE,pv);
        byte[] decryptedMessage = cipher.doFinal(encryptedBytes);
        return new String(decryptedMessage,"UTF8");
    }
    public PrivateKey getPrivateKey() {
        return privateKey;
    }
    public PublicKey getPublicKey() {
        return publicKey;
    }
}
