package com.example.smartcontractfvss;


import java.io.*;
import java.security.*;

//RSA加密包
public class RSACryptoExample {

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        // 生成RSA密钥对
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(1024);
        return keyPairGen.generateKeyPair();
    }

    static byte[] serialize(Object object) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return baos.toByteArray();
        }
    }

    static <T> T deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bias = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bias)) {
            return (T) ois.readObject();
        }
    }
}