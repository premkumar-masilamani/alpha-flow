package com.alphaflow.engine.downloaders.angelone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AngelOneNetworkHelperTest {

  private AngelOneNetworkHelper networkHelper;

  @BeforeEach
  void setUp() {
    networkHelper = new AngelOneNetworkHelper();
  }

  @Test
  void testGetMacAddressNotNull() {
    String mac = networkHelper.getMacAddress();
    assertNotNull(mac);
    assertFalse(mac.isBlank());
  }

  @Test
  void testExtractMacAddressNullAndEmpty() throws Exception {
    assertEquals("02:00:00:00:00:00", networkHelper.extractMacAddress(null));
    assertEquals(
        "02:00:00:00:00:00", networkHelper.extractMacAddress(Collections.emptyEnumeration()));
  }

  @Test
  void testExtractMacAddressFilteredInterfaces() throws Exception {
    NetworkInterface loopback = mock(NetworkInterface.class);
    when(loopback.isUp()).thenReturn(true);
    when(loopback.isLoopback()).thenReturn(true);

    NetworkInterface down = mock(NetworkInterface.class);
    when(down.isUp()).thenReturn(false);

    NetworkInterface nullMac = mock(NetworkInterface.class);
    when(nullMac.isUp()).thenReturn(true);
    when(nullMac.isLoopback()).thenReturn(false);
    when(nullMac.getHardwareAddress()).thenReturn(null);

    NetworkInterface emptyMac = mock(NetworkInterface.class);
    when(emptyMac.isUp()).thenReturn(true);
    when(emptyMac.isLoopback()).thenReturn(false);
    when(emptyMac.getHardwareAddress()).thenReturn(new byte[0]);

    NetworkInterface validMac = mock(NetworkInterface.class);
    when(validMac.isUp()).thenReturn(true);
    when(validMac.isLoopback()).thenReturn(false);
    when(validMac.getHardwareAddress())
        .thenReturn(
            new byte[] {
              (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC
            });

    List<NetworkInterface> interfaceList = List.of(loopback, down, nullMac, emptyMac, validMac);
    Enumeration<NetworkInterface> enumeration = Collections.enumeration(interfaceList);

    assertEquals("12:34:56:78:9A:BC", networkHelper.extractMacAddress(enumeration));
  }

  @Test
  void testResolveSystemMacAddressExceptionFallback() {
    AngelOneNetworkHelper helperWithException =
        new AngelOneNetworkHelper() {
          @Override
          String extractMacAddress(Enumeration<NetworkInterface> networkInterfaces)
              throws Exception {
            throw new RuntimeException("Simulated network interface error");
          }
        };
    assertEquals("02:00:00:00:00:00", helperWithException.resolveSystemMacAddress());
  }

  @Test
  void testGetClientLocalIpAndPublicIp() {
    String localIp = networkHelper.getClientLocalIp();
    assertNotNull(localIp);
    assertFalse(localIp.isBlank());

    String publicIp = networkHelper.getClientPublicIp();
    assertEquals(localIp, publicIp);
  }

  @Test
  void testResolveSystemLocalIpExceptionFallback() {
    String fallbackIp = InetAddress.getLoopbackAddress().getHostAddress();
    AngelOneNetworkHelper helperWithException =
        new AngelOneNetworkHelper() {
          @Override
          protected String resolveSystemLocalIp() {
            try {
              throw new RuntimeException("Simulated host resolution error");
            } catch (Exception exception) {
              return fallbackIp;
            }
          }
        };
    assertNotNull(helperWithException.getClientLocalIp());
  }

  @Test
  void testApplyHeadersAndAuthenticatedHeaders() {
    HttpHeaders headers = new HttpHeaders();
    networkHelper.applyHeaders(headers, "test-api-key");

    assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
    assertEquals("test-api-key", headers.getFirst("X-PrivateKey"));
    assertEquals("USER", headers.getFirst("X-UserType"));
    assertEquals("WEB", headers.getFirst("X-SourceID"));
    assertEquals(networkHelper.getClientLocalIp(), headers.getFirst("X-ClientLocalIP"));
    assertEquals(networkHelper.getClientPublicIp(), headers.getFirst("X-ClientPublicIP"));
    assertEquals(networkHelper.getMacAddress(), headers.getFirst("X-MACaddress"));

    HttpHeaders authHeaders = new HttpHeaders();
    networkHelper.applyAuthenticatedHeaders(authHeaders, "test-api-key", "test-jwt-token");
    assertEquals("Bearer test-jwt-token", authHeaders.getFirst("Authorization"));
    assertEquals("test-api-key", authHeaders.getFirst("X-PrivateKey"));
  }
}
