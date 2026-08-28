package code.elixir.utils.protection;

import java.nio.charset.StandardCharsets;

public final class ProtectionRuntime {
   private static final int SEED = 0x5F3759DF;

   public static String decrypt(String encryptedHex, int key) {
      if (encryptedHex == null || encryptedHex.isEmpty()) {
         return "";
      }
      try {
         int len = encryptedHex.length() / 2;
         byte[] result = new byte[len];
         for (int i = 0; i < len; i++) {
            int byteVal = Integer.parseInt(encryptedHex.substring(i * 2, i * 2 + 2), 16);
            int k = (key ^ (i * 31 + SEED)) & 0xFF;
            result[i] = (byte)(byteVal ^ k);
         }
         return new String(result, StandardCharsets.UTF_8);
      } catch (Exception e) {
         return "";
      }
   }

   public static String decryptArray(int[] data, int key) {
      if (data == null || data.length == 0) {
         return "";
      }
      byte[] result = new byte[data.length];
      for (int i = 0; i < data.length; i++) {
         int k = (key ^ (i * 17 + SEED)) & 0xFF;
         result[i] = (byte)(data[i] ^ k);
      }
      return new String(result, StandardCharsets.UTF_8);
   }

   public static int resolveInt(int enc, int key) {
      return enc ^ key ^ SEED;
   }

   public static long resolveLong(long enc, long key) {
      return enc ^ key ^ (long)SEED;
   }

   private ProtectionRuntime() {}
}
