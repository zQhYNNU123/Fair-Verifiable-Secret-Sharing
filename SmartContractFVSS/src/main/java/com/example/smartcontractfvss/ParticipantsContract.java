package com.example.smartcontractfvss;


import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import lombok.extern.java.Log;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.hyperledger.fabric.shim.ledger.QueryResultsIteratorWithMetadata;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAKey;
import java.util.*;
import static com.example.smartcontractfvss.FVSS.*;


//使用的数据库是CouchDB
@Contract(
        name = "ParticipantsContract",
        transactionSerializer = "com.example.smartcontractfvss.ValidationJSONTransactionSerializer",
        info = @Info(
                title = "Participants Contract",
                description = "Participant Contract",
                version = "0.0.1-SNAPSHOT",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "https://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "f.carr@example.com",
                        name = "Participants Contract",
                        url = "https://hyperledger.example.com")))

@Log
public class ParticipantsContract implements ContractInterface {

    public int n = FVSS.n;
    public static Pair<BigInteger[], BigInteger[]> distributedResult = FVSS.distributeSecret(secret);
    public BigInteger reconSecret;


    @Transaction
    public void init(final Context ctx){
        System.out.println("系统初始化...");
    }

    @Transaction//在账本里初始化一些参与方信息
    public void initLedger(final Context ctx) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException { //初始化账本信息
        ChaincodeStub stub = ctx.getStub();

        // 初始化一些数组，用来存储影子秘密份额相关信息
        BigInteger[] Xi_value = distributedResult.getLeft();
        BigInteger[] SXi_value = distributedResult.getRight();
        BigInteger[] commits = new BigInteger[n];
        BigInteger[] verification = new BigInteger[n];
        List<BigInteger> verified_xi_values_ls = new ArrayList<>();
        List<BigInteger> verified_sxi_values_ls = new ArrayList<>();
        Pair<List<BigInteger>, List<BigInteger>> VerifyShadowShares = null;
        BigInteger[] Vi_result = new BigInteger[n];
        BigInteger[] Ci_result = new BigInteger[n];

        System.out.println("参与者数量：n="+n);
        System.out.println("秘密共享门限值：t="+t+"\n");

        for (int i = 0; i < n; i++) {
            BigInteger Xi = Xi_value[i];
            BigInteger SXi = SXi_value[i];
            Pair<BigInteger, BigInteger> secretShares = Pair.of(Xi, SXi);

            BigInteger commit = g.modPow(SXi, p);
            commits[i] = commit;
            BigInteger Vi = hash(Xi.toString() + SXi.toString() + commit.toString());
            verification[i] = Vi;

            //初始化成员信息
            Participants participants = new Participants().setName("P" + i)
                    .setId(i)
                    .setAssets(1000)
                    .setDeposit(100)
                    .setShadowShares(secretShares)
                    .setCommits(commits[i])
                    .setVerification(verification[i]);

            String key = String.format("P%s", i);//初始化key值
            stub.putStringState(key, JSON.toJSONString(participants)); //将成员信息以键值对的形式存储到状态数据库CouchDB
            //System.out.println("\nP" + i + "信息：" +participants);

            //RSA加密影子秘密份额，获得密文
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(1024);
            KeyPair keyPair = keyPairGen.generateKeyPair();// 生成RSA密钥对
            PublicKey publicKey = keyPair.getPublic(); //公钥
            PrivateKey privateKey = keyPair.getPrivate(); //私钥

            // 创建加密器和解密器
            Cipher encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);  //公钥加密
            Cipher decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);  //私钥解密

            //存储影子秘密份额明文
            BigInteger[] plaintext_sub_Xi = Xi_value;
            BigInteger[] plaintext_sub_SXi = SXi_value;
            Pair<BigInteger[], BigInteger[]> dataPair = Pair.of(plaintext_sub_Xi, plaintext_sub_SXi);

            // 将dataPair转换为字节数组
            byte[] dataBytes = RSACryptoExample.serialize(dataPair);

            // 分块加密
            int keySize = ((RSAKey) publicKey).getModulus().bitLength();
            int maxBlockSize = keySize / 8 - 11; // RSA加密的最大块大小
            int blocks = (int) Math.ceil((double) dataBytes.length / maxBlockSize);
            ByteArrayOutputStream encryptedDataStream = new ByteArrayOutputStream();
            for (int j = 0; j < blocks; j++) {
                int offset = j * maxBlockSize;
                int length = Math.min(maxBlockSize, dataBytes.length - offset);
                byte[] block = new byte[length];
                System.arraycopy(dataBytes, offset, block, 0, length);
                byte[] encryptedBlock = encryptCipher.doFinal(block);
                encryptedDataStream.write(encryptedBlock);
            }
            byte[] encryptedData = encryptedDataStream.toByteArray(); //获得密文数据

            //打印密文数据为十六进制数
            System.out.println("影子秘密份额密文-RSA：");
            for (byte b : encryptedData) {
                System.out.print(Integer.toHexString(b & 0xFF));
            }

            //RSA解密影子秘密份额，获得影子秘密份额明文
            // 分块解密
            ByteArrayOutputStream decryptedDataStream = new ByteArrayOutputStream();
            for (int j = 0; j < blocks; j++) {
                int offset = j * (keySize / 8);
                int length = Math.min(keySize / 8, encryptedData.length - offset);
                byte[] block = new byte[length];
                System.arraycopy(encryptedData, offset, block, 0, length);
                byte[] decryptedBlock = decryptCipher.doFinal(block);
                decryptedDataStream.write(decryptedBlock);
            }
            byte[] decryptedData = decryptedDataStream.toByteArray();

            // 将解密后的字节数组还原为Pair对象
            Pair<BigInteger[], BigInteger[]> decryptedPair = RSACryptoExample.deserialize(decryptedData);
            BigInteger[] decrypt_sub_Xi = decryptedPair.getLeft();
            BigInteger[] decrypt_sub_SXi = decryptedPair.getRight();
            BigInteger Ci = g.modPow(decrypt_sub_SXi[i], p); //计算解密后的影子秘密份额承诺值
            Ci_result[i] = Ci;

            //判断加解密前后明文内容是否一致
            if (Arrays.equals(plaintext_sub_Xi, decrypt_sub_Xi) && Arrays.equals(plaintext_sub_SXi, decrypt_sub_SXi)) {
                System.out.println("加解密结果：数据有效！加解密前后内容一致!");
            } else {
                System.out.println("加解密结果：数据无效！加解密前后内容不一致!");
            }

            //验证报文信息：
            System.out.println("实际验证报文ActualViResult" + i + "= " + verification[i]);
            if (Ci_result[i].equals(commits[i])) { //先判断影子秘密份额的承诺值验证是否通过
                System.out.println("\n验证后公开的承诺值：Ci_result= " + Ci_result[i]);

                Vi_result = sendSharesToSmartContract(decrypt_sub_Xi, decrypt_sub_SXi, Ci_result);
                System.out.println("验证报文信息：ExpectViResult" + i + "= " + Vi_result[i]);

                boolean verify_result = getVerificationResultsFromSmartContract(Vi_result, verification);
                System.out.println("报文信息验证结果：Vi_result= " + verify_result + "\n");

                System.out.println("P" + i + "存储的影子秘密份额: X" + i + "= " + Xi + "," + "SX" + i + "= " + SXi+"\n");

                //验证承诺值
                VerifyShadowShares = verifyShares(decrypt_sub_Xi, decrypt_sub_SXi, verification, Vi_result, commits);

                List<BigInteger> verified_xi_result = VerifyShadowShares.getLeft();//获取已经验证过的xi;
                List<BigInteger> verified_sxi_result = VerifyShadowShares.getRight();//获取已经验证过的sxi

                verified_xi_values_ls.add(verified_xi_result.get(i));
                verified_sxi_values_ls.add(verified_sxi_result.get(i));
            } else {
                System.out.println("影子秘密份额P" + i + "对应的承诺值验证失败！");
            }


        }

        //重构秘密
        if ((VerifyShadowShares.getRight().size() == VerifyShadowShares.getLeft().size()) && (VerifyShadowShares.getRight().size() >= t)) {
            //调用重构方法，实现秘密重构获取有效的子秘密碎片
            reconSecret = reconstructSecret(verified_xi_values_ls, verified_sxi_values_ls);
            if (secret.equals(reconSecret)) {
                System.out.println("秘密重构结果：已正确重构秘密！原始秘密值为：" + secret + "；重构后的秘密值为：" + reconSecret + "\n");
            } else {
                System.out.println("秘密重构结果：秘密重构出错！原始秘密值为：" + secret + "；重构后的秘密值为：" + reconSecret + "\n");
            }
        } else {
            System.out.println("重构失败，秘密份额不足！" + "\n");
        }

        // 计算每个诚实节点应退还的押金数额
        int honestPeer = n - maliciousPeer; //系统剩下的恶意节点数量
        int maliciousPeer_total_d;  //系统收取所有恶意节点的罚金总和
        int honestPeer_Refund = 0; // 诚实节点应退回的押金数额

        if (honestPeer > 0 && maliciousPeer <= n) {
            maliciousPeer_total_d = maliciousPeer * d;
            honestPeer_Refund = d + maliciousPeer_total_d / honestPeer;
        } else {
            System.out.println("系统节点数不合法或系统中的节点全为恶意节点！");
        }
        System.out.println("每个诚实节点可以退回的押金数额: refund=" + honestPeer_Refund);
        System.out.println("恶意节点集合：isMalicious=" + isMalicious + "\n");

    }

    @Transaction //新建一些新参与方
    public static Participants createParticipants(final Context ctx, final String key, int id, String name, int assets, int deposit, Pair<BigInteger,BigInteger> secretShares,BigInteger commits,BigInteger verification){

        ChaincodeStub stub = ctx.getStub();
        String participantsState = stub.getStringState(key);
        if (StringUtils.isNotBlank(participantsState)){
            String errorMessage = String.format("添加结果：节点%s已经存在！\n",key);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        Participants participants = new Participants()
                .setId(id)
                .setName(name)
                .setAssets(assets)
                .setDeposit(deposit)
                .setShadowShares(secretShares)
                .setCommits(commits)
                .setVerification(verification);

        String participant_json = JSON.toJSONString(participants);
        stub.putStringState(key,participant_json);

        stub.setEvent("CreateParticipantEvent:",org.apache.commons.codec.binary.StringUtils.getBytesUtf8(participant_json));

        return participants;
    }


    @Transaction//从账本中返回对应键的参与方信息
    public static Participants queryParticipant(final Context ctx, final String key){

        ChaincodeStub stub = ctx.getStub();
        String participantState = stub.getStringState(key); //节点的状态数据

        if (StringUtils.isBlank(participantState)){
            String errorMessage = String.format("节点%s不存在！",key);
            System.out.println(errorMessage);
            throw  new ChaincodeException(errorMessage);
        }

        Participants participants_json = JSON.parseObject(participantState,Participants.class);
        return participants_json;
    }


    @Transaction
    public ParticipantQueryResultList queryParticipantByName(final Context ctx, String name){

        log.info(String.format("使用name查询节点信息，name = %s",name));

        String query = String.format("{\"selector\":{\"name\":\"%s\"} , \"use_index\":[\"_design/indexNameColorDoc\", \"indexNameColor\"]}",name);

        log.info(String.format("查询结果= %s",query));
        return queryParticipant(ctx.getStub(),query);
    }

    @Transaction
    public ParticipantQueryResultList queryParticipant(ChaincodeStub stub, String query){

        ParticipantQueryResultList resultList = new ParticipantQueryResultList();
        QueryResultsIterator<KeyValue> queryResult = stub.getQueryResult(query);
        List<ParticipantsQueryResult> results = Lists.newArrayList();

        if (!IterableUtils.isEmpty(queryResult)){
            for (KeyValue kv : queryResult){
                results.add(new ParticipantsQueryResult().setKey(kv.getKey()).setParticipants(JSON.parseObject(kv.getStringValue() , Participants.class)));
            }
            resultList.setParticipants(results);
        }

        return resultList;
    }


    @Transaction
    public ParticipantsQueryPageResult queryParticipantPageByName(final Context ctx, String name,Integer PageSize, String bookmark){

        log.info(String.format("使用name分页查询节点，name = %s",name));
        String query = String.format("{\"selector\":{\"name\":\"%s\"} , \"use_index\":[\"_design/indexNameColorDoc\", \"indexNameColor\"]}",name);

        log.info(String.format("查询结果 = %s", query));
        ChaincodeStub stub = ctx.getStub();

        QueryResultsIteratorWithMetadata<KeyValue> queryResult = stub.getQueryResultWithPagination(query, PageSize, bookmark);
        List<ParticipantsQueryResult> participants = Lists.newArrayList();

        if (! IterableUtils.isEmpty(queryResult)) {
            for (KeyValue kv : queryResult) {
                participants.add(new ParticipantsQueryResult().setKey(kv.getKey()).setParticipants(JSON.parseObject(kv.getStringValue() , Participants.class)));
            }
        }

        return new ParticipantsQueryPageResult()
                .setParticipants(participants)
                .setBookmark(queryResult.getMetadata().getBookmark());
    }


    @Transaction  //更新参与方信息
    public Participants updateParticipant(final Context ctx, final String key, int id, String name, int assets, int deposit, Pair<BigInteger,BigInteger> shadowShares,BigInteger commits,BigInteger verification){

        ChaincodeStub stub = ctx.getStub();
        String participantState = stub.getStringState(key);

        if (StringUtils.isBlank(participantState)){
            String err = String.format("参与方%s不存在！",key);
            System.out.println(err);
            throw new ChaincodeException(err);
        }

        Participants up_participants = new Participants()
                .setId(id)
                .setName(name)
                .setAssets(assets)
                .setDeposit(deposit)
                .setShadowShares(shadowShares)
                .setCommits(commits)
                .setVerification(verification);

        stub.putStringState(key,JSON.toJSONString(up_participants));
        return up_participants;

    }

    @Transaction  //删除参与方信息
    public Participants delparticipants(final Context ctx, final String key){

        ChaincodeStub stub = ctx.getStub();
        String participantState = stub.getStringState(key);

        if (StringUtils.isBlank(participantState)){
            String err = String.format("参与方%s不存在！",key);
            System.out.println(err);
            throw new ChaincodeException(err);
        }

        stub.delState(key);
        System.out.format("已将参与方%s的成员信息删除！",key);

        return JSON.parseObject(participantState,Participants.class);

    }

    @Override
    public  void beforeTransaction(Context ctx){
        log.info("***************************************执行交易之前*********************************************");
    }


    @Override
    public void afterTransaction(Context ctx, Object result){
        log.info("***************************************执行交易之后*********************************************");
        System.out.println("结果："+result);
    }

}
