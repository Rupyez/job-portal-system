package com.jobportal.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class TokenHashUtil {

    private TokenHashUtil(){
        //preventing creating instances of this utility class.
    }


    public static String sha256(String token){
        if(token == null || token.isBlank()){
            throw new IllegalArgumentException("Token must not be null or blank");
        }

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        }catch (NoSuchAlgorithmException ex){
            throw new IllegalArgumentException("SHA-256 algorithm is not available", ex);
        }
    }
}
