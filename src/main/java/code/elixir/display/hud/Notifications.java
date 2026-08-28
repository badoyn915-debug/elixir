package code.elixir.display.hud;

import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Notifications extends AbstractDraggable {
   private static final Notifications INSTANCE = new Notifications();

   private Notifications() {
      super("Notifications", 0, 350, 120, 20, false);
   }

   public static Notifications getInstance() {
      return INSTANCE;
   }

   public void addList(String text, int durationMs) {
      this.push(text, DynamicIsland.NotificationType.DEFAULT);
   }

   public void addList(String text, int durationMs, boolean error) {
      this.push(text, error ? DynamicIsland.NotificationType.ERROR : DynamicIsland.NotificationType.SUCCESS);
   }

   public void addList(String text, int durationMs, Object sound) {
      this.push(text, DynamicIsland.NotificationType.DEFAULT);
   }

   public void addList(String text, int durationMs, Object sound, boolean error, ItemStack icon) {
      this.push(text, error ? DynamicIsland.NotificationType.ERROR : DynamicIsland.NotificationType.DEFAULT);
   }

   public void addList(Text text, int durationMs) {
      this.push(text == null ? null : text.getString(), DynamicIsland.NotificationType.DEFAULT);
   }

   public void addList(Text text, int durationMs, Object sound) {
      this.push(text == null ? null : text.getString(), DynamicIsland.NotificationType.DEFAULT);
   }

   public void addList(Text text, int durationMs, Object sound, boolean error, ItemStack icon) {
      this.push(text == null ? null : text.getString(), error ? DynamicIsland.NotificationType.ERROR : DynamicIsland.NotificationType.DEFAULT);
   }

   private void push(String text, DynamicIsland.NotificationType type) {
      String normalized = this.normalize(text);
      if (normalized != null && !normalized.isEmpty()) {
         DynamicIsland.addNotification(normalized, type);
      }
   }

   private String normalize(String text) {
      if (text == null) {
         return "";
      } else {
         String stripped = Formatting.strip(text);
         return stripped == null ? text : stripped;
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
   }
}
