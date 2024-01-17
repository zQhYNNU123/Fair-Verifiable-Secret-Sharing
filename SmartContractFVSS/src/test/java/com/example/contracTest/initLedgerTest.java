package com.example.contracTest;

import com.alibaba.fastjson.JSON;
import com.example.smartcontractfvss.Participants;
import com.example.smartcontractfvss.ParticipantsContract;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import static com.example.smartcontractfvss.FVSS.n;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class initLedgerTest {

    @Test
    public void participantTest() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, IOException, BadPaddingException, InvalidKeyException, ClassNotFoundException {

        // 创建模拟的智能合约上下文和存根
        ChaincodeStub stub = Mockito.mock(ChaincodeStub.class);
        Context ctx = Mockito.mock(Context.class);
        when(ctx.getStub()).thenReturn(stub);

        // 调用initLedger方法
        ParticipantsContract contract = new ParticipantsContract();
        contract.initLedger(ctx);//调用初始化账本方法

        // 验证stub的putStringState方法是否被调用，并打印参数
        verify(stub, times(n)).putStringState(anyString(), anyString());

        // 打印participants信息
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(stub, times(n)).putStringState(anyString(), captor.capture());

        System.out.println("参与方信息：");
        for (int i = 0; i < n; i++) {
            String json = captor.getAllValues().get(i);
            Participants participants = JSON.parseObject(json, Participants.class);
            System.out.println("节点P" + i + "：" +participants);
        }

    }

}

