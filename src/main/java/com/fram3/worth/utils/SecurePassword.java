package com.fram3.worth.utils;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SecurePassword modella l'hashing di una password per la persistenza nel server
 * e operazioni per futuri confronti tra password fornita e salvata
 *
 * @author Francesco Amodeo
 * @version 1.0
 */
public class SecurePassword {

    /**
     * maggiore sarà il numero di iterzione più
     * sarà difficile risalire alla password */
    private static final int iterations = 20*1000;

    /**
     * numero bytes del salt.
     * Il salt fa si che anche se due utenti scelgono la stessa password
     * l'hash di esse sarà sempre diverso */
    private static final int saltLen = 32;

    /**
     * lunghezza dell'hash della password */
    private static final int desiredKeyLen = 256;



    /**
     * generazione dell'hashing della password da salvare nel server
     *
     * @param password password in plain text di cui fare l'hashing
     * @return l'hash della password in codifica Base64 da persistere nel server
     * @throws Exception -
     */
    public static String getSaltedHash(String password) throws Exception {
        byte[] salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(saltLen);
        return Base64.getEncoder().withoutPadding().encodeToString(salt) + "$" + hash(password, salt);
    }


    /**
     * confronto tra password fornita e quella salvata nel server
     *
     * @param password password in plain text da confrontare
     * @param stored password salvata nel server
     * @return true se l'hash della password fornita è uguale a quello salvato
     * @throws Exception -
     */
    public static boolean check(String password, String stored) throws Exception{
        String[] saltAndHash = stored.split("\\$");
        if (saltAndHash.length != 2)
            throw new IllegalStateException("La password stored deve avere la forma 'salt$hash'");

        String hashOfInput = hash(password, Base64.getDecoder().decode((saltAndHash[0])));
        return hashOfInput.equals(saltAndHash[1]);
    }


    /**
     * hash della password con il salt fornito, utilizza l'algoritmo PBKDF2
     *
     * @param password password in plain text di cui fare l'hashing
     * @param salt salt da aggiungere alla hashing
     * @return l'hashing della password in codifica Base64
     * @throws Exception -
     */
    private static String hash(String password, byte[] salt) throws Exception {
        if (password == null || password.length() == 0)
            throw new IllegalArgumentException("Non sono supportate password vuote");

        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        SecretKey key = f.generateSecret(
                new PBEKeySpec(password.toCharArray(), salt, iterations, desiredKeyLen));
        return Base64.getEncoder().withoutPadding().encodeToString(key.getEncoded());
    }
}
