package code.elixir.features.impl.movement.autoMystGlider.states;

import code.elixir.utils.display.interfaces.QuickImports;

public class StacheState implements QuickImports {
   private final int anarchy;
   private final String homeName;
   private boolean teleported = false;
   private boolean connected = false;

   public StacheState(int anarchy, String homeName) {
      this.anarchy = anarchy;
      this.homeName = homeName;
   }

   public void connect() {
      mc.getNetworkHandler().sendCommand("an" + this.anarchy);
      this.connected = true;
   }

   public void teleport() {
      mc.getNetworkHandler().sendCommand("home " + this.homeName);
      this.teleported = true;
   }

   public void reload() {
      this.teleported = false;
      this.connected = false;
   }

   public int getAnarchy() {
      return this.anarchy;
   }

   public String getHomeName() {
      return this.homeName;
   }

   public boolean isTeleported() {
      return this.teleported;
   }

   public boolean isConnected() {
      return this.connected;
   }
}
