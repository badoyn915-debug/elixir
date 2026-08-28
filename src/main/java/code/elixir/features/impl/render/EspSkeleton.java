package code.elixir.features.impl.render;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.display.screens.clickgui.MenuScreen;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.WorldLoadEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;

public class EspSkeleton extends Module {
   private final List<PlayerEntity> players = new ArrayList<>();
   private final MultiSelectSetting entityType = new MultiSelectSetting("Показывать", "Игроки, для которых рисуется скелетон")
      .value("Игроков", "Друзья")
      .selected("Игроков");
   private final SliderSettings thickness = new SliderSettings("Толщина", "Толщина линий скелета").setValue(2.0F).range(0.5F, 5.0F).step(0.5F);
   private final ColorSetting color = new ColorSetting("Цвет", "Цвет линий скелетона").value(new Color(255, 255, 255, 255).getRGB());

   public static EspSkeleton getInstance() {
      return Instance.get(EspSkeleton.class);
   }

   public EspSkeleton() {
      super("EspSkeleton", "EspSkeleton", ModuleCategory.ESP);
      this.setup(new Setting[]{this.entityType, this.thickness, this.color});
   }

   @EventHandler
   public void onWorldLoad(WorldLoadEvent e) {
      this.players.clear();
   }

   @EventHandler
   public void onTick(TickEvent e) {
      this.players.clear();
      if (mc.world != null) {
         mc.world
            .getPlayers()
            .stream()
            .filter(player -> player != mc.player)
            .filter(player -> player.getCustomName() == null || !player.getCustomName().getString().startsWith("Ghost_"))
            .forEach(this.players::add);
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (mc.player != null && mc.world != null) {
         if (!(mc.currentScreen instanceof MenuScreen)) {
            if (mc.getEntityRenderDispatcher() != null && mc.getEntityRenderDispatcher().camera != null) {
               boolean selPlayers = this.entityType.isSelected("Игроков");
               boolean selFriends = this.entityType.isSelected("Друзья");
               if (selPlayers || selFriends) {
                  ArrayList<PlayerEntity> skeletonTargets = new ArrayList<>(this.players.size() + 1);
                  if (selPlayers) {
                     skeletonTargets.add(mc.player);
                  }

                  for (PlayerEntity player : this.players) {
                     if (player != null && (player.getCustomName() == null || !player.getCustomName().getString().startsWith("Ghost_"))) {
                        boolean friend = FriendUtils.isFriend(player);
                        if (selPlayers || selFriends && friend) {
                           skeletonTargets.add(player);
                        }
                     }
                  }

                  EspSkeletonRenderer.renderPlayers(
                     e.getStack(), skeletonTargets, mc.getRenderTickCounter().getTickDelta(false), this.thickness.getValue(), this.color.getColor()
                  );
               }
            }
         }
      }
   }
}
