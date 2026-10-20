package com.alphaflow.engine.downloaders.angelone;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AngelOneTotpGeneratorTest {

  private AngelOneTotpGenerator angelOneTotpGenerator;

  @BeforeEach
  void setUp() {
    angelOneTotpGenerator = new AngelOneTotpGenerator();
  }

  @Test
  void testGenerateTotpWithKnownSecret() {
    // Secret: "JBSWY3DPEHPK3PXP" (Base32 for "Hello!\xde\xad\xbe\xef")
    String secret = "JBSWY3DPEHPK3PXP";
    // At epoch 59 (timeStep = 1)
    String otp = angelOneTotpGenerator.generateTotp(secret, 59L);
    assertNotNull(otp);
    assertEquals(6, otp.length());

    // Same time step should yield identical OTP
    assertEquals(otp, angelOneTotpGenerator.generateTotp(secret, 30L));
    assertEquals(otp, angelOneTotpGenerator.generateTotp(secret, 59L));

    // Next time step at 60L
    String nextOtp = angelOneTotpGenerator.generateTotp(secret, 60L);
    assertNotNull(nextOtp);
    assertEquals(6, nextOtp.length());
  }

  @Test
  void testGenerateCurrentTotp() {
    String secret = "JBSWY3DPEHPK3PXP";
    String currentOtp = angelOneTotpGenerator.generateCurrentTotp(secret);
    assertNotNull(currentOtp);
    assertEquals(6, currentOtp.length());
  }

  @Test
  void testNullOrEmptySecretThrows() {
    assertThrows(IllegalArgumentException.class, () -> angelOneTotpGenerator.generateTotp(null, 100L));
    assertThrows(IllegalArgumentException.class, () -> angelOneTotpGenerator.generateTotp("   ", 100L));
  }

  @Test
  void testDecodeBase32Variations() {
    // Test spaces, padding '=', and lowercase
    byte[] decoded1 = angelOneTotpGenerator.decodeBase32("JBSWY3DPEHPK3PXP");
    byte[] decoded2 = angelOneTotpGenerator.decodeBase32(" jbswy3dp ehpk3pxp== ");
    assertArrayEquals(decoded1, decoded2);

    // Empty string
    assertEquals(0, angelOneTotpGenerator.decodeBase32("").length);
  }

  @Test
  void testInvalidBase32CharacterThrows() {
    assertThrows(
        IllegalArgumentException.class, () -> angelOneTotpGenerator.decodeBase32("INVALID!CHAR89"));
  }
}
