package code.elixir.utils.client.managers.api.draggable;

import code.elixir.display.hud.ArmorBar;
import code.elixir.display.hud.CoolDowns;
import code.elixir.display.hud.CustomHotbar;
import code.elixir.display.hud.DynamicIsland;
import code.elixir.display.hud.Info;
import code.elixir.display.hud.InventoryHud;
import code.elixir.display.hud.Keybinds;
import code.elixir.display.hud.Notifications;
import code.elixir.display.hud.Potions;
import code.elixir.display.hud.StaffList;
import code.elixir.display.hud.TargetHud;
import code.elixir.display.hud.TotemBar;
import java.util.ArrayList;
import java.util.List;

public class DraggableRepository {
   private final List<AbstractDraggable> draggable = new ArrayList<>();

   public void setup() {
      this.register(
         new DynamicIsland(),
         new TargetHud(),
         Notifications.getInstance(),
         new CoolDowns(),
         new Keybinds(),
         new Potions(),
         new StaffList(),
         Info.getInstance(),
         new ArmorBar(),
         new TotemBar(),
         new CustomHotbar(),
         InventoryHud.getInstance()
      );
   }

   public void register(AbstractDraggable... module) {
      this.draggable.addAll(List.of(module));
   }

   public List<AbstractDraggable> draggable() {
      return this.draggable;
   }

   public <T extends AbstractDraggable> T get(String name) {
      return this.draggable.stream().filter(module -> module.getName().equalsIgnoreCase(name)).map(module -> (T)module).findFirst().orElse(null);
   }

   public <T extends AbstractDraggable> T get(Class<T> clazz) {
      return this.draggable.stream().filter(module -> clazz.isAssignableFrom(module.getClass())).map(clazz::cast).findFirst().orElse(null);
   }
}
