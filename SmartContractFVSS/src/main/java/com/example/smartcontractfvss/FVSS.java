package com.example.smartcontractfvss;

import org.apache.commons.lang3.tuple.Pair;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;


public class FVSS {

    public static int n= 4 ; //参与者数量
    public static final BigInteger g= new BigInteger("2");//群GF(p)的生成元g
    public static final int t = (n/3)+1; //门限值设置
    public static final String pw ="zqh20240113";//设置用户口令
    public final static BigInteger secret = new BigInteger("20240113564181");//设置初始秘密值
   // public final static BigInteger secret = Secrets.secretInput();//这里也可以选择手动输入秘密值

    public static Random random = new Random(System.currentTimeMillis());
    public static int bitLength = 128;  //设置所需的位长度
    public static BigInteger p = generatePrime(bitLength);
    public static final Logger logger = Logger.getLogger(FVSS.class.getName());//创建了一个logger对象，使用SecretSharingProtocol.class.getName()作为记录器名称

    public static int maliciousPeer; // 恶意节点数量
    public static List<Integer> isMalicious = new ArrayList<>(); // 恶意节点列表
    public static int d = 100; // 每个节点缴纳的押金数额

    //生成一个较大素数
    public static BigInteger generatePrime(int bitLength) {
        Random random = new Random();
        BigInteger p = BigInteger.probablePrime(bitLength, random);
        while (!p.isProbablePrime(100)) {
            p = p.nextProbablePrime();
        }
        return p;
    }


    //分发秘密
    public static Pair<BigInteger[], BigInteger[]> distributeSecret(BigInteger secret){

        BigInteger[] sub_xi_values = new BigInteger[n];
        BigInteger[] sub_sxi_values = new BigInteger[n];
        BigInteger[] sub_Xi_values = new BigInteger[n];
        BigInteger[] sub_SXi_values = new BigInteger[n];
        BigInteger[] sub_Ci_values = new BigInteger[n];
        BigInteger[] sub_Vi_values = new BigInteger[n];

        BigInteger hashPw = hash(pw);//生成哈希值。
        //构造n-1阶多项式：
        List<BigInteger> coefficients = Arrays.asList(new BigInteger[n]);
        for (int i=0;i<n;i++){
            //coefficients.set(i, new BigInteger(p.bitLength() - 1, random).mod(p));
            coefficients.set(i, secret);
        }

        //计算秘密份额和影子秘密份额
        for (int i=0;i<n;i++){
            BigInteger xi = new BigInteger(p.bitLength()-1,random).mod(p); //随机生成xi
            sub_xi_values[i] = xi;//将xi存储进入xi_value数组中

            BigInteger sxi = evaluatePolynomial(coefficients,xi);//调用自定义的evaluatePolynomial方法；
            sub_sxi_values[i] = sxi;//将sxi存储进入secretShares数组中, sxi=f(xi)

            //生成影子秘密份额
            BigInteger SXi = sxi.xor(hashPw);//调用hash方法, SXi=sxi⊕hash(pw)
            sub_SXi_values[i] = SXi;//将SXi存储进入SXi_value数组中
            //生成影子数
            BigInteger Xi = xi.xor(hashPw);//调用hash方法,Xi=xi⊕hash(pw)
            sub_Xi_values[i] = Xi;//将Xi存储进入Xi_value数组中
            //为影子秘密份额生成对于承诺值
            BigInteger Ci = g.modPow(SXi,p); //Ci=g^SXi mod p
            sub_Ci_values[i] = Ci;
            //生成验证报文
            BigInteger Vi = hash(Xi.toString()+SXi.toString()+Ci.toString()); //验证报文：Vi=hash(Xi||SXi||Ci);
            sub_Vi_values[i] = Vi;
        }
        //返回（sub_Xi_values[i],sub_SXi_values[i]);
        return Pair.of(sub_Xi_values,sub_SXi_values);
    }

    //模拟：发送秘密份额给智能合约进行验证，返回当前计算的验证报文
    public static BigInteger[] sendSharesToSmartContract(BigInteger[] sub_Xi_values, BigInteger[] sub_SXi_values, BigInteger[] sub_Ci_values) {
        //实现与智能合约交互
        //数据准备
        BigInteger[] Vi_values1;
        Vi_values1 = new BigInteger[n];

        BigInteger[] Xi_values1 = new BigInteger[n];
        BigInteger[] SXi_values1 = new BigInteger[n];
        BigInteger[] Ci_values1 = new BigInteger[n];

        try {
            if (Xi_values1 !=null && SXi_values1!=null){
                for (int i = 0; i < n; i++) {
                    Xi_values1[i] = sub_Xi_values[i];
                    //System.out.println("影子数" + Xi_values1[i] + "已提交至智能合约！");
                    SXi_values1[i] = sub_SXi_values[i];
                    //System.out.println("影子秘密份额" + SXi_values1[i] + "已提交至智能合约！");
                    Ci_values1[i] = sub_Ci_values[i];
                    //System.out.println("承诺值" + Ci_values1[i] + "已提交至智能合约！");
                    //验证报文：Vi=hash(Xi||SXi||Ci)
                    Vi_values1[i] = hash(Xi_values1[i].toString() + SXi_values1[i].toString() + Ci_values1[i].toString());
                }
            }else {
                return null;
            }
            //提交成功，打印日志信息
            logger.info("秘密份额信息成功提交至智能合约！");
        } catch (Exception e) {
            logger.info("秘密份额信息提交至智能合约失败！");
        }
        return Vi_values1;
    }


    //传入最初的验证报文和当前验证报文，模拟返回智能合约的验证结果
    public static boolean getVerificationResultsFromSmartContract(BigInteger[] Vi_values1, BigInteger[] sub_Vi_values) {
        int[] verificationResults = new int[Vi_values1.length];

        int finalResult = 1;

        for (int i = 0; i < Vi_values1.length-1; i++) {
            if (Arrays.equals(Vi_values1, sub_Vi_values)) {
                verificationResults[i] = 1;
            } else {
                verificationResults[i] = 0;
            }

            finalResult &= verificationResults[i];
        }
        return finalResult == 1;
    }

    //存储验证通过后的影子秘密碎片
    public static Pair<List<BigInteger>, List<BigInteger>> verifyShares(BigInteger[] sub_Xi_values, BigInteger[] sub_SXi_values, BigInteger[] sub_Vi_values, BigInteger[] Vi_values1, BigInteger[] sub_Ci_values) {
        int n = sub_Xi_values.length;

        // 创建空数组，存放用户贡献的影子秘密碎片相关信息
        BigInteger[] verified_Ci_values = new BigInteger[n];
        BigInteger[] verified_Xi_values = new BigInteger[n];
        BigInteger[] verified_SXi_values = new BigInteger[n];

        // 创建空数组，存放验证过后的随机数和子秘密份额
        BigInteger[] verified_xi_values = new BigInteger[n];
        BigInteger[] verified_sxi_values = new BigInteger[n];

        // 逐个遍历用户贡献的影子秘密份额的相关信息
        List<BigInteger> verified_xi_values_ls = new ArrayList<>(n);
        List<BigInteger> verified_sxi_values_ls = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            BigInteger verified_Xi = sub_Xi_values[i];
            verified_Xi_values[i] = verified_Xi;

            BigInteger verified_SXi = sub_SXi_values[i];
            verified_SXi_values[i] = verified_SXi;

            BigInteger verified_Ci = g.modPow(verified_SXi, p);
            verified_Ci_values[i] = verified_Ci;

            // 获取智能合约对于验证报文的核验结果，并在核验通过的前提下执行以下操作
            // 在验证报文核验通过的前提下，执行影子秘密份额承诺值的验证操作
            if (getVerificationResultsFromSmartContract(Vi_values1, sub_Vi_values)) {

                BigInteger hashPw = hash(pw);
                BigInteger verified_xi = verified_Xi.xor(hashPw); // 调用hash方法, xi = Xi ⊕ hash(pw)
                BigInteger verified_sxi = verified_SXi.xor(hashPw); // 调用hash方法, sxi = SXi ⊕ hash(pw)
                // 获得验证通过的随机数和子秘密份额
                verified_xi_values[i] = verified_xi;
                verified_sxi_values[i] = verified_sxi;

                if (!verified_Ci_values[i].equals(g.modPow(sub_SXi_values[i], p))) {
                    // 广播验证报文错误信息
                    System.out.println("Peer" + i + "承诺信息验证失败！请在规定时间内向智能合约提交认证请求！");
                    //智能合约验证：
                    if (!verified_Ci_values[i].equals(g.modPow(sub_SXi_values[i], p))) {
                        // 广播承诺值的错误消息
                        System.out.println("Peer" + i + "的承诺值在智能合约中验证未通过！");
                        System.out.println("Peer" + i + "被识别为恶意节点，并扣除其押金数额：" + d + ".");
                        //处理恶意节点：
                        isMalicious.add(i); //识别出节点i为恶意节点，将其加入恶意节点列表。
                        maliciousPeer++;  // 恶意节点的数量自增1；
                    }else{
                        System.out.println("Peer" + i + "为诚实节点，智能合约验证通过！");
                    }
                }else{
                    System.out.println("Peer" + i + "的影子秘密份额承诺值验证通过！");
                    // 存储该子秘密碎片
                    System.out.println("Peer" + i + "为诚实诚实节点，贡献的子秘密份额为:(Xi, SXi): (" + verified_Xi_values[i] + ", " + verified_SXi_values[i] + ")。");

            }
            verified_xi_values_ls = Arrays.asList(verified_xi_values);
            verified_sxi_values_ls = Arrays.asList(verified_sxi_values);

        }else{
                System.out.println("验证报文核验不通过！");
            }
        }
        return Pair.of(verified_xi_values_ls, verified_sxi_values_ls);
    }

    //秘密重构方法
    public static BigInteger reconstructSecret(List<BigInteger> verified_xi_values_ls, List<BigInteger> verified_sxi_values_ls){

        List<BigInteger> real_xi_values1 = new ArrayList<>();
        List<BigInteger> real_sxi_values1 = new ArrayList<>();

        for (int i=0;i<verified_xi_values_ls.size();i++){
            BigInteger element = verified_xi_values_ls.get(i);
            real_xi_values1.add(element);
            //System.out.println("real_xi_values1["+i+"]="+real_xi_values1.get(i));
        }

        for (int i=0;i<verified_sxi_values_ls.size();i++){
            BigInteger element = verified_sxi_values_ls.get(i);
            real_sxi_values1.add(element);
            //System.out.println("\real_sxi_values1["+i+"]="+real_sxi_values1.get(i));
        }

        if ((real_xi_values1.size() >= t) && (real_sxi_values1.size() >= t) && (t!=0)){
            BigInteger result = lagrangeInterpolation(real_xi_values1,real_sxi_values1);
            return result;
        }else {
            throw new IllegalArgumentException("秘密碎片数量未达门限值，重构失败！");
        }
    }

//******构造辅助函数

    //计算多项式的值
    public static BigInteger evaluatePolynomial(List<BigInteger> coefficients, BigInteger x){
        Random random = new Random(12345); // 使用随机种子
        BigInteger result = BigInteger.ZERO;
        BigInteger powerOfx = BigInteger.ONE;

        for (int i=0;i<coefficients.size();i++){
            result = result.add(coefficients.get(i).multiply(powerOfx)).mod(p);
            powerOfx = powerOfx.multiply(x).mod(p);
        }
        return result;
    }

    //拉格朗日插值法
    public static BigInteger lagrangeInterpolation(List<BigInteger> real_xi_values, List<BigInteger> real_sxi_values) {
        //先判空
        if (real_xi_values.size() < t || real_sxi_values.size() < t) {
            throw new IllegalArgumentException("秘密份额数量不足！");
        }

        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < real_xi_values.size(); i++) {
            BigInteger xi = real_xi_values.get(i);
            BigInteger sxi = real_sxi_values.get(i);

            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            BigInteger[] coefficients = new BigInteger[n]; //系数列表
            //List<BigInteger> coefficients = new ArrayList<>(n);//系数列表

            for (int j = 0; j < real_xi_values.size(); j++) {
                if (i != j) {
                    BigInteger xj = real_xi_values.get(j);
                    numerator = numerator.multiply(xj.negate()).mod(p);
                    denominator = denominator.multiply(xi.subtract(xj)).mod(p);
                    coefficients[i] = new BigInteger(p.bitLength() - 1, random).mod(p);
                    //coefficients[i] = coefficients.set(i, secret);
                }
            }

            BigInteger inverseDenominator = denominator.modInverse(p);
            List<BigInteger> tempCoefficients = new ArrayList<>();
            tempCoefficients.add(numerator.multiply(inverseDenominator)); // 临时列表包含计算得到的值
            BigInteger term = sxi.multiply(evaluatePolynomial(tempCoefficients, xi)).mod(p);
            result = result.add(term).mod(p);
        }
        return result;
    }

    //计算哈希值
    public static BigInteger hash(String input){
        if (input != null){
            try{
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = md.digest(input.getBytes());
                return new BigInteger(1,hashBytes);
            }catch (NoSuchAlgorithmException e){
                e.printStackTrace();
            }
        }else {
            return null;
        }
        return BigInteger.ZERO;
    }

}
