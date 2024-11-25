package itts.volterra.quintab;

import java.math.BigInteger;

public class DiffieHellman {
    BigInteger nPrimo, base;

    public DiffieHellman(BigInteger nPrimo, BigInteger base) {
        this.nPrimo = nPrimo;
        this.base = base;
    }
}
