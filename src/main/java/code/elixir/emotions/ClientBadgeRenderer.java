package code.elixir.emotions;

import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.Render2D;
import java.util.UUID;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public final class ClientBadgeRenderer {
   private static final int CLIENT_ICON_TEXTURE_SIZE = 2100;
   private static Identifier clientIcon;

   private ClientBadgeRenderer() {
   }

   private static Identifier getClientIcon() {
      if (clientIcon == null) {
         clientIcon = Identifier.of("mre", "elixir_icon.png");
      }

      return clientIcon;
   }

   public static boolean hasCompatibleClient(UUID uuid) {
      EmotionWheelManager manager = EmotionWheelManager.getInstance();
      return manager != null && manager.hasCompatibleClient(uuid);
   }

   public static boolean hasCompatibleClient(PlayerEntity player) {
      return player != null && hasCompatibleClient(player.getUuid());
   }

   public static void drawRoundedBadge(DrawContext context, float x, float y, float size) {
      try {
         Render2D.drawTexture(
            context, getClientIcon(), x, y, size, size / 2.0F, 2100, 2100, 2100, ColorAssist.rgba(255, 255, 255, 255), ColorAssist.rgba(255, 255, 255, 255)
         );
      } catch (Exception var5) {
      }
   }
}
