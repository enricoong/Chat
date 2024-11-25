package itts.volterra.quintab;

import java.math.BigInteger;

public class DiffieHellman {

    /*
    Alice e Bob si accordano di usare un numero primo p = 23 e la base g = 5.
    Alice sceglie un numero segreto a = 6 e manda a Bob A = g^a mod p
        A = 56 mod 23 = 8
    Bob sceglie l'intero segreto b = 15 e manda ad Alice B = gb mod p
        B = 515 mod 23 = 19.
    Alice calcola KA = (gb mod p)a mod p = Ba mod p
        KA = 196 mod 23 = 2.
    Bob calcola KB = (ga mod p)b mod p = Ab mod p
        KB = 815 mod 23 = 2.
     */

    BigInteger P, G;                        //common public keys
    BigInteger privateKeyA, privateKeyB;    //chiavi private
    BigInteger x, y;                        //
    BigInteger ka, kb;

    public DiffieHellman(BigInteger p, BigInteger g, BigInteger privateKeyA, BigInteger privateKeyB) {
        P = p;
        G = g;
        this.privateKeyA = privateKeyA;
        this.privateKeyB = privateKeyB;

        x = calculatePower(G, privateKeyA, P);  //user 1
        y = calculatePower(G, privateKeyB, P);  //user 2
        ka = calculatePower(y, privateKeyA, P); //user 1  uguale a kb
        kb = calculatePower(x, privateKeyB, P); //user 2  uguale a ka
    }

    /**
     * Find the value of x^y mod P
     *
     * @param x Base
     * @param y Exponent
     * @param P Power
     * @return Result
     */
    private static BigInteger calculatePower(BigInteger x, BigInteger y, BigInteger P) {
        BigInteger result;

        if (y.equals(BigInteger.ONE)){
            return x;
        } else{
            result = x.modPow(y, P);
            return result;
        }
    }
}
