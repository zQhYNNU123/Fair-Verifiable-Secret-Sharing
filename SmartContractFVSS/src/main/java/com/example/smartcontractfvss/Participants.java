package com.example.smartcontractfvss;

import org.apache.commons.lang3.tuple.Pair;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.math.BigInteger;


@Data
@DataType
@Accessors(chain = true)
public class Participants {

    @Property
    int id = 3; //主键

    @Property
    String name; //节点名称

    @Property
    int assets;  //节点资产总额，注意：资产总额必须大于押金

    @Property
    int deposit;  //押金数额

    @Property
    Pair<BigInteger,BigInteger> shadowShares; //存储的影子份额

    @Property
    BigInteger Commits;//影子秘密份额对于的承诺值

    @Property
    BigInteger Verification; //验证报文

}
