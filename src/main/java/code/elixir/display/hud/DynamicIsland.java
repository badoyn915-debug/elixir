package code.elixir.display.hud;

import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.theme.ThemeManager;
import java.awt.Color;
import java.lang.invoke.CallSite;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public class DynamicIsland extends AbstractDraggable {
   private static final long SHOW_MS = 2200L;
   private static final float PILL_H = 15.0F;
   private static final float ORB_R = 3.2F;
   private static final float ICON_TEXT_GAP = 5.0F;
   private static final float TIMER_PREFIX_GAP = 3.0F;
   private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
   private static final Pattern TIME_MM_SS = Pattern.compile("\\b\\d{1,2}:\\d{2}\\b");
   private static final Pattern TIME_WITH_SUFFIX = Pattern.compile("\\b\\d+\\s*(?:s|sec|secs|сек|с)\\b", 66);
   private static final Pattern TIME_DIGITS = Pattern.compile("\\b\\d{1,3}\\b");
   private static volatile DynamicIsland.Notif active = null;
   private static volatile long activeEnd = 0L;
   private static volatile long activeRevision = 0L;
   private static final Queue<DynamicIsland.Notif> queue = new ArrayDeque<>();
   private final FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
   private float pillW = 80.0F;
   private float notifAnim = 0.0F;
   private long lastAnimTime = System.currentTimeMillis();
   private DynamicIsland.Notif lastNotif = null;
   private long lastSeenRevision = -1L;
   private final Set<String> warnedExpiringEffects = new HashSet<>();
   private long nextArmorCheckAt = 0L;
   private float pingHoverAnim = 0.0F;
   private String cachedTimeString = "";
   private long cachedTimeAt = 0L;
   private String cachedPvpBossbarText = null;
   private String cachedTeleportBossbarText = null;
   private String cachedEventStatus = null;
   private static Method cachedEventStatusMethod;

   public DynamicIsland() {
      super("Watermark", 10, 10, 80, 18, false);
   }

   public static void addNotification(String text, DynamicIsland.NotificationType type) {
      if (text != null && !text.isEmpty()) {
         String clean = Formatting.strip(text) == null ? text : Formatting.strip(text);
         boolean isDynamic = text.contains("Ивент через") || text.contains("Ивент уже идет");
         DynamicIsland.Notif notif = new DynamicIsland.Notif(clean, type.color, isDynamic);
         if (active != null && System.currentTimeMillis() < activeEnd) {
            queue.offer(notif);
         } else {
            active = notif;
            activeEnd = System.currentTimeMillis() + 2200L;
            activeRevision++;
         }
      }
   }

   public static void notifyModuleToggle(String name, boolean enabled) {
      String cleanName = Formatting.strip(name) == null ? name : Formatting.strip(name);
      active = new DynamicIsland.Notif(
         cleanName + (enabled ? " enabled" : " disabled"),
         (enabled ? DynamicIsland.NotificationType.SUCCESS : DynamicIsland.NotificationType.ERROR).color,
         false
      );
      activeEnd = System.currentTimeMillis() + 2200L;
      activeRevision++;
   }

   public static void strengthNotification() {
      if (mc.player != null) {
         EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
         String[] names = new String[]{"Шлем", "Нагрудник", "Поножи", "Ботинки"};

         for (int i = 0; i < slots.length; i++) {
            ItemStack stack = mc.player.getEquippedStack(slots[i]);
            float pct;
            if (!stack.isEmpty()
               && stack.isDamageable()
               && (pct = (float)(stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage()) <= 0.1F
               && pct > 0.0F) {
               addNotification(names[i] + " скоро сломается!", DynamicIsland.NotificationType.WARNING);
            }
         }
      }
   }

   public static String getEventStatus() {
      if (cachedEventStatusMethod == null) {
         return null;
      } else {
         try {
            return (String)cachedEventStatusMethod.invoke(null);
         } catch (Exception var1) {
            return null;
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (active != null && System.currentTimeMillis() >= activeEnd) {
         DynamicIsland.Notif next;
         active = next = queue.poll();
         activeEnd = next != null ? System.currentTimeMillis() + 2200L : 0L;
         activeRevision++;
      }

      if (this.lastSeenRevision != activeRevision) {
         this.notifAnim = 0.0F;
         this.lastNotif = active;
         this.lastSeenRevision = activeRevision;
      }

      this.tickArmorDurabilityWarnings();
      this.tickExpiringPotionWarnings();
      this.updateRenderCache();
   }

   private void updateRenderCache() {
      this.cachedPvpBossbarText = null;
      this.cachedTeleportBossbarText = null;
      if (mc != null && mc.inGameHud != null && mc.inGameHud.getBossBarHud() != null) {
         for (ClientBossBar bar : mc.inGameHud.getBossBarHud().bossBars.values()) {
            String raw;
            if (bar != null && bar.getName() != null && (raw = Formatting.strip(bar.getName().getString())) != null && !raw.isEmpty()) {
               String low = raw.toLowerCase(Locale.ROOT);
               if (this.cachedPvpBossbarText == null && (low.contains("pvp") || low.contains("пвп"))) {
                  this.cachedPvpBossbarText = raw;
               }

               if (this.cachedTeleportBossbarText == null
                  && (low.contains("телепортац") || low.contains("телепорт") || low.contains("перемещ") || low.contains("teleport") || low.contains("tp "))) {
                  this.cachedTeleportBossbarText = raw;
               }

               if (this.cachedPvpBossbarText != null && this.cachedTeleportBossbarText != null) {
                  break;
               }
            }
         }
      }

      long now = System.currentTimeMillis();
      if (this.cachedTimeAt == 0L || now - this.cachedTimeAt >= 1000L) {
         this.cachedTimeString = LocalTime.now().format(TIME_FORMATTER);
         this.cachedTimeAt = now;
      }

      DynamicIsland.Notif currentActive = active;
      this.cachedEventStatus = active != null && currentActive.isDynamic() && now < activeEnd ? getEventStatus() : null;
   }

   private void tickArmorDurabilityWarnings() {
      Hud hud = Hud.getInstance();
      if (hud != null && hud.notificationSettings != null) {
         if (hud.notificationSettings.isSelected("Скорой поломке брони")) {
            long now = System.currentTimeMillis();
            if (now >= this.nextArmorCheckAt) {
               this.nextArmorCheckAt = now + 1200L;
               strengthNotification();
            }
         }
      }
   }

   private void tickExpiringPotionWarnings() {
      Hud hud = Hud.getInstance();
      if (hud != null && hud.notificationSettings != null && mc.player != null) {
         if (hud.notificationSettings.isSelected("Заканчивающемся зелье")) {
            HashSet<String> activeKeys = new HashSet<>();

            for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
               String effectName = ((StatusEffect)effect.getEffectType().value()).getName().getString();
               int amp = effect.getAmplifier() + 1;
               String key = effectName + ":" + amp;
               activeKeys.add(key);
               int seconds = effect.getDuration() / 20;
               if (seconds > 0 && seconds <= 30 && !this.warnedExpiringEffects.contains(key)) {
                  addNotification("Зелье " + effectName + " " + amp + " заканчивается (" + seconds + "s)", DynamicIsland.NotificationType.WARNING);
                  this.warnedExpiringEffects.add(key);
               }
            }

            Iterator<String> it = this.warnedExpiringEffects.iterator();

            while (it.hasNext()) {
               String key = it.next();
               if (!activeKeys.contains(key)) {
                  it.remove();
               }
            }
         }
      }
   }

   @Override
   public boolean canDraw(Hud hud, AbstractDraggable draggable) {
      if (hud == null) {
         return false;
      } else {
         return !hud.isState() ? false : Hud.interfaceSettings.isSelected("Watermark");
      }
   }

   private DynamicIsland.IslandCapsule resolveCapsule(boolean showNotif, DynamicIsland.Notif renderNotif) {
      String pvpRaw = this.cachedPvpBossbarText;
      if (pvpRaw != null) {
         String timer = this.extractPvpTimer(pvpRaw);
         int accent = new Color(12131356).getRGB();
         return new DynamicIsland.IslandCapsule(DynamicIsland.IslandState.PVP, "Вы в PVP режиме", "Вы в PvP", timer, accent);
      } else {
         String tpRaw = this.cachedTeleportBossbarText;
         if (tpRaw != null) {
            String timer = this.extractPvpTimer(tpRaw);
            int accent = new Color(6530303).getRGB();
            return new DynamicIsland.IslandCapsule(DynamicIsland.IslandState.TELEPORT, "Телепортация...", "Телепортация через ", timer, accent);
         } else if (showNotif && renderNotif != null) {
            String text = renderNotif.text();
            if (renderNotif.isDynamic()) {
               String updatedStatus = this.cachedEventStatus;
               if (this.cachedEventStatus != null && !updatedStatus.isEmpty()) {
                  text = updatedStatus;
               }
            }

            return new DynamicIsland.IslandCapsule(DynamicIsland.IslandState.NOTIFICATION, text, "", null, renderNotif.color().getRGB());
         } else {
            return new DynamicIsland.IslandCapsule(DynamicIsland.IslandState.DEFAULT, this.getDisplayName(), "", null, ThemeManager.primaryColor.getColor());
         }
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      MatrixStack matrices = context.getMatrices();
      int scaledWidth = window.getScaledWidth();
      int themeText = ThemeManager.textColor.getColor();
      long now = System.currentTimeMillis();
      boolean hasNotif = active != null && now < activeEnd;
      float targetAnim = hasNotif ? 1.0F : 0.0F;
      float dt = (float)(now - this.lastAnimTime) / 1000.0F;
      this.lastAnimTime = now;
      this.notifAnim = this.notifAnim + (targetAnim - this.notifAnim) * Math.min(1.0F, dt * 12.0F);
      if (this.notifAnim < 0.005F) {
         this.notifAnim = 0.0F;
      }

      if (this.notifAnim > 0.995F) {
         this.notifAnim = 1.0F;
      }

      if (hasNotif) {
         this.lastNotif = active;
      }

      if (!hasNotif && this.notifAnim == 0.0F) {
         this.lastNotif = null;
      }

      DynamicIsland.Notif renderNotif = hasNotif ? active : (this.notifAnim > 0.0F ? this.lastNotif : null);
      boolean showNotif = renderNotif != null;
      DynamicIsland.IslandCapsule capsule = this.resolveCapsule(showNotif, renderNotif);
      DynamicIsland.IslandState state = capsule.state();
      boolean hasTeleport = state == DynamicIsland.IslandState.TELEPORT;
      boolean hasPvp = state == DynamicIsland.IslandState.PVP;
      String time = this.cachedTimeString;
      float timeW = this.font.getStringWidth(time);

      this.pillW = this.pillW + (switch (state) {
         case NOTIFICATION -> {
            float tw = this.font.getStringWidth(capsule.text());
            yield 17.4F + tw + 8.0F;
         }
         case PVP, TELEPORT -> {
            String full = capsule.timer() == null ? capsule.text() : capsule.prefix() + " " + capsule.timer();
            float tw = this.font.getStringWidth(full) + (capsule.timer() != null ? 3.0F : 0.0F);
            yield 17.4F + tw + 8.0F;
         }
         default -> {
            float nameW = this.font.getStringWidth(capsule.text());
            yield 17.4F + nameW + 8.0F;
         }
      } - this.pillW) * 0.16F;
      float pingGroupW = 16.2F;
      float totalW = timeW + 6.0F + this.pillW + 6.0F + pingGroupW;
      float startX = scaledWidth / 2.0F - totalW / 2.0F;
      startX = MathHelper.clamp(startX, 2.0F, Math.max(2.0F, scaledWidth - totalW - 2.0F));
      float pillX = startX + timeW + 6.0F;
      float pillY = 7.0F;
      float midY = pillY + 7.5F;
      float textY = this.centeredTextY(this.font, pillY, 15.0F);
      this.font.drawString(matrices, time, startX, textY, themeText);
      Hud.getInstance();
      if (Hud.blur.isValue() && !KawaseBlur.INSTANCE.fbos.isEmpty()) {
         Render2D.rectangleWithMask(
            matrices.peek().getPositionMatrix(), pillX, pillY, this.pillW, 15.0F, 7.5F, 0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment()
         );
      }

      rectangle.render(
         ShapeProperties.create(matrices, pillX, pillY, this.pillW, 15.0).round(7.5F).softness(2.5F).color(new Color(0, 0, 0, 210).getRGB()).build()
      );
      float cx = pillX + 6.0F;
      if (hasTeleport) {
         int dotColor = ColorAssist.multAlpha(capsule.accentColor(), 0.95F);
         rectangle.render(ShapeProperties.create(matrices, cx, midY - 3.2F, 6.4F, 6.4F).round(3.2F).color(dotColor).build());
         cx += 11.4F;
         if (capsule.timer() == null) {
            this.font.drawString(matrices, capsule.text(), cx, textY, themeText);
         } else {
            this.font.drawString(matrices, capsule.prefix(), cx, textY, themeText);
            TimerTextAnimator.draw(
               this.font, matrices, "dynamic_island_tp_timer", capsule.timer(), cx + this.font.getStringWidth(capsule.prefix()) + 3.0F, textY, themeText
            );
         }
      } else if (hasPvp) {
         int dotColor = ColorAssist.multAlpha(capsule.accentColor(), 0.95F);
         rectangle.render(ShapeProperties.create(matrices, cx, midY - 3.2F, 6.4F, 6.4F).round(3.2F).color(dotColor).build());
         cx += 11.4F;
         if (capsule.timer() == null) {
            this.font.drawString(matrices, capsule.text(), cx, textY, themeText);
         } else {
            this.font.drawString(matrices, capsule.prefix(), cx, textY, themeText);
            TimerTextAnimator.draw(
               this.font, matrices, "dynamic_island_pvp_timer", capsule.timer(), cx + this.font.getStringWidth(capsule.prefix()) + 3.0F, textY, themeText
            );
         }
      } else if (showNotif) {
         float slide = (1.0F - this.notifAnim) * 7.0F;
         int dotColor = ColorAssist.multAlpha(capsule.accentColor(), Math.max(0.1F, this.notifAnim));
         int textColor = ColorAssist.multAlpha(themeText, Math.max(0.2F, this.notifAnim));
         rectangle.render(ShapeProperties.create(matrices, cx + slide, midY - 3.2F, 6.4F, 6.4F).round(3.2F).color(dotColor).build());
         float var43;
         this.font.drawString(matrices, capsule.text(), (var43 = cx + 11.4F) + slide, textY, textColor);
      } else {
         this.renderClientOrb(matrices, cx, midY);
         int titleColor = ThemeManager.textColor.getColor();
         float var44;
         this.font.drawString(matrices, capsule.text(), Math.round(var44 = cx + 11.4F), Math.round(textY), titleColor);
      }

      float barsX = pillX + this.pillW + 6.0F;
      float barsY = midY - 5.0F;
      float mouseX = (float)(mc.mouse.getX() / window.getScaleFactor());
      float mouseY = (float)(mc.mouse.getY() / window.getScaleFactor());
      boolean pingHovered = mouseX >= barsX - 3.0F && mouseX <= barsX + 17.0F && mouseY >= barsY - 3.0F && mouseY <= barsY + 14.0F;
      this.pingHoverAnim = this.pingHoverAnim + ((pingHovered ? 1.0F : 0.0F) - this.pingHoverAnim) * 0.2F;
      this.renderPing(matrices, barsX, barsY, themeText, this.pingHoverAnim);
      if (this.pingHoverAnim > 0.03F) {
         int pingTextColor = ColorAssist.multAlpha(themeText, this.pingHoverAnim);
         String pingText = this.getPlayerPing() + " ms";
         float pingTextX = barsX + 16.0F;
         this.font.drawString(matrices, pingText, pingTextX, textY, pingTextColor);
      }

      this.setX((int)startX);
      this.setY((int)pillY);
      this.setWidth((int)Math.ceil(totalW));
      this.setHeight(15);
      this.clampToScreen();
   }

   private void renderPing(MatrixStack matrices, float x, float y, int textBase, float hoverAnim) {
      int ping = this.getPlayerPing();
      int lit = ping <= 50 ? 4 : (ping <= 80 ? 3 : (ping <= 120 ? 2 : (ping <= 180 ? 1 : 0)));
      int[] maxH = new int[]{4, 6, 8, 10};
      long now = System.currentTimeMillis();
      float slideX = 4.0F * hoverAnim;

      for (int i = 0; i < 4; i++) {
         float breath = (float)(Math.sin(now / 600.0 + i * 0.7) * 0.5 + 0.5);
         float bh = i < lit ? maxH[i] * (0.6F + 0.4F * breath) : maxH[i] * (0.25F + 0.1F * breath);
         int col = i < lit
            ? ColorAssist.multAlpha(textBase, 0.85F * (1.0F - hoverAnim * 0.2F))
            : ColorAssist.multAlpha(textBase, 0.18F * (1.0F - hoverAnim * 0.25F));
         rectangle.render(ShapeProperties.create(matrices, x + i * 3.3F + slideX, y + (10.0F - bh), 2.8F, bh).round(1.0F).color(col).build());
      }
   }

   private float centeredTextY(FontRenderer font, float boxY, float boxHeight) {
      return font.getCenteredY(boxY, boxHeight);
   }

   private void renderClientOrb(MatrixStack matrices, float x, float midY) {
      long now = System.currentTimeMillis();
      float t = (float)((Math.sin(now / 320.0) + 1.0) * 0.5);
      int c1 = ThemeManager.primaryColor.getColor();
      int c2 = ThemeManager.secondaryColor.getColor();
      int orb = blend(c1, c2, t);
      rectangle.render(ShapeProperties.create(matrices, x, midY - 3.2F, 6.4F, 6.4F).round(3.2F).color(orb).build());
   }

   private String extractPvpTimer(String text) {
      Matcher mmss = TIME_MM_SS.matcher(text);
      if (mmss.find()) {
         return mmss.group();
      } else {
         Matcher withSuffix = TIME_WITH_SUFFIX.matcher(text);
         if (withSuffix.find()) {
            return withSuffix.group().replaceAll("\\s+", "");
         } else {
            Matcher digits = TIME_DIGITS.matcher(text);
            return digits.find() ? digits.group() + "s" : null;
         }
      }
   }

   private String getDisplayName() {
      return "Elixir";
   }

   private int getPlayerPing() {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
         return entry != null ? entry.getLatency() : 0;
      } else {
         return 0;
      }
   }

   private static int blend(int from, int to, float t) {
      float p = MathHelper.clamp(t, 0.0F, 1.0F);
      int fr = from >> 16 & 0xFF;
      int fg = from >> 8 & 0xFF;
      int fb = from & 0xFF;
      int fa = from >>> 24 & 0xFF;
      int tr = to >> 16 & 0xFF;
      int tg = to >> 8 & 0xFF;
      int tb = to & 0xFF;
      int ta = to >>> 24 & 0xFF;
      int r = (int)(fr + (tr - fr) * p);
      int g = (int)(fg + (tg - fg) * p);
      int b = (int)(fb + (tb - fb) * p);
      int a = (int)(fa + (ta - fa) * p);
      return a << 24 | r << 16 | g << 8 | b;
   }

   static {
      try {
         Class<?> trackerClass = Class.forName("code.elixir.features.impl.movement.autoMystGlider.EventStatusTracker");
         cachedEventStatusMethod = trackerClass.getMethod("getDisplayStatus");
      } catch (Exception var1) {
         cachedEventStatusMethod = null;
      }
   }

   private record IslandCapsule(DynamicIsland.IslandState state, String text, String prefix, String timer, int accentColor) {
   }

   private static enum IslandState {
      DEFAULT,
      NOTIFICATION,
      PVP,
      TELEPORT;
   }

   private record Notif(String text, Color color, boolean isDynamic) {
   }

   public static enum NotificationType {
      SUCCESS(new Color(4908362)),
      ERROR(new Color(16729413)),
      WARNING(new Color(16758832)),
      INFO(new Color(4894207)),
      DEFAULT(new Color(11032055));

      public final Color color;

      private NotificationType(Color color) {
         this.color = color;
      }
   }
}
