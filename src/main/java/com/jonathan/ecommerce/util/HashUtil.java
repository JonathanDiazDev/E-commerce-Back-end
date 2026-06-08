package com.jonathan.ecommerce.util;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;

public class HashUtil {
  public static String hashToken(String token) {
    if (token == null || token.isEmpty()) {
      throw new IllegalArgumentException("Token cannot be null or empty");
    }
    return Hashing.sha256().hashString(token, StandardCharsets.UTF_8).toString();
  }
}
