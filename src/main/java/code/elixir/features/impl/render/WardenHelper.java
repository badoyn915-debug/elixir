package code.elixir.features.impl.render;

import code.elixir.display.hud.Notifications;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.DrawEvent;
import code.elixir.events.render.WorldLoadEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.projection.Projection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class WardenHelper extends Module {
   private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2})\\s*:\\s*(\\d{2})");
   private static final ItemStack CHEST_ICON = Items.CHEST.getDefaultStack();
   private static final Identifier ARROW_ICON = Identifier.of("textures/arrow.png");
   private final SliderSettings rangeSetting = new SliderSettings("Радиус", "Дистанция поиска сундуков").range(10, 60).setValue(30.0F);
   private final SliderSettings tagSizeSetting = new SliderSettings("Размер тега", "Размер текста над сундуком").range(10, 20).setValue(13.0F);
   private final BooleanSetting notifySoon = new BooleanSetting("Уведомление 30с", "Показывать уведомление, когда сундук скоро откроется").setValue(true);
   private final BooleanSetting arrowsSetting = new BooleanSetting("Arrows", "Показывать стрелки к сундукам").setValue(false);
   private final SliderSettings arrowRadiusSetting = new SliderSettings("Радиус стрелок", "Дистанция стрелок от прицела")
      .setValue(50.0F)
      .range(30, 100)
      .visible(this.arrowsSetting::isValue);
   private final SliderSettings arrowSizeSetting = new SliderSettings("Размер стрелок", "Размер стрелок")
      .setValue(10.0F)
      .range(8, 20)
      .visible(this.arrowsSetting::isValue);
   private final Map<BlockPos, WardenHelper.ChestData> chests = new ConcurrentHashMap<>();

   public WardenHelper() {
      super("WardenHelper", "WardenHelper", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.rangeSetting, this.tagSizeSetting, this.notifySoon, this.arrowsSetting, this.arrowRadiusSetting, this.arrowSizeSetting});
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.chests.clear();
   }

   @EventHandler
   public void onWorldLoad(WorldLoadEvent e) {
      this.chests.clear();
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.world != null && mc.player != null) {
         if (mc.player.age % 5 == 0) {
            this.scanNearbyChests();
         }

         Vec3d playerPos = mc.player.getPos();
         double removeDistance = this.rangeSetting.getValue() + 10.0;
         this.chests.entrySet().removeIf(entry -> {
            WardenHelper.ChestData data = entry.getValue();
            return playerPos.distanceTo(entry.getKey().toCenterPos()) > removeDistance ? true : data.hasValidTimer() && data.getRemainingSeconds() <= 0;
         });
         if (this.notifySoon.isValue() && this.shouldSendChestNotification()) {
            this.chests.forEach((pos, data) -> this.maybeNotifySoon(pos, data));
         }
      }
   }

   private void scanNearbyChests() {
      int range = (int)this.rangeSetting.getValue();
      BlockPos playerPos = mc.player.getBlockPos();

      for (int x = -range; x <= range; x++) {
         for (int z = -range; z <= range; z++) {
            for (int y = -5; y <= 5; y++) {
               BlockPos pos = playerPos.add(x, y, z);
               if (mc.world.getBlockState(pos).getBlock() instanceof ChestBlock) {
                  this.updateChestTimer(pos);
               }
            }
         }
      }
   }

   private void updateChestTimer(BlockPos pos) {
      Vec3d center = pos.toCenterPos();
      Box searchBox = new Box(center.x - 1.0, center.y, center.z - 1.0, center.x + 1.0, center.y + 4.0, center.z + 1.0);

      for (Entity entity : mc.world.getOtherEntities(null, searchBox)) {
         String rawText = null;
         if (entity instanceof TextDisplayEntity textDisplay) {
            rawText = textDisplay.getText().getString();
         } else if (entity instanceof ArmorStandEntity armorStand) {
            rawText = armorStand.getCustomName() != null ? armorStand.getCustomName().getString() : null;
         }

         Matcher matcher;
         if (rawText != null && (matcher = TIME_PATTERN.matcher(Formatting.strip(rawText) == null ? rawText : Formatting.strip(rawText))).find()) {
            int totalSeconds = Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2));
            WardenHelper.ChestData data = this.chests.get(pos);
            if (data == null) {
               this.chests.put(pos, new WardenHelper.ChestData(totalSeconds, entity));
            } else {
               data.update(totalSeconds, entity);
            }

            return;
         }
      }

      this.chests.putIfAbsent(pos, new WardenHelper.ChestData(-1, null));
   }

   private void maybeNotifySoon(BlockPos pos, WardenHelper.ChestData data) {
      if (data.hasValidTimer() && !data.notifiedSoon) {
         int remaining = data.getRemainingSeconds();
         if (remaining <= 30 && remaining > 0) {
            MutableText text = Text.literal("Сундук скоро откроется")
               .formatted(Formatting.GREEN)
               .append(Text.literal(" [" + this.formatTime(remaining) + "]").formatted(Formatting.GRAY))
               .append(Text.literal(" " + pos.getX() + " " + pos.getY() + " " + pos.getZ()).formatted(Formatting.DARK_GRAY));
            Notifications.getInstance().addList(text, 3500, SoundManager.NOTIF, false, CHEST_ICON.copy());
            data.notifiedSoon = true;
         }
      }
   }

   private boolean shouldSendChestNotification() {
      Hud hud = Hud.getInstance();
      return hud != null && hud.notificationSettings.isSelected("Скором открытии сундука");
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (mc.world != null && mc.player != null) {
         for (Entry<BlockPos, WardenHelper.ChestData> entry : this.chests.entrySet()) {
            WardenHelper.ChestData data = entry.getValue();
            int remaining;
            if (data.hasValidTimer() && (remaining = data.getRemainingSeconds()) > 0) {
               Render3D.drawBox(new Box(entry.getKey()), this.getChestColor(remaining), 2.0F);
            }
         }
      }
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      if (mc.world != null && mc.player != null) {
         DrawContext context = e.getDrawContext();
         MatrixStack matrix = context.getMatrices();
         FontRenderer font = Fonts.getSize((int)this.tagSizeSetting.getValue());

         for (Entry<BlockPos, WardenHelper.ChestData> entry : this.chests.entrySet()) {
            WardenHelper.ChestData data = entry.getValue();
            int remaining;
            if (data.hasValidTimer() && (remaining = data.getRemainingSeconds()) > 0) {
               Vec3d drawPos = data.entity != null
                  ? data.entity.getPos().add(0.0, data.entity.getHeight() + 0.25, 0.0)
                  : entry.getKey().toCenterPos().add(0.0, 1.4, 0.0);
               if (Projection.canSee(drawPos)) {
                  Vec3d screen = Projection.worldSpaceToScreenSpace(drawPos);
                  if (!(screen.z <= 0.0) && !(screen.z >= 1.0)) {
                     MutableText text = Text.literal(this.formatTime(remaining)).formatted(this.getTextFormatting(remaining));
                     this.drawTag(matrix, text, screen.x, screen.y, font);
                  }
               }
            }
         }
      }
   }

   private void drawTag(MatrixStack matrix, Text text, double x, double y, FontRenderer font) {
      float width = font.getStringWidth(text);
      float height = font.getFont().getSize() / 1.5F;
      float drawX = (float)x - width / 2.0F;
      float drawY = (float)y - height;
      blur.render(ShapeProperties.create(matrix, drawX - 3.0F, drawY - 1.0F, width + 6.0F, height + 3.0F).round(4.0F).color(ColorAssist.HALF_BLACK).build());
      font.drawText(matrix, text, drawX, drawY + 3.0F);
   }

   private int getChestColor(int remainingSeconds) {
      if (remainingSeconds <= 60) {
         return -1875640525;
      } else {
         return remainingSeconds <= 120 ? -1862297293 : -1862323405;
      }
   }

   private Formatting getTextFormatting(int remainingSeconds) {
      if (remainingSeconds <= 60) {
         return Formatting.GREEN;
      } else {
         return remainingSeconds <= 120 ? Formatting.GOLD : Formatting.RED;
      }
   }

   private String formatTime(int seconds) {
      return String.format("%02d:%02d", seconds / 60, seconds % 60);
   }

   public Optional<WardenHelper.ChestSnapshot> getClosestChestUnder(int maxSeconds) {
      if (mc.player == null) {
         return Optional.empty();
      } else {
         Vec3d playerPos = mc.player.getPos();
         return this.chests.entrySet().stream().map(entry -> {
            WardenHelper.ChestData data = entry.getValue();
            if (!data.hasValidTimer()) {
               return null;
            } else {
               int remaining = data.getRemainingSeconds();
               return remaining > 0 && remaining <= maxSeconds ? new WardenHelper.ChestSnapshot(entry.getKey().toImmutable(), remaining, data.entity) : null;
            }
         }).filter(snapshot -> snapshot != null).min(Comparator.comparingDouble(snapshot -> playerPos.squaredDistanceTo(snapshot.pos().toCenterPos())));
      }
   }

   public Optional<WardenHelper.ChestSnapshot> getChestWithMinTimer(int maxSeconds) {
      return mc.player == null ? Optional.empty() : this.chests.entrySet().stream().map(entry -> {
         WardenHelper.ChestData data = entry.getValue();
         if (!data.hasValidTimer()) {
            return null;
         } else {
            int remaining = data.getRemainingSeconds();
            return remaining > 0 && remaining <= maxSeconds ? new WardenHelper.ChestSnapshot(entry.getKey().toImmutable(), remaining, data.entity) : null;
         }
      }).filter(snapshot -> snapshot != null).min(Comparator.comparingInt(WardenHelper.ChestSnapshot::remainingSeconds));
   }

   public Optional<WardenHelper.ChestSnapshot> getChestSnapshot(BlockPos pos) {
      WardenHelper.ChestData data = this.chests.get(pos);
      if (data != null && data.hasValidTimer()) {
         int remaining = data.getRemainingSeconds();
         return remaining <= 0 ? Optional.empty() : Optional.of(new WardenHelper.ChestSnapshot(pos.toImmutable(), remaining, data.entity));
      } else {
         return Optional.empty();
      }
   }

   private static class ChestData {
      private int timerSeconds;
      private long startTime;
      private boolean hasTimer;
      private Entity entity;
      private boolean notifiedSoon;

      private ChestData(int seconds, Entity entity) {
         this.update(seconds, entity);
      }

      private void update(int seconds, Entity entity) {
         if (seconds > 0) {
            if (!this.hasTimer || Math.abs(this.timerSeconds - seconds) > 1) {
               this.timerSeconds = seconds;
               this.startTime = System.currentTimeMillis();
               this.notifiedSoon = false;
            }

            this.hasTimer = true;
         } else if (!this.hasTimer) {
            this.timerSeconds = 0;
            this.startTime = System.currentTimeMillis();
            this.hasTimer = false;
         }

         this.entity = entity;
      }

      private int getRemainingSeconds() {
         return !this.hasTimer ? 0 : Math.max(0, this.timerSeconds - (int)((System.currentTimeMillis() - this.startTime) / 1000L));
      }

      private boolean hasValidTimer() {
         return this.hasTimer;
      }
   }

   public record ChestSnapshot(BlockPos pos, int remainingSeconds, Entity entity) {
   }
}
