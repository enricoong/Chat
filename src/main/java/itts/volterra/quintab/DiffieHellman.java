package itts.volterra.quintab;

import java.math.BigInteger;

public class DiffieHellman {

    /*
    Alice e Bob si accordano di usare un numero primo p = 23 e la base g = 5.
    Alice sceglie un numero segreto a = 6 e manda a Bob A = ga mod p
        A = 56 mod 23 = 8
    Bob sceglie l'intero segreto b = 15 e manda ad Alice B = gb mod p
        B = 515 mod 23 = 19.
    Alice calcola KA = (gb mod p)a mod p = Ba mod p
        KA = 196 mod 23 = 2.
    Bob calcola KB = (ga mod p)b mod p = Ab mod p
        KB = 815 mod 23 = 2.
     */

    BigInteger nPrimo, base;

    public DiffieHellman(BigInteger nPrimo, BigInteger base) {
        this.nPrimo = nPrimo;
        this.base = base;
    }
}
