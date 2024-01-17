package com.example.smartcontractfvss;

import com.google.gson.Gson;
import org.apache.commons.lang3.tuple.Pair;
import org.hyperledger.fabric.shim.ChaincodeStub;
import java.math.BigInteger;
import static com.example.smartcontractfvss.FVSS.secret;


//把分发的影子秘密份额存储进入状态数据库中，默认的状态数据库是levelDB
public class putShadowShares {
    public BigInteger[] sub_Xi_values;
    public BigInteger[] sub_SXi_values;

    Pair<BigInteger[], BigInteger[]> distributeResults = FVSS.distributeSecret(secret);

    public putShadowShares(BigInteger[] sub_Xi_values,BigInteger[] sub_SXi_values){
        this.sub_Xi_values = sub_Xi_values;
        this.sub_SXi_values = sub_SXi_values;
    }

    public BigInteger[] getSub_Xi_values(){
        sub_Xi_values = distributeResults.getLeft();
        return sub_Xi_values;
    }

    public BigInteger[] getSub_SXi_values(){
        sub_SXi_values = distributeResults.getRight();
        return sub_SXi_values;
    }

    public String getName(){
        //返回智能合约的名字
        return "FVSSContract";
    }

    public byte[] putDate(ChaincodeStub stub,String[] args){
        if (args.length<2){
            throw new IllegalArgumentException("参数错误！");
        }

        String sub_Xi_valuesJson = args[0];
        String sub_SXi_valuesJson = args[1];
        Gson gson = new Gson();

        //将JSON格式的数据转换为BigINTEGER类型的数组
        BigInteger[] sub_Xi_values = gson.fromJson(sub_Xi_valuesJson,BigInteger[].class);
        BigInteger[] sub_SXi_values = gson.fromJson(sub_SXi_valuesJson,BigInteger[].class);

        //创建shadowShares对象
        putShadowShares shadowShares = new putShadowShares(sub_Xi_values,sub_SXi_values);

        //转换为JSON格式
        String shadowSharesJson = gson.toJson(shadowShares);

        //存储到状态数据库中
        stub.putStringState("key",shadowSharesJson);
        String log = "影子秘密份额成功上链！";
        return log.getBytes();
    }


}
