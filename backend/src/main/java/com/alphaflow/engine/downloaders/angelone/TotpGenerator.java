package com.alphaflow.engine.downloaders.angelone;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class TotpGenerator {

  private static final String HMAC_SHA1 = "HmacSHA1";
  private static final int TIME_STEP_SECONDS = 30;
  private static final int DIGITS = 6;
  private static final int MODULUS = 1_000_000;

  public String generateCurrentTotp(String base32Secret) {
    return generateTotp(base32Secret, Instant.now().getEpochSecond());
  }

  public String generateTotp(String base32Secret, long epochSeconds) {
    if (base32Secret == null || base32Secret.trim().isEmpty()) {
      throw new IllegalArgumentException("TOTP secret cannot be null or empty");
    }
    long timeStep = epochSeconds / TIME_STEP_SECONDS;
    byte[] key = decodeBase32(base32Secret);
    byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();

    try {
      Mac mac = Mac.getInstance(HMAC_SHA1);
      mac.init(new SecretKeySpec(key, HMAC_SHA1));
      byte[] hash = mac.doFinal(timeBytes);

      int offset = hash[hash.length - 1] & 0x0F;
      int binary =
          ((hash[offset] & 0x7F) << 24)
              | ((hash[offset + 1] & 0xFF) << 16)
              | ((hash[offset + 2] & 0xFF) << 8)
              | (hash[offset + 3] & 0xFF);

      int otp = binary % MODULUS;
      return String.format("%0" + DIGITS + "d", otp);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Failed to generate TOTP code", e);
    }
  }

  public byte[] decodeBase32(String base32) {
    String clean = base32.trim().toUpperCase(Locale.ROOT).replaceAll("[=\\s]", "");
    if (clean.isEmpty()) {
      return new byte[0];
    }

    int numBytes = (clean.length() * 5) / 8;
    byte[] result = new byte[numBytes];

    int buffer = 0;
    int bitsLeft = 0;
    int index = 0;

    for (int i = 0; i < clean.length(); i++) {
      char c = clean.charAt(i);
      int val;
      if (c >= 'A' && c <= 'Z') {
        val = c - 'A';
      } else if (c >= '2' && c <= '7') {
        val = c - '2' + 26;
      } else {
        throw new IllegalArgumentException("Illegal character in Base32 secret: " + c);
      }

      buffer = (buffer << 5) | (val & 31);
      bitsLeft += 5;
      if (bitsLeft >= 8) {
        bitsLeft -= 8;
        if (index < result.length) {
          result[index++] = (byte) ((buffer >> bitsLeft) & 0xFF);
        }
      }
    }
    return result;
  }
}
