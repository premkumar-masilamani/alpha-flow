package com.alphaflow.engine.downloaders.angelone;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TotpGeneratorTest {

  private TotpGenerator totpGenerator;

  @BeforeEach
  void setUp() {
    totpGenerator = new TotpGenerator();
  }

  @Test
  void testGenerateTotpWithKnownSecret() {
    // Secret: "JBSWY3DPEHPK3PXP" (Base32 for "Hello!\xde\xad\xbe\xef")
    String secret = "JBSWY3DPEHPK3PXP";
    // At epoch 59 (timeStep = 1)
    String otp = totpGenerator.generateTotp(secret, 59L);
    assertNotNull(otp);
    assertEquals(6, otp.length());

    // Same time step should yield identical OTP
    assertEquals(otp, totpGenerator.generateTotp(secret, 30L));
    assertEquals(otp, totpGenerator.generateTotp(secret, 59L));

    // Next time step at 60L
    String nextOtp = totpGenerator.generateTotp(secret, 60L);
    assertNotNull(nextOtp);
    assertEquals(6, nextOtp.length());
  }

  @Test
  void testGenerateCurrentTotp() {
    String secret = "JBSWY3DPEHPK3PXP";
    String currentOtp = totpGenerator.generateCurrentTotp(secret);
    assertNotNull(currentOtp);
    assertEquals(6, currentOtp.length());
  }

  @Test
  void testNullOrEmptySecretThrows() {
    assertThrows(IllegalArgumentException.class, () -> totpGenerator.generateTotp(null, 100L));
    assertThrows(IllegalArgumentException.class, () -> totpGenerator.generateTotp("   ", 100L));
  }

  @Test
  void testDecodeBase32Variations() {
    // Test spaces, padding '=', and lowercase
    byte[] decoded1 = totpGenerator.decodeBase32("JBSWY3DPEHPK3PXP");
    byte[] decoded2 = totpGenerator.decodeBase32(" jbswy3dp ehpk3pxp== ");
    assertArrayEquals(decoded1, decoded2);

    // Empty string
    assertEquals(0, totpGenerator.decodeBase32("").length);
  }

  @Test
  void testInvalidBase32CharacterThrows() {
    assertThrows(
        IllegalArgumentException.class, () -> totpGenerator.decodeBase32("INVALID!CHAR89"));
  }
}
