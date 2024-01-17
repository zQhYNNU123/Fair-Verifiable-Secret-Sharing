package com.example.smartcontractfvss;

import org.apache.commons.lang3.tuple.Pair;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.SerializationUtils.deserialize;
import static org.apache.commons.lang3.SerializationUtils.serialize;

public class Main {

    static int n = FVSS.n; //参与者数量
    static final BigInteger p = FVSS.p;//大素数
    static final BigInteger g= FVSS.g;//群GF(p)的生成元g
    static final int t = FVSS.t; //门限值设置

    static final BigInteger secret = FVSS.secret;//设置初始秘密值

    static int maliciousPeer = FVSS.maliciousPeer; // 恶意节点数量
    static List<Integer> isMalicious = new ArrayList<>(); // 恶意节点列表

    //主函数
    public static void main(String[] args) throws Exception {

        //创建空数组，用来存储影子秘密份额
        BigInteger[] sub_Xi_values;
        BigInteger[] sub_SXi_values;

        System.out.println("\n****************************************⭐秘密分发阶段⭐****************************************");
        //秘密分发阶段
        Pair<BigInteger[], BigInteger[]> distributeResults = FVSS.distributeSecret(secret);
        sub_Xi_values = distributeResults.getLeft();
        sub_SXi_values = distributeResults.getRight();
        BigInteger[] sub_Ci_values = new BigInteger[sub_SXi_values.length];//存放承诺值
        BigInteger[] sub_Vi_values = new BigInteger[sub_SXi_values.length];//存放验证报文

        //打印分发的影子数和影子秘密份额
        System.out.println("影子数：");
        for (int i = 0; i < n; i++) {
            System.out.println("sub_Xi_values[" + i + "]: " + sub_Xi_values[i]);
        }

        System.out.println("影子秘密份额：");
        for (int i = 0; i < n; i++) {
            System.out.println("sub_SXi_values[" + i + "]: " + sub_SXi_values[i]);
        }

        System.out.println("影子秘密对应的承诺值：");
        for (int i = 0; i < n; i++) {
            BigInteger Ci = g.modPow(sub_SXi_values[i], p);
            sub_Ci_values[i] = Ci;
            System.out.println("sub_Ci_values[" + i + "]: " + sub_Ci_values[i]);
        }

        System.out.println("影子秘密对应的验证报文：");
        for (int i = 0; i < n; i++) {
            BigInteger Vi = FVSS.hash(sub_Xi_values[i].toString() + sub_SXi_values[i].toString() + sub_Ci_values[i].toString());
            sub_Vi_values[i] = Vi;
            System.out.println("sub_Vi_values[" + i + "]: " + sub_Vi_values[i]);
        }

        //可选择代码开始的地方开始测开始时间
        long startTime = System.currentTimeMillis();

        System.out.println("\n*************************************⭐影子秘密份额加解密阶段⭐***********************************");
        //这里可以选择更换不同的加密算法，如果选用这个加密算法记得把另外一个注释掉，选用另外一个加密算法记得把这个注释掉

        //Elgamal加密算法
//        BigInteger alpha = FVSS.g;
//        BigInteger d = new BigInteger("7"); //私钥sk
//        elgamal elGamalDemo = new elgamal(p,alpha,d);
//        BigInteger k = new BigInteger("7");
//
//        //将获取的影子秘密份额存储进入明文数组
//        BigInteger[] plaintext_sub_Xi = distributeResults.getLeft();
//        BigInteger[] plaintext_sub_SXi = distributeResults.getRight();
//
//        //创建数组存储同态加密后的密文
//        BigInteger[][] encrypted_sub_X = new BigInteger[plaintext_sub_Xi.length][plaintext_sub_Xi.length];
//        BigInteger[][] encrypted_sub_SX = new BigInteger[plaintext_sub_SXi.length][plaintext_sub_SXi.length];
//        //创建数组存储同态解密后的明文
//        BigInteger[] decrypted_sub_X = new BigInteger[plaintext_sub_Xi.length];
//        BigInteger[] decrypted_sub_SX = new BigInteger[plaintext_sub_SXi.length];
//
//        for (int i = 0; i < plaintext_sub_Xi.length; i++) {
//            //加密
//            encrypted_sub_X[i] = elGamalDemo.encrypt(plaintext_sub_Xi[i], k);
//            encrypted_sub_SX[i] = elGamalDemo.encrypt(plaintext_sub_SXi[i], k);
//            //解密
//            decrypted_sub_X[i] = elGamalDemo.decrypt(encrypted_sub_X[i]);
//            decrypted_sub_SX[i] = elGamalDemo.decrypt(encrypted_sub_SX[i]);
//            System.out.println("解密后X["+i+"]明文："+decrypted_sub_X[i]);
//            System.out.println("解密后SX["+i+"]明文："+decrypted_sub_SX[i]);
//        }

        //RSA加密算法
        // 生成RSA密钥对
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(1024);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // 创建加密器和解密器
        Cipher encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        Cipher decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);

        //将获取的影子秘密份额存储进入明文数组
        BigInteger[] plaintext_sub_Xi = distributeResults.getLeft();
        BigInteger[] plaintext_sub_SXi = distributeResults.getRight();

         //打印加密前的明文
        System.out.println("\nRSA加密前的影子秘密份额明文");
        System.out.println("plaintext_sub_Xi=" + Arrays.toString(plaintext_sub_Xi));
        System.out.println("plaintext_sub_SXi=" + Arrays.toString(plaintext_sub_SXi));

        // 生成对称密钥
        Pair<BigInteger[], BigInteger[]> dataPair = Pair.of(plaintext_sub_Xi, plaintext_sub_SXi);

        // 将数据转换为字节数组
        byte[] dataBytes = serialize(dataPair);

        // 分块加密
        int keySize = ((RSAKey) publicKey).getModulus().bitLength();
        int maxBlockSize = keySize / 8 - 11; // RSA加密的最大块大小
        int blocks = (int) Math.ceil((double) dataBytes.length / maxBlockSize);
        ByteArrayOutputStream encryptedDataStream = new ByteArrayOutputStream();

        for (int i = 0; i < blocks; i++) {
            int offset = i * maxBlockSize;
            int length = Math.min(maxBlockSize, dataBytes.length - offset);
            byte[] block = new byte[length];
            System.arraycopy(dataBytes, offset, block, 0, length);
            byte[] encryptedBlock = encryptCipher.doFinal(block);
            encryptedDataStream.write(encryptedBlock);
        }

        byte[] encryptedData = encryptedDataStream.toByteArray();

        //打印密文数据为十六进制数
        System.out.println("RSA加密后影子秘密份额密文数据：");
        for (byte b : encryptedData) {
            System.out.print(Integer.toHexString(b & 0xFF));
        }

        // 分块解密
        ByteArrayOutputStream decryptedDataStream = new ByteArrayOutputStream();
        for (int i = 0; i < blocks; i++) {
            int offset = i * (keySize / 8);
            int length = Math.min(keySize / 8, encryptedData.length - offset);
            byte[] block = new byte[length];
            System.arraycopy(encryptedData, offset, block, 0, length);
            byte[] decryptedBlock = decryptCipher.doFinal(block);
            decryptedDataStream.write(decryptedBlock);
        }
        byte[] decryptedData = decryptedDataStream.toByteArray();

        // 将解密后的字节数组还原为Pair对象
        Pair<BigInteger[], BigInteger[]> decryptedPair = deserialize(decryptedData);
        BigInteger[] decrypt_sub_Xi = decryptedPair.getLeft();
        BigInteger[] decrypt_sub_SXi = decryptedPair.getRight();

        // 输出解密后的结果
        System.out.println("\nRSA解密后的影子秘密份额明文");
        System.out.println("decryptSub_Xi=" + Arrays.toString(decrypt_sub_Xi));
        System.out.println("decryptSub_SXi=" + Arrays.toString(decrypt_sub_SXi));

        //判断加解密前后明文内容是否一致
        if (Arrays.equals(plaintext_sub_Xi, decrypt_sub_Xi) && Arrays.equals(plaintext_sub_SXi, decrypt_sub_SXi)) {
            System.out.println("加解密结果：数据有效！加解密前后内容一致!");
        } else {
            System.out.println("加解密结果：数据无效！加解密前后内容不一致!");
        }

        //用解密后的数据进行计算承诺值，用解密后的数据进行计算验证报文：
        System.out.println("\n解密后影子秘密对应的承诺值：");
        BigInteger[] de_Ci_values = new BigInteger[n];
        for (int i = 0; i < n; i++) {
            BigInteger de_Ci = g.modPow(decrypt_sub_SXi[i], p);
            de_Ci_values[i] = de_Ci;
            System.out.println("decryptCiValues[" + i + "]: " + de_Ci_values[i]);
        }

        System.out.println("解密后影子秘密对应的验证报文：");
        BigInteger[] de_Vi_values = new BigInteger[n];
        for (int i = 0; i < n; i++) {
            BigInteger de_Vi = FVSS.hash(decrypt_sub_Xi[i].toString() + decrypt_sub_SXi[i].toString() + de_Ci_values[i].toString());
            de_Vi_values[i] = de_Vi;
            System.out.println("decryptViValues[" + i + "]: " + de_Vi_values[i]);
        }

        System.out.println("\n*************************************⭐本地协议与智能合约交互⭐***********************************");
        //智能合约验证报文：
        //System.out.println("协议与智能合约交互情况:");
        //打印智能合约接收的验证报文
        BigInteger[] sc_Vi_values = FVSS.sendSharesToSmartContract(decrypt_sub_Xi, decrypt_sub_SXi, de_Ci_values);

        //智能合约验证情况：
        boolean scVerifyResult = false;
        for (int i = 0; i < sub_Vi_values.length; i++) {
            scVerifyResult = FVSS.getVerificationResultsFromSmartContract(de_Vi_values, sub_Vi_values);
        }
        System.out.println("\n智能合约验证结果：" + scVerifyResult);
        if (scVerifyResult) {
            System.out.println("智能合约验证通过！");
        } else {
            System.out.println("智能合约验证失败！");
        }

        //测试数据是否成功提交到智能合约,打印日志信息
        FVSS logs = new FVSS();
        FVSS.sendSharesToSmartContract(sub_Xi_values, sub_SXi_values, sub_Ci_values);
        System.out.println(logs);

        System.out.println("\n*************************************⭐影子秘密份额验证阶段⭐***********************************");
        // 份额验证＋恶意节点检查**************************
        System.out.println("子秘密份额验证结果：");
        for (int i = 0; i < n; i++) {
            //验证报文信息：
            if (!de_Vi_values[i].equals(sub_Vi_values[i])){
                // 广播验证报文错误信息
                System.out.println("Peer" + i + "报文消息验证失败！请在规定时间内向智能合约提交核验请求！");
                //调用智能合约验证结果
                if(!scVerifyResult){
                    System.out.println("Peer" + i + "报文消息验证失败！请在规定时间内重新广播相关报文！");
                }else {
                    System.out.println("Peer" + i + "的报文信息在智能合约中验证通过！");
                }
            }else {
                System.out.println("Peer" + i + "报文消息验证通过！");
            }
        }

        //获取验证通过的子秘密份额
        //调用验证函数获取经验证过后返回的子秘密份额二元组(xi,sxi)，用一个二元组verificationResult去存储
        Pair<List<BigInteger>, List<BigInteger>> verificationResult = FVSS.verifyShares(decrypt_sub_Xi, decrypt_sub_SXi, sub_Vi_values, de_Vi_values, de_Ci_values);
        //分别打印子秘密份额二元组的结果
        List<BigInteger> verified_xi_result = verificationResult.getLeft();//获取已经验证过的xi;
        List<BigInteger> verified_sxi_result = verificationResult.getRight();//获取已经验证过的sxi；

        System.out.println("\n****************************************⭐秘密重构密阶段⭐**************************************");
        //秘密重构阶段
        List<BigInteger> verified_xi_values_ls = new ArrayList<>();
        List<BigInteger> verified_sxi_values_ls = new ArrayList<>();

        for (int i=0;i<n;i++){
            verified_xi_values_ls.add(verified_xi_result.get(i));
            verified_sxi_values_ls.add(verified_sxi_result.get(i));
        }

        BigInteger reconSecret;
        if ((verificationResult.getRight().size() == verificationResult.getLeft().size()) && (verificationResult.getRight().size() >= t)) {
            //调用重构方法，实现秘密重构获取有效的子秘密碎片
            reconSecret = FVSS.reconstructSecret(verified_xi_values_ls, verified_sxi_values_ls);
            if (secret.equals(reconSecret)) {
                System.out.println("秘密重构结果：已正确重构秘密！原始秘密值为：" + secret + "；重构后的秘密值为：" + reconSecret);
            } else {
                System.out.println("秘密重构结果：秘密重构出错！原始秘密值为：" + secret + "；重构后的秘密值为：" + reconSecret);
            }
        } else {
            System.out.println("重构失败，秘密份额不足！");
        }

        System.out.println("\n***********************************⭐恶意节点和诚实节点奖惩情况⭐*********************************");
        System.out.println("恶意节点数量: maliciousPeer="+maliciousPeer);
        System.out.println("恶意节点集合: isMalicious="+isMalicious);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double seconds = duration ;
        System.out.println("\n⏰代码执行时间：" + seconds + " s");

    }
}

