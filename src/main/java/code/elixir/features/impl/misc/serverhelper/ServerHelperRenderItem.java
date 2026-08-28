package code.elixir.features.impl.misc.serverhelper;

import net.minecraft.item.Item;

public class ServerHelperRenderItem {
   private final String fullName;
   private final String shortName;
   private final String status;
   private final Item icon;
   private int bindKey = -1;
   private int tint = -1;

   public ServerHelperRenderItem(String fullName, String shortName, String status, Item icon) {
      this.fullName = fullName;
      this.shortName = shortName;
      this.status = status;
      this.icon = icon;
   }

   public ServerHelperRenderItem withBind(int bindKey) {
      this.bindKey = bindKey;
      return this;
   }

   public ServerHelperRenderItem withTint(int tint) {
      this.tint = tint;
      return this;
   }

   public String fullName() {
      return this.fullName;
   }

   public String shortName() {
      return this.shortName;
   }

   public String status() {
      return this.status;
   }

   public Item icon() {
      return this.icon;
   }

   public int bindKey() {
      return this.bindKey;
   }

   public int tint() {
      return this.tint;
   }

   public boolean hasTint() {
      return this.tint != -1;
   }
}
