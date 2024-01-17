package com.example.contracTest;

import com.example.smartcontractfvss.FVSS;
import org.apache.commons.lang3.tuple.Pair;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


/*该测试类用于验证智能合约对于第三方恶意分发行为的监控效果，
该部分实验为仿真实验，模拟了恶意分发方篡改影子秘密份额的恶意行为，并且智能合约对恶意分发方和诚实参与方的做出相应奖惩。
实验中假设恶意分发方为P4，其余皆为接收方，最终目的是识别出P4的恶意行为
 */

public class MaliPeerVerifyTest {
    static int n = FVSS.n; //参与者数量
    static final BigInteger p = FVSS.p;//大素数
    static final BigInteger g= FVSS.g;//群GF(p)的生成元g
    static final BigInteger secret = FVSS.secret;//设置初始秘密值
    static int maliciousPeer = FVSS.maliciousPeer; // 恶意节点数量
    static List<Integer> isMalicious = new ArrayList<>(); // 恶意节点列表
    static int dep = 300 ; //分发方预缴的押金

    //主函数
    public static void main(String[] args) throws Exception {

        //创建空数组，用来存储影子秘密份额
        BigInteger[] sub_Xi_values;
        BigInteger[] sub_SXi_values;

        //秘密分发阶段
        Pair<BigInteger[], BigInteger[]> distributeResults = FVSS.distributeSecret(secret);
        sub_Xi_values = distributeResults.getLeft();
        sub_SXi_values = distributeResults.getRight();
        BigInteger[] sub_Ci_values = new BigInteger[sub_SXi_values.length];//存放承诺值
        BigInteger[] sub_Vi_values = new BigInteger[sub_SXi_values.length];//存放验证报文

        //⭐⭐⭐⭐⭐此部分的份额为真实影子秘密份额
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

        //⭐⭐⭐⭐⭐此部分的份额为篡改后的伪影子秘密份额，我是通过随机修改某个真实影子秘密份额来实现篡改效果
        //分别创建数组来存储伪影子秘密份额；
        BigInteger [] Jia_sub_Xi = new BigInteger[n];
        BigInteger [] Jia_sub_SXi = new BigInteger[n];

        //⭐⭐⭐⭐⭐篡改影子秘密份额：通过将目标份额与对应的承诺值进行异或运算，将异或结果作为篡改后影子秘密份额（伪影子秘密份额）分发给接收方；
        System.out.println("\n篡改后的伪影子秘密份额和伪阴影数：");
        for (int i=0; i<n; i++){
            Jia_sub_Xi[i] = sub_Xi_values[i].xor(sub_Ci_values[i]);
            Jia_sub_SXi[i] = sub_SXi_values[i].xor(sub_Ci_values[i]);
            System.out.println("Jia_sub_Xi[" + i + "]: " + Jia_sub_Xi[i]);
            System.out.println("Jia_sub_SXi[" + i + "]: " + Jia_sub_SXi[i]);
        }

        //由于重点是验证分发方的恶意行为，这里就省略了伪影子秘密份额的加解密过程了，假设直接将伪影子秘密份额分发给接收方
        //初始化参与方对象，参与方用一个二位数组来存储(X,SX)
        //假设分发方为Pn，其余的n-1个节点均为接收方
        BigInteger[][] P = new BigInteger[2][n];

        //参与方需要计算承诺值和验证报文，故创建一个新数组来分别存储伪影子秘密份额的承诺值和报文信息
        BigInteger[] Jia_Ci = new BigInteger[n];
        BigInteger[] Jia_Vi = new BigInteger[n];

        //报文信息验证情况初始化伪false：
        boolean VerifyResult = false;

        //将伪影子逐个分发给参与方对象,这里的参与方对象主键用i标识，如：P1
        for (int i=0; i<n-1; i++) {
            P[0][i] = Jia_sub_Xi[i];
            P[1][i] = Jia_sub_SXi[i];

            //参与方当前的承诺值
            BigInteger j_Ci = g.modPow(P[1][i], p);
            Jia_Ci[i] = j_Ci;

            //存储参与方当前的报文信息
            BigInteger j_Vi = FVSS.hash(P[0][i].toString() + P[1][i].toString() + Jia_Ci[i].toString());
            Jia_Vi[i] = j_Vi;

            //验证报文信息的一致性：调用本方案的验证算法，传入将当前伪影子秘密份额计算得来的报文信息和最初广播的报文信息
            VerifyResult = FVSS.getVerificationResultsFromSmartContract(Jia_Vi, sub_Vi_values);

            //初始化一个空数组来盛放接收方最终持有的验证报文信息，该验证报文可能为当前验证报文，也可能为空，为空的情况说明接收方拒绝了接收该份额
            BigInteger[] P_Vi = new BigInteger[n];

            if (VerifyResult) {
                System.out.printf("\n参与方互验结果：P%s所持份额验证通过！\n", i);
                //若验证通过，则接收方存储该份额
                P[0][i] = Jia_sub_Xi[i];
                P[1][i] = Jia_sub_SXi[i];
                BigInteger j1_Vi = FVSS.hash(P[0][i].toString() + P[1][i].toString() + Jia_Ci[i].toString());
                P_Vi[i] = j1_Vi;
            } else {
                System.out.printf("\n参与方互验结果：P%s所持份额验证失败！", i);
                //调用智能合约验证算法进行二次验证，目的是对分发方进行确责, SC_Vi为智能合约计算的验证报文
                BigInteger[] SC_Vi = FVSS.sendSharesToSmartContract(Jia_sub_Xi, Jia_sub_SXi, Jia_Vi);
                //若验证失败，则参与方拒绝存储该份额，参与方所持份额置空；
                P[0][i] = null;
                P[1][i] = null;
                P_Vi[i] = null;
                //若智能合约验证的报文信息和最初广播的一致且与当前报文信息不一致且参与方最终没有存储任何份额信息，则说明是分发方的确篡改了份额
                if (P_Vi[i] == null){
                    if(!(SC_Vi[i].equals(sub_Vi_values[i]))) {
                        System.out.println("\n智能合约验证结果：分发方所分发的影子秘密份额不合法！初步判定分发方恶意！接收方未存储该份额！\n");
                        //对分发方执行惩罚：将其加入恶意节点集合，扣除其押金
                        isMalicious.add(n);
                        System.out.println("恶意节点判定结果：恶意分发方为P"+n+"!");
                    }else{
                        System.out.println("\n智能合约验证结果：分发方所分发的影子秘密份额合法！初步判定接收方恶意！对接收方执行恶意惩罚！\n");
                        //对接收方执行惩罚：将其加入恶意节点集合，扣除其押金
                        isMalicious.add(i);
                        System.out.println("恶意节点判定结果：恶意分发方为P"+i+"!");
                    }
                    dep -= dep;
                    System.out.println("奖惩结果：已扣除恶意分发方的押金,其剩余的押金约为："+dep);
                }else {
                    System.out.println("\n智能合约验证结果：接收方所存储的影子秘密份额非法，请分发方重新发送合法份额至接收方，否则执行惩罚办法同时奖惩分方和接收方!\n");
                    //若分发方重新诚实执行秘密分发，则返还其押金
                    if(FVSS.distributeSecret(secret) != null){
                        isMalicious = null;
                        maliciousPeer = 0 ;
                    }else {
                        dep -= dep;
                        isMalicious.add(n);
                        maliciousPeer +=1 ;
                    }
                }
            }
        }
        maliciousPeer += 1;
        System.out.println("恶意节点数量为："+maliciousPeer);
    }
}
