package com.example.smartcontractfvss;
import java.math.BigInteger;

//Elgamal加密包
public class elgamal {

    public BigInteger  alpha, y; /*y=α^x(mod p)，1≤x≤p-1 x=log α y，1≤y≤p-1*/
    private BigInteger d;   //私钥
    static BigInteger p = FVSS.p;

    static BigInteger secret = FVSS.secret;
    public elgamal(BigInteger p, BigInteger alpha, BigInteger d) {
        this.p = p;
        this.alpha = alpha;
        this.d = d;
        y = alpha.modPow(d, p);
    }

    /*** 加密*/
    BigInteger[] encrypt(BigInteger M, BigInteger k) {
        BigInteger[] C = new BigInteger[2];
        BigInteger U = y.modPow(k, p);
        C[0] = alpha.modPow(k, p);
        C[1] = U.multiply(M).mod(p);
        return C;
    }

    /*** 解密*/
    BigInteger decrypt(BigInteger[] C) {
        BigInteger V = C[0].modPow(d, p);
        //BigInteger M = C[1].multiply(V.modPow(new BigInteger("-1"), p)).mod(p);
        BigInteger M = C[1].multiply(V.modInverse(p)).mod(p);
        return M;
    }

}