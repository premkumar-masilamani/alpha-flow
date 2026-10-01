package com.alphaflow.engine.downloaders.angelone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alphaflow.engine.configs.AngelOneConfig;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AngelOneNetworkHelperTest {

  private AngelOneConfig config;
  private AngelOneNetworkHelper networkHelper;

  @BeforeEach
  void setUp() {
    config = new AngelOneConfig();
    networkHelper = new AngelOneNetworkHelper(config);
  }

  @Test
  void testGetMacAddressConfigured() {
    config.setMacAddress("AA:BB:CC:DD:EE:FF");
    assertEquals("AA:BB:CC:DD:EE:FF", networkHelper.getMacAddress());
  }

  @Test
  void testGetMacAddressDynamicBlankAndNull() {
    config.setMacAddress("");
    String macBlank = networkHelper.getMacAddress();
    assertNotNull(macBlank);
    assertFalse(macBlank.isBlank());

    config.setMacAddress(null);
    String macNull = networkHelper.getMacAddress();
    assertNotNull(macNull);
    assertFalse(macNull.isBlank());
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
        new AngelOneNetworkHelper(config) {
          @Override
          String extractMacAddress(Enumeration<NetworkInterface> networkInterfaces)
              throws Exception {
            throw new RuntimeException("Simulated network interface error");
          }
        };
    assertEquals("02:00:00:00:00:00", helperWithException.resolveSystemMacAddress());
  }

  @Test
  void testGetClientLocalIpConfigured() {
    config.setClientLocalIp("test-local-ip");
    assertEquals("test-local-ip", networkHelper.getClientLocalIp());
  }

  @Test
  void testGetClientLocalIpDynamicBlankAndNull() {
    config.setClientLocalIp("");
    String localIpBlank = networkHelper.getClientLocalIp();
    assertNotNull(localIpBlank);
    assertFalse(localIpBlank.isBlank());

    config.setClientLocalIp(null);
    String localIpNull = networkHelper.getClientLocalIp();
    assertNotNull(localIpNull);
    assertFalse(localIpNull.isBlank());
  }

  @Test
  void testResolveSystemLocalIpExceptionFallback() {
    String fallbackIp = InetAddress.getLoopbackAddress().getHostAddress();
    AngelOneNetworkHelper helperWithException =
        new AngelOneNetworkHelper(config) {
          @Override
          protected String resolveSystemLocalIp() {
            try {
              throw new RuntimeException("Simulated host resolution error");
            } catch (Exception exception) {
              return fallbackIp;
            }
          }
        };
    assertEquals(fallbackIp, helperWithException.getClientLocalIp());
  }

  @Test
  void testGetClientPublicIpConfigured() {
    config.setClientPublicIp("test-public-ip");
    assertEquals("test-public-ip", networkHelper.getClientPublicIp());
  }

  @Test
  void testGetClientPublicIpFallbackToLocal() {
    config.setClientPublicIp("");
    config.setClientLocalIp("test-fallback-ip");
    assertEquals("test-fallback-ip", networkHelper.getClientPublicIp());

    config.setClientPublicIp(null);
    assertEquals("test-fallback-ip", networkHelper.getClientPublicIp());
  }

  @Test
  void testApplyHeadersAndAuthenticatedHeaders() {
    config.setApiKey("test-key");
    config.setClientLocalIp("test-local-ip");
    config.setClientPublicIp("test-public-ip");
    config.setMacAddress("11:22:33:44:55:66");

    HttpURLConnection connection = mock(HttpURLConnection.class);

    networkHelper.applyHeaders(connection, config.getApiKey());

    verify(connection).setRequestProperty("Content-Type", "application/json");
    verify(connection).setRequestProperty("Accept", "application/json");
    verify(connection).setRequestProperty("X-PrivateKey", "test-key");
    verify(connection).setRequestProperty("X-UserType", "USER");
    verify(connection).setRequestProperty("X-SourceID", "WEB");
    verify(connection).setRequestProperty("X-ClientLocalIP", "test-local-ip");
    verify(connection).setRequestProperty("X-ClientPublicIP", "test-public-ip");
    verify(connection).setRequestProperty("X-MACaddress", "11:22:33:44:55:66");

    HttpURLConnection authConnection = mock(HttpURLConnection.class);
    networkHelper.applyAuthenticatedHeaders(authConnection, config.getApiKey(), "test-jwt-token");
    verify(authConnection).setRequestProperty("Authorization", "Bearer " + "test-jwt-token");
    verify(authConnection).setRequestProperty("X-PrivateKey", "test-key");
  }
}
