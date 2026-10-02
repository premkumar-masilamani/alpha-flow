package com.alphaflow.engine.downloaders.angelone;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AngelOneNetworkHelper {

  private static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";
  private static final String DEFAULT_LOCAL_IP = InetAddress.getLoopbackAddress().getHostAddress();

  private String cachedMacAddress;
  private String cachedLocalIp;

  public synchronized String getMacAddress() {
    if (cachedMacAddress == null) {
      cachedMacAddress = resolveSystemMacAddress();
    }
    return cachedMacAddress;
  }

  public synchronized String getClientLocalIp() {
    if (cachedLocalIp == null) {
      cachedLocalIp = resolveSystemLocalIp();
    }
    return cachedLocalIp;
  }

  public String getClientPublicIp() {
    return getClientLocalIp();
  }

  public void applyHeaders(HttpHeaders headers, String apiKey) {
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.set("X-PrivateKey", apiKey);
    headers.set("X-UserType", "USER");
    headers.set("X-SourceID", "WEB");
    headers.set("X-ClientLocalIP", getClientLocalIp());
    headers.set("X-ClientPublicIP", getClientPublicIp());
    headers.set("X-MACaddress", getMacAddress());
  }

  public void applyAuthenticatedHeaders(HttpHeaders headers, String apiKey, String jwtToken) {
    applyHeaders(headers, apiKey);
    headers.setBearerAuth(jwtToken);
  }

  protected String resolveSystemMacAddress() {
    try {
      return extractMacAddress(NetworkInterface.getNetworkInterfaces());
    } catch (Exception exception) {
      log.debug("Unable to determine hardware MAC address: {}", exception.getMessage());
      return DEFAULT_MAC_ADDRESS;
    }
  }

  String extractMacAddress(Enumeration<NetworkInterface> networkInterfaces) throws Exception {
    if (networkInterfaces != null) {
      while (networkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = networkInterfaces.nextElement();
        if (networkInterface.isUp() && !networkInterface.isLoopback()) {
          byte[] hardwareAddress = networkInterface.getHardwareAddress();
          if (hardwareAddress != null && hardwareAddress.length > 0) {
            StringBuilder macBuilder = new StringBuilder();
            for (int index = 0; index < hardwareAddress.length; index++) {
              macBuilder.append(
                  String.format(
                      "%02X%s",
                      hardwareAddress[index], (index < hardwareAddress.length - 1) ? ":" : ""));
            }
            return macBuilder.toString();
          }
        }
      }
    }
    return DEFAULT_MAC_ADDRESS;
  }

  protected String resolveSystemLocalIp() {
    try {
      InetAddress localHost = InetAddress.getLocalHost();
      return localHost.getHostAddress();
    } catch (Exception exception) {
      log.debug("Unable to determine local host IP: {}", exception.getMessage());
      return DEFAULT_LOCAL_IP;
    }
  }
}
