package code.elixir.utils.client.auth;

import code.elixir.utils.client.logs.Logger;
import java.io.File;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.UUID;

public final class HardwareFingerprint {
   private static volatile String cachedHwid;

   private HardwareFingerprint() {
   }

   public static String computeStableHwid() {
      String cached = cachedHwid;
      if (cached != null && !cached.isBlank()) {
         return cached;
      } else {
         Class<HardwareFingerprint> var2 = HardwareFingerprint.class;
         synchronized (HardwareFingerprint.class) {
            if (cachedHwid != null && !cachedHwid.isBlank()) {
               return cachedHwid;
            } else {
               cachedHwid = buildHwid();
               return cachedHwid;
            }
         }
      }
   }

   private static String buildHwid() {
      try {
         String material = buildFingerprintMaterial();
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
         return bytesToUuid(hash).toString();
      } catch (Exception var3) {
         Logger.warn("[HardwareFingerprint] Fallback HWID used: " + var3.getMessage());
         String fallback = "Elixir-HWID-FALLBACK|" + safe(System.getProperty("user.name")) + "|" + safe(System.getProperty("os.name"));
         return UUID.nameUUIDFromBytes(fallback.getBytes(StandardCharsets.UTF_8)).toString();
      }
   }

   private static String buildFingerprintMaterial() {
      StringJoiner joiner = new StringJoiner("|");
      append(joiner, "os.name", System.getProperty("os.name"));
      append(joiner, "os.version", System.getProperty("os.version"));
      append(joiner, "os.arch", System.getProperty("os.arch"));
      append(joiner, "java.vendor", System.getProperty("java.vendor"));
      append(joiner, "computer", System.getenv("COMPUTERNAME"));
      append(joiner, "hostname", System.getenv("HOSTNAME"));
      append(joiner, "domain", System.getenv("USERDOMAIN"));
      append(joiner, "proc.id", System.getenv("PROCESSOR_IDENTIFIER"));
      append(joiner, "proc.arch", System.getenv("PROCESSOR_ARCHITECTURE"));
      appendNetworkData(joiner);
      appendDiskData(joiner);
      return joiner.toString();
   }

   private static void appendNetworkData(StringJoiner joiner) {
      ArrayList<String> macs = new ArrayList<>();

      try {
         Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
         if (interfaces != null) {
            while (interfaces.hasMoreElements()) {
               NetworkInterface networkInterface = interfaces.nextElement();
               byte[] hardwareAddress;
               if (networkInterface != null
                  && !networkInterface.isLoopback()
                  && !networkInterface.isVirtual()
                  && networkInterface.isUp()
                  && (hardwareAddress = networkInterface.getHardwareAddress()) != null
                  && hardwareAddress.length != 0) {
                  macs.add(networkInterface.getName() + "=" + toHex(hardwareAddress));
               }
            }
         }
      } catch (Exception var5) {
         Logger.warn("[HardwareFingerprint] Network enumeration failed: " + var5.getMessage());
      }

      Collections.sort(macs);

      for (String string : macs) {
         append(joiner, "net", string);
      }
   }

   private static void appendDiskData(StringJoiner joiner) {
      try {
         File[] roots = File.listRoots();
         if (roots == null) {
            return;
         }

         ArrayList<String> rootData = new ArrayList<>();

         for (File root : roots) {
            if (root != null) {
               rootData.add(root.getAbsolutePath() + ":" + root.getTotalSpace());
            }
         }

         Collections.sort(rootData);

         for (String string : rootData) {
            append(joiner, "disk", string);
         }
      } catch (Exception var7) {
         Logger.warn("[HardwareFingerprint] Disk info unavailable: " + var7.getMessage());
      }
   }

   private static UUID bytesToUuid(byte[] bytes) {
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      long msb = buffer.getLong();
      long lsb = buffer.getLong();
      return new UUID(msb, lsb);
   }

   private static String toHex(byte[] bytes) {
      StringBuilder builder = new StringBuilder(bytes.length * 2);

      for (byte value : bytes) {
         builder.append(String.format(Locale.ROOT, "%02x", value));
      }

      return builder.toString();
   }

   private static void append(StringJoiner joiner, String key, String value) {
      if (value != null && !value.isBlank()) {
         joiner.add(key + "=" + value.trim());
      }
   }

   private static String safe(String value) {
      return value == null ? "" : value.trim();
   }
}
