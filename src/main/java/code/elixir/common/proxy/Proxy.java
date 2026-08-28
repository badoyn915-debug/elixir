package code.elixir.common.proxy;

import com.google.gson.annotations.SerializedName;

public class Proxy {
   @SerializedName("IP:PORT")
   public String ipPort = "";
   public Proxy.ProxyType type = Proxy.ProxyType.SOCKS5;
   public String username = "";
   public String password = "";

   public Proxy() {
   }

   public Proxy(boolean isSocks4, String ipPort, String username, String password) {
      this.type = isSocks4 ? Proxy.ProxyType.SOCKS4 : Proxy.ProxyType.SOCKS5;
      this.ipPort = ipPort;
      this.username = username;
      this.password = password;
   }

   public int getPort() {
      if (this.ipPort != null && !this.ipPort.isEmpty() && this.ipPort.contains(":")) {
         try {
            return Integer.parseInt(this.ipPort.split(":")[1]);
         } catch (NumberFormatException | ArrayIndexOutOfBoundsException var2) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public String getIp() {
      return this.ipPort != null && !this.ipPort.isEmpty() && this.ipPort.contains(":") ? this.ipPort.split(":")[0] : "";
   }

   public static enum ProxyType {
      SOCKS4,
      SOCKS5;
   }
}
