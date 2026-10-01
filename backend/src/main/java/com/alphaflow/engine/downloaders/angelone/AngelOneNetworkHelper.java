package com.alphaflow.engine.downloaders.angelone;

import com.alphaflow.engine.configs.AngelOneConfig;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AngelOneNetworkHelper {

  private static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";
  private static final String DEFAULT_LOCAL_IP = InetAddress.getLoopbackAddress().getHostAddress();

  private final AngelOneConfig config;

  public AngelOneNetworkHelper(AngelOneConfig config) {
    this.config = config;
  }

  public String getMacAddress() {
    if (config.getMacAddress() != null && !config.getMacAddress().isBlank()) {
      return config.getMacAddress();
    } else {
      return resolveSystemMacAddress();
    }
  }

  public String getClientLocalIp() {
    if (config.getClientLocalIp() != null && !config.getClientLocalIp().isBlank()) {
      return config.getClientLocalIp();
    } else {
      return resolveSystemLocalIp();
    }
  }

  public String getClientPublicIp() {
    if (config.getClientPublicIp() != null && !config.getClientPublicIp().isBlank()) {
      return config.getClientPublicIp();
    } else {
      return getClientLocalIp();
    }
  }

  public void applyHeaders(HttpURLConnection connection, String apiKey) {
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setRequestProperty("Accept", "application/json");
    connection.setRequestProperty("X-PrivateKey", apiKey);
    connection.setRequestProperty("X-UserType", "USER");
    connection.setRequestProperty("X-SourceID", "WEB");
    connection.setRequestProperty("X-ClientLocalIP", getClientLocalIp());
    connection.setRequestProperty("X-ClientPublicIP", getClientPublicIp());
    connection.setRequestProperty("X-MACaddress", getMacAddress());
  }

  public void applyAuthenticatedHeaders(
      HttpURLConnection connection, String apiKey, String jwtToken) {
    applyHeaders(connection, apiKey);
    connection.setRequestProperty("Authorization", "Bearer " + jwtToken);
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
