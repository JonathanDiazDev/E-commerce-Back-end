package com.jonathan.ecommerce.util;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;

public class HashUtil {
    public static String hashToken(String token){
        return Hashing.sha256()
                .hashString(token, StandardCharsets.UTF_8)
                .toString();
    }
}
