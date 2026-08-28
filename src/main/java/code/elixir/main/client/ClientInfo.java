package code.elixir.main.client;

import code.elixir.utils.client.chat.StringHelper;
import java.io.File;

public record ClientInfo(String clientName, String userName, String role, File clientDir, File filesDir, File configsDir) implements ClientInfoProvider {
   @Override
   public String getFullInfo() {
      return String.format("Welcome! Client: %s Version: %s Branch: %s", this.clientName, "читак вроде", StringHelper.getUserRole());
   }
}
