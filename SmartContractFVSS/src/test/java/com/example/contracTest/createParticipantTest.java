package com.example.contracTest;


import com.alibaba.fastjson.JSON;
import com.example.smartcontractfvss.FVSS;
import com.example.smartcontractfvss.Participants;
import com.example.smartcontractfvss.ParticipantsContract;
import org.apache.commons.lang3.tuple.Pair;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.math.BigInteger;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class createParticipantTest {


     //创建模拟的智能合约上下文和存根
    @Test
    public void  testCreateParticipants_NodeExists(){

        ChaincodeStub stub = Mockito.mock(ChaincodeStub.class);
        Context ctx = Mockito.mock(Context.class);
        when(ctx.getStub()).thenReturn(stub);

        org.apache.commons.lang3.tuple.Pair<BigInteger[], BigInteger[]> distributeSecret = FVSS.distributeSecret(FVSS.secret);
        BigInteger[] Xi_value = distributeSecret.getLeft();
        BigInteger[] SXi_value = distributeSecret.getRight();
        BigInteger[] commit = new BigInteger[1]; //只新增一个节点的情况，如果新增多个就换成数组，用i遍历

        BigInteger Xi = null;
        BigInteger SXi = null;
        BigInteger Ci = null;
        BigInteger Vi = null;
        for (int i = 0; i < 1; i++) {

            Xi = Xi_value[i];
            SXi = SXi_value[i];

            Ci = FVSS.g.modPow(SXi, FVSS.p);
            Vi = FVSS.hash(Xi.toString() + SXi.toString() + commit.toString());
        }

        //输入参数
        String key = "P1";
        int id = 1;
        String name = "P1";
        int assets = 100;
        int deposit = 1000;
        Pair<BigInteger, BigInteger> secretShares = Pair.of(Xi, SXi);
        BigInteger commits = Ci;
        BigInteger verification = Vi;

        //模拟已存在的参与方
        when(stub.getStringState(key)).thenReturn("节点" +key+ "已经存在！");
        ParticipantsContract contract = new ParticipantsContract();

        try {
            //调用合约函数
            Participants participants = contract.createParticipants(ctx, key, id, name, assets, deposit, secretShares, commits, verification);
            //验证合约方法是否被调用
            verify(stub).getStringState(key);
            verify(stub, times(0)).putStringState(eq(key), anyString());
            verify(stub,times(0)).setEvent(eq("CreateParticipantEvent:"),any(byte[].class));

            fail("合约函数调用发生异常！");

        }catch (ChaincodeException e){
            assertEquals("添加结果：节点"+key+ "已经存在！\n",e.getMessage());

            verify(stub).getStringState(key);
            verify(stub, times(0)).putStringState(eq(key), anyString());
            verify(stub,times(0)).setEvent(eq("CreateParticipantEvent:"),any(byte[].class));
        }

    }


    @Test
    public void testCreateParticipants() {

        ChaincodeStub stub = Mockito.mock(ChaincodeStub.class);
        Context ctx = Mockito.mock(Context.class);
        when(ctx.getStub()).thenReturn(stub);

        org.apache.commons.lang3.tuple.Pair<BigInteger[], BigInteger[]> distributeSecret = FVSS.distributeSecret(FVSS.secret);
        BigInteger[] Xi_value = distributeSecret.getLeft();
        BigInteger[] SXi_value = distributeSecret.getRight();
        BigInteger[] commit = new BigInteger[1]; //只新增一个节点的情况，如果新增多个就换成数组，用i遍历
        BigInteger[] verification = new BigInteger[1];

        BigInteger Xi = null;
        BigInteger SXi = null;
        BigInteger Ci = null;
        BigInteger Vi = null;
        for (int i = 0; i < 1; i++) {

            Xi = Xi_value[i];
            SXi = SXi_value[i];

            Ci = FVSS.g.modPow(SXi, FVSS.p);
            Vi = FVSS.hash(Xi.toString() + SXi.toString() + commit.toString());
        }

        //新增一个节点；如果需要新增多条数据，换成数组就行
        String key = "P10";
        int id = 10;
        String name = "P10";
        int assets = 100;
        int deposit = 5000;
        Pair<BigInteger, BigInteger> secretShares = Pair.of(Xi, SXi);
        commit[0] = Ci;
        verification[0] = Vi;

        when(stub.getStringState(key)).thenReturn(null);
        ParticipantsContract contract = new ParticipantsContract();

        try {
            //调用合约函数
            Participants participants = contract.createParticipants(
                    ctx, key, id, name, assets, deposit, secretShares, commit[0], verification[0]);

            String participant_json = JSON.toJSONString(participants);
            stub.putStringState(key, participant_json);

            //验证合约方法是否被调用
            verify(stub).getStringState(key);
            verify(stub, times(2)).putStringState(anyString(), anyString());
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(stub, times(2)).putStringState(anyString(), captor.capture());

            System.out.println("添加结果：已成功添加新节点" + key + ":" + participant_json);
        } catch (ChaincodeException e) {
            fail("添加节点失败！");
        }

    }

}
