package code.elixir.features.impl.render;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.display.screens.clickgui.MenuScreen;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.DrawEvent;
import code.elixir.events.render.WorldLoadEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.math.projection.Projection;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;

public class Esp extends Module {
   private static final Identifier BLOOM_TEXTURE = Identifier.of("textures/bloom.png");
   private static final float BOX_EPSILON = 0.001F;
   private static final int[] BLOOM_PASS_ALPHA = new int[]{55, 110};
   private static final float[] BLOOM_PASS_EXPAND = new float[]{1.0F, 0.45F};
   private static final int[] GLOW_LAYER_ALPHA = new int[]{45, 85, 140, 235};
   private static final float[] GLOW_LAYER_WIDTH_MUL = new float[]{3.2F, 2.0F, 1.1F, 0.5F};
   private static final Vector3f LINE_START = new Vector3f();
   private static final Vector3f LINE_END = new Vector3f();
   private final List<PlayerEntity> players = new ArrayList<>();
   private List<Box> glowBatchBoxes = new ArrayList<>(64);
   private List<Integer> glowBatchColors = new ArrayList<>(64);
   private Vector4d vecScratch = new Vector4d();
   public static MultiSelectSetting entityType = new MultiSelectSetting("Показывать", "Сущности, которые будут отображаться")
      .value("Игроков", "Мобов", "Животных", "Предметы", "Подставка для брони", "Друзья")
      .selected("Игроков");
   public static RadioSetting boxType = new RadioSetting("Режим бокса", "Тип бокса", new String[]{"Обычный", "Корнер", "Свечение"}, "Свечение");
   public final SliderSettings glowThickness = new SliderSettings("Толщина свечения", "Толщина светящегося контура")
      .setValue(3.0F)
      .range(1.0F, 6.0F)
      .step(0.5F)
      .visible(() -> boxType.get().equals("Свечение"));
   public final BooleanSetting potions = new BooleanSetting("Зелья", "Компактные панели эффектов справа от хитбокса").setValue(false);
   public final SliderSettings potionScale = new SliderSettings("Размер зелий", "Масштаб панелей эффектов")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .step(0.05F)
      .visible(() -> this.potions.isValue());
   static ColorSetting color = new ColorSetting("Цвет боксов", "Цвет кристаллов")
      .value(new Color(255, 255, 255, 55).getRGB())
      .presets(
         new Color(0, 246, 255, 255).getRGB(), new Color(183, 1, 195, 255).getRGB(), new Color(255, 60, 0, 255).getRGB(), new Color(171, 253, 0, 255).getRGB()
      );
   private static final int MAX_POTION_PANELS = 6;
   private static final float POTION_BASE_PANEL_HEIGHT = 11.0F;
   private static final float POTION_BASE_ICON_SIZE = 8.0F;
   private static final float POTION_BASE_PANEL_GAP = 1.5F;
   private static final float POTION_BASE_PADDING_X = 3.0F;
   private static final float POTION_BASE_ICON_TEXT_GAP = 2.5F;
   private static final float POTION_BASE_VERTICAL_PAD = 2.0F;
   private static final float POTION_BASE_FONT_SIZE = 10.0F;
   private static final int POTION_TEXT_COLOR = -1;
   private static final int POTION_MIN_FONT_SIZE = 8;
   private static final float POTION_REFERENCE_BOX_HEIGHT = 68.0F;
   private static final float POTION_MIN_DISTANCE_SCALE = 0.32F;
   private static final float POTION_MAX_DISTANCE_SCALE = 1.0F;

   public static Esp getInstance() {
      return Instance.get(Esp.class);
   }

   public Esp() {
      super("Esp", "Esp", ModuleCategory.ESP);
      this.setup(new Setting[]{entityType, boxType, this.glowThickness, this.potions, this.potionScale, color});
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
               if (boxType.get().equals("Свечение")) {
                  boolean selPlayers = entityType.isSelected("Игроков");
                  boolean selFriends = entityType.isSelected("Друзья");
                  boolean selMobs = entityType.isSelected("Мобов");
                  boolean selAnimals = entityType.isSelected("Животных");
                  boolean selItems = entityType.isSelected("Предметы");
                  boolean selArmor = entityType.isSelected("Подставка для брони");
                  if (selPlayers || selMobs || selAnimals || selItems || selArmor || selFriends) {
                     this.glowBatchBoxes.clear();
                     this.glowBatchColors.clear();
                     float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
                     double renderDistance = Math.sqrt(Projection.getMaxRenderDistanceSq());
                     Box searchBox = mc.player.getBoundingBox().expand(renderDistance);
                     if (selPlayers || selFriends) {
                        this.collectPlayerGlowBoxes(tickDelta, selPlayers, selFriends);
                     }

                     if (selMobs) {
                        this.collectLivingGlowBoxes(tickDelta, searchBox, MobEntity.class, entity -> !(entity instanceof AnimalEntity));
                     }

                     if (selAnimals) {
                        this.collectLivingGlowBoxes(tickDelta, searchBox, AnimalEntity.class, entity -> true);
                     }

                     if (selItems) {
                        this.collectEntityGlowBoxes(searchBox, ItemEntity.class);
                     }

                     if (selArmor) {
                        this.collectLivingGlowBoxes(tickDelta, searchBox, ArmorStandEntity.class, entity -> true);
                     }

                     if (!this.glowBatchBoxes.isEmpty()) {
                        this.renderGlowBatch(e.getStack(), this.glowThickness.getValue());
                     }
                  }
               }
            }
         }
      }
   }

   private void collectPlayerGlowBoxes(float tickDelta, boolean selPlayers, boolean selFriends) {
      int enemyRgb = color.getColorWithAlpha() & 16777215;
      int friendRgb = ColorAssist.getFriendColor() & 16777215;

      for (PlayerEntity player : this.players) {
         if (player != null && (player.getCustomName() == null || !player.getCustomName().getString().startsWith("Ghost_"))) {
            boolean friend = FriendUtils.isFriend(player);
            Box box;
            if ((selPlayers || selFriends && friend) && (box = this.getInterpolatedBox(player, tickDelta)) != null) {
               this.glowBatchBoxes.add(box);
               this.glowBatchColors.add(friend ? friendRgb : enemyRgb);
            }
         }
      }
   }

   private <T extends LivingEntity> void collectLivingGlowBoxes(float tickDelta, Box searchBox, Class<T> type, Predicate<T> filter) {
      int enemyRgb = color.getColorWithAlpha() & 16777215;

      for (LivingEntity entity : mc.world.getEntitiesByClass(type, searchBox, filter::test)) {
         Box box = this.getInterpolatedBox(entity, tickDelta);
         if (box != null) {
            this.glowBatchBoxes.add(box);
            this.glowBatchColors.add(enemyRgb);
         }
      }
   }

   private <T extends Entity> void collectEntityGlowBoxes(Box searchBox, Class<T> type) {
      int enemyRgb = color.getColorWithAlpha() & 16777215;

      for (Entity entity2 : mc.world.getEntitiesByClass(type, searchBox, entity -> true)) {
         Box box;
         if (Projection.isWithinRenderDistance(entity2) && Projection.canSee(box = entity2.getBoundingBox())) {
            this.glowBatchBoxes.add(box);
            this.glowBatchColors.add(enemyRgb);
         }
      }
   }

   private Box getInterpolatedBox(LivingEntity entity, float tickDelta) {
      if (!Projection.isWithinRenderDistance(entity)) {
         return null;
      } else {
         double interpX = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
         double interpY = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
         double interpZ = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
         Box box = entity.getDimensions(entity.getPose()).getBoxAt(interpX, interpY, interpZ);
         return Projection.canSee(box) ? box : null;
      }
   }

   private void renderGlowBatch(MatrixStack matrix, float thickness) {
      Matrix4f positionMatrix = matrix.peek().getPositionMatrix();
      Entry lineEntry = matrix.peek();
      this.batchDrawWorldBloom(positionMatrix, thickness);
      this.batchDrawBoxFillGradient(positionMatrix);

      for (int layer = 0; layer < GLOW_LAYER_ALPHA.length; layer++) {
         float width = layer == GLOW_LAYER_ALPHA.length - 1 ? Math.max(1.0F, thickness * GLOW_LAYER_WIDTH_MUL[layer]) : thickness * GLOW_LAYER_WIDTH_MUL[layer];
         this.batchDrawGlowLines(lineEntry, GLOW_LAYER_ALPHA[layer], width);
      }
   }

   private void batchDrawWorldBloom(Matrix4f matrix, float thickness) {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

      for (int pass = 0; pass < BLOOM_PASS_ALPHA.length; pass++) {
         int passAlpha = BLOOM_PASS_ALPHA[pass];
         float passExpand = BLOOM_PASS_EXPAND[pass];
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

         for (int i = 0; i < this.glowBatchBoxes.size(); i++) {
            this.appendBloomQuads(buffer, matrix, this.glowBatchBoxes.get(i), this.glowBatchColors.get(i), passAlpha, thickness, passExpand);
         }

         BufferDraw.draw(buffer);
      }

      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   private void appendBloomQuads(BufferBuilder buffer, Matrix4f matrix, Box box, int rgb, int passAlpha, float thickness, float passExpand) {
      Box b = box.expand(0.001F);
      float x1 = (float)b.minX;
      float y1 = (float)b.minY;
      float z1 = (float)b.minZ;
      float x2 = (float)b.maxX;
      float y2 = (float)b.maxY;
      float z2 = (float)b.maxZ;
      float mxz = (float)((b.maxX - b.minX) * 0.6) + thickness * 0.05F;
      float my = (float)((b.maxY - b.minY) * 0.12) + thickness * 0.05F;
      int c = rgb | passAlpha << 24;
      float ex = mxz * passExpand;
      float ey = my * passExpand;
      float ax1 = x1 - ex;
      float ax2 = x2 + ex;
      float ay1 = y1 - ey;
      float ay2 = y2 + ey;
      float az1 = z1 - ex;
      float az2 = z2 + ex;
      buffer.vertex(matrix, ax1, ay1, z1).texture(0.0F, 1.0F).color(c);
      buffer.vertex(matrix, ax2, ay1, z1).texture(1.0F, 1.0F).color(c);
      buffer.vertex(matrix, ax2, ay2, z1).texture(1.0F, 0.0F).color(c);
      buffer.vertex(matrix, ax1, ay2, z1).texture(0.0F, 0.0F).color(c);
      buffer.vertex(matrix, ax1, ay1, z2).texture(0.0F, 1.0F).color(c);
      buffer.vertex(matrix, ax2, ay1, z2).texture(1.0F, 1.0F).color(c);
      buffer.vertex(matrix, ax2, ay2, z2).texture(1.0F, 0.0F).color(c);
      buffer.vertex(matrix, ax1, ay2, z2).texture(0.0F, 0.0F).color(c);
      buffer.vertex(matrix, x1, ay1, az1).texture(0.0F, 1.0F).color(c);
      buffer.vertex(matrix, x1, ay1, az2).texture(1.0F, 1.0F).color(c);
      buffer.vertex(matrix, x1, ay2, az2).texture(1.0F, 0.0F).color(c);
      buffer.vertex(matrix, x1, ay2, az1).texture(0.0F, 0.0F).color(c);
      buffer.vertex(matrix, x2, ay1, az1).texture(0.0F, 1.0F).color(c);
      buffer.vertex(matrix, x2, ay1, az2).texture(1.0F, 1.0F).color(c);
      buffer.vertex(matrix, x2, ay2, az2).texture(1.0F, 0.0F).color(c);
      buffer.vertex(matrix, x2, ay2, az1).texture(0.0F, 0.0F).color(c);
   }

   private void batchDrawBoxFillGradient(Matrix4f matrix) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

      for (int i = 0; i < this.glowBatchBoxes.size(); i++) {
         int rgb = this.glowBatchColors.get(i);
         this.appendFillGradientQuads(buffer, matrix, this.glowBatchBoxes.get(i), rgb | 1174405120, rgb);
      }

      BufferDraw.draw(buffer);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   private void appendFillGradientQuads(BufferBuilder buffer, Matrix4f matrix, Box box, int bottom, int top) {
      Box b = box.expand(0.001F);
      float x1 = (float)b.minX;
      float y1 = (float)b.minY;
      float z1 = (float)b.minZ;
      float x2 = (float)b.maxX;
      float y2 = (float)b.maxY;
      float z2 = (float)b.maxZ;
      buffer.vertex(matrix, x1, y1, z1).color(bottom);
      buffer.vertex(matrix, x2, y1, z1).color(bottom);
      buffer.vertex(matrix, x2, y1, z2).color(bottom);
      buffer.vertex(matrix, x1, y1, z2).color(bottom);
      buffer.vertex(matrix, x1, y2, z1).color(top);
      buffer.vertex(matrix, x1, y2, z2).color(top);
      buffer.vertex(matrix, x2, y2, z2).color(top);
      buffer.vertex(matrix, x2, y2, z1).color(top);
      buffer.vertex(matrix, x1, y1, z1).color(bottom);
      buffer.vertex(matrix, x1, y2, z1).color(top);
      buffer.vertex(matrix, x2, y2, z1).color(top);
      buffer.vertex(matrix, x2, y1, z1).color(bottom);
      buffer.vertex(matrix, x1, y1, z2).color(bottom);
      buffer.vertex(matrix, x2, y1, z2).color(bottom);
      buffer.vertex(matrix, x2, y2, z2).color(top);
      buffer.vertex(matrix, x1, y2, z2).color(top);
      buffer.vertex(matrix, x1, y1, z1).color(bottom);
      buffer.vertex(matrix, x1, y1, z2).color(bottom);
      buffer.vertex(matrix, x1, y2, z2).color(top);
      buffer.vertex(matrix, x1, y2, z1).color(top);
      buffer.vertex(matrix, x2, y1, z1).color(bottom);
      buffer.vertex(matrix, x2, y2, z1).color(top);
      buffer.vertex(matrix, x2, y2, z2).color(top);
      buffer.vertex(matrix, x2, y1, z2).color(bottom);
   }

   private void batchDrawGlowLines(Entry entry, int layerAlpha, float width) {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
      RenderSystem.lineWidth(width);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.LINES);

      for (int i = 0; i < this.glowBatchBoxes.size(); i++) {
         int lineColor = this.glowBatchColors.get(i) | layerAlpha << 24;
         this.appendGlowBoxLines(entry, buffer, this.glowBatchBoxes.get(i), lineColor);
      }

      BufferDraw.draw(buffer);
      RenderSystem.lineWidth(1.0F);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   private void appendGlowBoxLines(Entry entry, BufferBuilder buffer, Box box, int color) {
      Box b = box.expand(0.001F);
      int topColor = color & 16777215;
      float minX = (float)b.minX;
      float minY = (float)b.minY;
      float minZ = (float)b.minZ;
      float maxX = (float)b.maxX;
      float maxY = (float)b.maxY;
      float maxZ = (float)b.maxZ;
      this.appendGlowLine(entry, buffer, minX, minY, minZ, maxX, minY, minZ, color, color);
      this.appendGlowLine(entry, buffer, maxX, minY, minZ, maxX, minY, maxZ, color, color);
      this.appendGlowLine(entry, buffer, maxX, minY, maxZ, minX, minY, maxZ, color, color);
      this.appendGlowLine(entry, buffer, minX, minY, maxZ, minX, minY, minZ, color, color);
      this.appendGlowLine(entry, buffer, minX, minY, minZ, minX, maxY, minZ, color, topColor);
      this.appendGlowLine(entry, buffer, maxX, minY, minZ, maxX, maxY, minZ, color, topColor);
      this.appendGlowLine(entry, buffer, maxX, minY, maxZ, maxX, maxY, maxZ, color, topColor);
      this.appendGlowLine(entry, buffer, minX, minY, maxZ, minX, maxY, maxZ, color, topColor);
   }

   private void appendGlowLine(Entry entry, BufferBuilder buffer, float x1, float y1, float z1, float x2, float y2, float z2, int startColor, int endColor) {
      LINE_START.set(x1, y1, z1);
      LINE_END.set(x2, y2, z2);
      Render3D.vertexLine(entry, buffer, LINE_START, LINE_END, startColor, endColor);
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      if (!(mc.currentScreen instanceof MenuScreen)) {
         if (mc.world != null && mc.player != null) {
            boolean selPlayers = entityType.isSelected("Игроков");
            boolean selFriends = entityType.isSelected("Друзья");
            boolean selMobs = entityType.isSelected("Мобов");
            boolean selAnimals = entityType.isSelected("Животных");
            boolean selItems = entityType.isSelected("Предметы");
            boolean selArmor = entityType.isSelected("Подставка для брони");
            if (selPlayers || selFriends || selMobs || selAnimals || selItems || selArmor || this.potions.isValue()) {
               MatrixStack matrix = e.getDrawContext().getMatrices();
               RenderSystem.disableDepthTest();
               matrix.push();
               matrix.translate(0.0F, 0.0F, -1000.0F);
               if (selPlayers || selFriends || this.potions.isValue()) {
                  for (PlayerEntity player : this.players) {
                     if (player != null && (player.getCustomName() == null || !player.getCustomName().getString().startsWith("Ghost_"))) {
                        boolean friend = FriendUtils.isFriend(player);
                        boolean drawBox = (selPlayers || selFriends && friend) && !boxType.get().equals("Свечение");
                        if ((drawBox || this.potions.isValue())
                           && Projection.isWithinRenderDistance(player)
                           && Projection.isInFrontOfCamera(
                              player.getLerpedPos(mc.getRenderTickCounter().getTickDelta(false)).add(0.0, player.getHeight() * 0.5, 0.0)
                           )
                           && Projection.canSee(player.getBoundingBox())) {
                           boolean onScreen = Projection.fillVector4D(player, this.vecScratch);
                           if (this.potions.isValue() && onScreen && isValidPotionScreenVec(this.vecScratch)) {
                              this.drawPotionPanels(matrix, player, this.vecScratch);
                           }

                           if (drawBox && onScreen) {
                              this.drawBox(friend, this.vecScratch, player);
                           }
                        }
                     }
                  }
               }

               if (boxType.get().equals("Свечение")) {
                  matrix.pop();
                  RenderSystem.enableDepthTest();
               } else if (!selPlayers && !selFriends && !selMobs && !selAnimals && !selItems && !selArmor) {
                  matrix.pop();
                  RenderSystem.enableDepthTest();
               } else {
                  double renderDistance = Math.sqrt(Projection.getMaxRenderDistanceSq());
                  Box searchBox = mc.player.getBoundingBox().expand(renderDistance);
                  if (selMobs) {
                     for (MobEntity mob : mc.world.getEntitiesByClass(MobEntity.class, searchBox, entity -> !(entity instanceof AnimalEntity))) {
                        if (Projection.isWithinRenderDistance(mob) && Projection.canSee(mob.getBoundingBox()) && Projection.fillVector4D(mob, this.vecScratch)) {
                           this.drawBox(false, this.vecScratch, mob);
                        }
                     }
                  }

                  if (selAnimals) {
                     for (AnimalEntity animal : mc.world.getEntitiesByClass(AnimalEntity.class, searchBox, entity -> true)) {
                        if (Projection.isWithinRenderDistance(animal)
                           && Projection.canSee(animal.getBoundingBox())
                           && Projection.fillVector4D(animal, this.vecScratch)) {
                           this.drawBox(false, this.vecScratch, animal);
                        }
                     }
                  }

                  if (selItems) {
                     for (ItemEntity item : mc.world.getEntitiesByClass(ItemEntity.class, searchBox, entity -> true)) {
                        if (Projection.isWithinRenderDistance(item)
                           && Projection.canSee(item.getBoundingBox())
                           && Projection.fillVector4D(item, this.vecScratch)) {
                           this.drawBox(false, this.vecScratch, item);
                        }
                     }
                  }

                  if (selArmor) {
                     for (ArmorStandEntity armorStand : mc.world.getEntitiesByClass(ArmorStandEntity.class, searchBox, entity -> true)) {
                        if (Projection.isWithinRenderDistance(armorStand)
                           && Projection.canSee(armorStand.getBoundingBox())
                           && Projection.fillVector4D(armorStand, this.vecScratch)) {
                           this.drawBox(false, this.vecScratch, armorStand);
                        }
                     }
                  }

                  matrix.pop();
                  RenderSystem.enableDepthTest();
               }
            }
         }
      }
   }

   public void renderPreviewPotions(MatrixStack matrix, LivingEntity entity, Vector4d vec) {
      if (this.potions.isValue() && entity != null && vec != null) {
         this.drawPotionPanels(matrix, entity, vec);
      }
   }

   private void drawPotionPanels(MatrixStack matrix, LivingEntity entity, Vector4d vec) {
      Collection<StatusEffectInstance> effects = entity.getStatusEffects();
      if (!effects.isEmpty()) {
         float boxHeight = (float)Math.max(1.0, vec.w - vec.y);
         float userScale = MathHelper.clamp(this.potionScale.getValue(), 0.5F, 2.0F);
         float distanceScale = MathHelper.clamp(boxHeight / 68.0F, 0.32F, 1.0F);
         float panelScale = userScale * distanceScale;
         int fontSize = MathHelper.clamp(MathHelper.floor(10.0F * panelScale), 8, 20);
         FontRenderer font = Fonts.getSize(fontSize, Fonts.Type.DEFAULT);
         float iconSize = Math.max(6.0F, 8.0F * panelScale);
         float panelGap = 1.5F * panelScale;
         float paddingX = 3.0F * panelScale;
         float iconTextGap = 2.5F * panelScale;
         float verticalPad = 2.0F * panelScale;
         float textHeight = font.getStringHeight("Ag");
         float panelHeight = Math.max(8.0F, Math.max(11.0F * panelScale, textHeight + verticalPad * 2.0F));
         float rowContentHeight = Math.max(iconSize, textHeight);
         float panelX = (float)vec.z + 3.0F * distanceScale;
         float panelY = (float)vec.y;
         int drawn = 0;

         for (StatusEffectInstance effect : effects) {
            if (drawn >= 6) {
               break;
            }

            int duration = effect.getDuration();
            if (isRenderableDuration(duration)) {
               String name = ((StatusEffect)effect.getEffectType().value()).getName().getString();
               int level = effect.getAmplifier() + 1;
               String timer = formatPotionDuration(duration);
               String label = name + " " + level + "  " + timer;
               float textWidth = font.getStringWidth(label);
               float contentWidth = iconSize + iconTextGap + textWidth;
               float panelWidth = paddingX * 2.0F + contentWidth;
               float rowY = panelY + drawn * (panelHeight + panelGap);
               Render2D.drawQuad(panelX, rowY, panelWidth, panelHeight, potionPanelBackground(effect));
               drawn++;
            }
         }

         Render2D.flushQuads(matrix);
         drawn = 0;

         for (StatusEffectInstance effect : effects) {
            if (drawn >= 6) {
               break;
            }

            int duration = effect.getDuration();
            if (isRenderableDuration(duration)) {
               String name = ((StatusEffect)effect.getEffectType().value()).getName().getString();
               int level = effect.getAmplifier() + 1;
               String timer = formatPotionDuration(duration);
               String label = name + " " + level + "  " + timer;
               float textWidth = font.getStringWidth(label);
               float contentWidth = iconSize + iconTextGap + textWidth;
               float panelWidth = paddingX * 2.0F + contentWidth;
               float rowY = panelY + drawn * (panelHeight + panelGap);
               float contentX = panelX + paddingX + (panelWidth - paddingX * 2.0F - contentWidth) * 0.5F;
               float contentY = rowY + (panelHeight - rowContentHeight) * 0.5F;
               Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType());
               float iconY = contentY + (rowContentHeight - iconSize) * 0.5F;
               Render2D.drawSprite(matrix, sprite, contentX, iconY, iconSize, (int)iconSize, -1);
               float textX = contentX + iconSize + iconTextGap;
               float textY = font.getCenteredY(contentY, rowContentHeight, label);
               font.drawFlatString(matrix, label, textX, textY, -1);
               drawn++;
            }
         }
      }
   }

   private static boolean isValidPotionScreenVec(Vector4d vec) {
      if (vec == null) {
         return false;
      } else {
         float width = (float)(vec.z - vec.x);
         float height = (float)(vec.w - vec.y);
         return width > 0.5F && height > 0.5F;
      }
   }

   private static int potionPanelBackground(StatusEffectInstance effect) {
      int effectRgb = ((StatusEffect)effect.getEffectType().value()).getColor() & 16777215;
      int darkened = ColorAssist.multBright(effectRgb | 0xFF000000, 2.55F);
      return ColorAssist.multAlpha(darkened, 0.38F);
   }

   private static boolean isRenderableDuration(int ticks) {
      return ticks == -1 || ticks > 0;
   }

   private static String formatPotionDuration(int ticks) {
      if (ticks != -1 && ticks <= 72000) {
         int totalSeconds = ticks / 20;
         int minutes = totalSeconds / 60;
         int seconds = totalSeconds % 60;
         return minutes + ":" + String.format("%02d", seconds);
      } else {
         return "**:**";
      }
   }

   private void drawBox(boolean friend, Vector4d vec, Entity entity) {
      int baseColor = friend ? ColorAssist.getFriendColor() : color.getColorWithAlpha();
      int alpha = (int)(color.getAlpha() * 255.0F);
      int client = baseColor & 16777215 | alpha << 24;
      int black = ColorAssist.HALF_BLACK;
      float posX = (float)vec.x;
      float posY = (float)vec.y;
      float endPosX = (float)vec.z;
      float endPosY = (float)vec.w;
      float size = (endPosX - posX) / 3.0F;
      if (boxType.get().equals("Корнер")) {
         Render2D.drawQuad(posX - 0.5F, posY - 0.5F, size, 1.0F, client);
         Render2D.drawQuad(posX - 0.5F, posY, 1.0F, size + 0.5F, client);
         Render2D.drawQuad(posX - 0.5F, endPosY - size - 0.5F, 1.0F, size, client);
         Render2D.drawQuad(posX - 0.5F, endPosY - 0.5F, size, 1.0F, client);
         Render2D.drawQuad(endPosX - size + 1.0F, posY - 0.5F, size, 1.0F, client);
         Render2D.drawQuad(endPosX - 0.5F, posY, 1.0F, size + 0.5F, client);
         Render2D.drawQuad(endPosX - 0.5F, endPosY - size - 0.5F, 1.0F, size, client);
         Render2D.drawQuad(endPosX - size + 1.0F, endPosY - 0.5F, size, 1.0F, client);
      } else if (boxType.get().equals("Обычный")) {
         Render2D.drawQuad(posX - 0.5F, posY - 0.5F, endPosX - posX + 1.0F, 1.0F, client);
         Render2D.drawQuad(posX - 0.5F, posY - 0.5F, 1.0F, endPosY - posY + 1.0F, client);
         Render2D.drawQuad(posX - 0.5F, endPosY - 0.5F, endPosX - posX + 1.0F, 1.0F, client);
         Render2D.drawQuad(endPosX - 0.5F, posY - 0.5F, 1.0F, endPosY - posY + 1.0F, client);
      }
   }

   public void renderPreviewBox(Vector4d vec, boolean friend) {
      if (vec != null) {
         if (!boxType.get().equals("Свечение")) {
            this.drawBox(friend, vec, null);
         }
      }
   }

   public void renderPreviewBox(MatrixStack matrix, Vector4d vec, boolean friend, float yaw, float pitch) {
      if (vec != null) {
         String mode = boxType.get();
         if (mode.equals("Свечение")) {
            this.drawPreview3dBox(matrix, vec, friend, yaw, pitch, true);
         } else {
            this.drawBox(friend, vec, null);
         }
      }
   }

   private int[] previewGlowColors(int rgb, int alpha) {
      int bottom = rgb | alpha << 24;
      return new int[]{rgb, rgb, bottom, bottom, rgb, rgb, bottom, bottom};
   }

   private void drawPreviewGlowLines(MatrixStack matrix, float[] sx, float[] sy, int[][] edges, int[] vertColors, float thickness) {
      Matrix4f m = matrix.peek().getPositionMatrix();
      float half = thickness * 0.5F;
      float shaderAlpha = RenderSystem.getShaderColor()[3];
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

      for (int[] edge : edges) {
         int a = edge[0];
         int b = edge[1];
         int colorA = ColorAssist.multAlpha(vertColors[a], shaderAlpha);
         int colorB = ColorAssist.multAlpha(vertColors[b], shaderAlpha);
         float dx = sx[b] - sx[a];
         float dy = sy[b] - sy[a];
         float length = (float)Math.sqrt(dx * dx + dy * dy);
         if (!(length < 1.0E-4F)) {
            float nx = -dy / length * half;
            float ny = dx / length * half;
            buffer.vertex(m, sx[a] + nx, sy[a] + ny, 0.0F).color(colorA);
            buffer.vertex(m, sx[a] - nx, sy[a] - ny, 0.0F).color(colorA);
            buffer.vertex(m, sx[b] - nx, sy[b] - ny, 0.0F).color(colorB);
            buffer.vertex(m, sx[b] + nx, sy[b] + ny, 0.0F).color(colorB);
         }
      }

      BufferDraw.draw(buffer);
      RenderSystem.depthMask(true);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   private void drawPreview3dBox(MatrixStack matrix, Vector4d vec, boolean friend, float yaw, float pitch, boolean glow) {
      int baseColor = friend ? ColorAssist.getFriendColor() : color.getColorWithAlpha();
      int rgb = baseColor & 16777215;
      float posX = (float)vec.x;
      float posY = (float)vec.y;
      float endPosX = (float)vec.z;
      float endPosY = (float)vec.w;
      float cx = (posX + endPosX) * 0.5F;
      float cy = (posY + endPosY) * 0.5F;
      float hw = (endPosX - posX) * 0.5F;
      float hh = (endPosY - posY) * 0.5F;
      float[] lx = new float[]{-hw, hw, hw, -hw, -hw, hw, hw, -hw};
      float[] ly = new float[]{-hh, -hh, hh, hh, -hh, -hh, hh, hh};
      float[] lz = new float[]{-hw, -hw, -hw, -hw, hw, hw, hw, hw};
      double yawR = Math.toRadians(yaw);
      double pitchR = Math.toRadians(pitch);
      float cosY = (float)Math.cos(yawR);
      float sinY = (float)Math.sin(yawR);
      float cosP = (float)Math.cos(pitchR);
      float sinP = (float)Math.sin(pitchR);
      float[] sx = new float[8];
      float[] sy = new float[8];

      for (int i = 0; i < 8; i++) {
         float rx = lx[i] * cosY + lz[i] * sinY;
         float rz = -lx[i] * sinY + lz[i] * cosY;
         float ry = ly[i] * cosP - rz * sinP;
         sx[i] = cx + rx;
         sy[i] = cy + ry;
      }

      int[][] edges = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
      if (glow) {
         float t = this.glowThickness.getValue();
         float left = posX;
         float right = endPosX;
         float topY = posY;
         float bottomY = endPosY;
         float boxWidth = endPosX - posX;
         float boxHeight = endPosY - posY;
         Identifier bloomTexture = Identifier.of("textures/bloom.png");
         Matrix4f bloomMatrix = matrix.peek().getPositionMatrix();
         float bloomShaderAlpha = RenderSystem.getShaderColor()[3];
         float bloomMarginX = boxWidth * 1.15F + t * 6.0F;
         float bloomMarginY = boxHeight * 0.22F + t * 6.0F;
         int[] bloomAlpha = new int[]{60, 110, 190};
         float[] bloomSpread = new float[]{1.0F, 0.62F, 0.3F};
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.setShaderTexture(0, bloomTexture);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

         for (int p = 0; p < bloomAlpha.length; p++) {
            float gx1 = left - bloomMarginX * bloomSpread[p];
            float gx2 = right + bloomMarginX * bloomSpread[p];
            float gy1 = topY - bloomMarginY * bloomSpread[p];
            float gy2 = bottomY + bloomMarginY * bloomSpread[p];
            int glowColor = ColorAssist.multAlpha(rgb | bloomAlpha[p] << 24, bloomShaderAlpha);
            BufferBuilder bloomBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            bloomBuffer.vertex(bloomMatrix, gx1, gy1, 0.0F).texture(0.0F, 0.0F).color(glowColor);
            bloomBuffer.vertex(bloomMatrix, gx1, gy2, 0.0F).texture(0.0F, 1.0F).color(glowColor);
            bloomBuffer.vertex(bloomMatrix, gx2, gy2, 0.0F).texture(1.0F, 1.0F).color(glowColor);
            bloomBuffer.vertex(bloomMatrix, gx2, gy1, 0.0F).texture(1.0F, 0.0F).color(glowColor);
            BufferDraw.draw(bloomBuffer);
         }

         RenderSystem.depthMask(true);
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
         int fillSteps = 12;

         for (int s = 0; s < fillSteps; s++) {
            float f = (float)s / (fillSteps - 1);
            float stripY = bottomY - boxHeight * ((float)(s + 1) / fillSteps);
            int fillAlpha = (int)(70.0F * (1.0F - f));
            if (fillAlpha > 1) {
               Render2D.drawQuad(left, stripY, boxWidth, boxHeight / fillSteps + 0.75F, rgb | fillAlpha << 24);
            }
         }

         float[] layerThickness = new float[]{t * 3.2F, t * 2.0F, t * 1.1F, Math.max(1.0F, t * 0.5F)};
         int[] layerAlpha = new int[]{45, 85, 140, 235};

         for (int layer = 0; layer < layerThickness.length; layer++) {
            float lt = layerThickness[layer];
            int baseAlpha = layerAlpha[layer];
            int topAlpha = (int)(baseAlpha * 0.12F);
            int midAlpha = (int)(baseAlpha * 0.55F);
            Render2D.drawQuad(left - lt * 0.5F, bottomY - lt * 0.5F, boxWidth + lt, lt, rgb | baseAlpha << 24);
            if (topAlpha > 1) {
               Render2D.drawQuad(left - lt * 0.5F, topY - lt * 0.5F, boxWidth + lt, lt, rgb | topAlpha << 24);
            }

            if (midAlpha > 1) {
               Render2D.drawQuad(left - lt * 0.5F, topY, lt, boxHeight, rgb | midAlpha << 24);
               Render2D.drawQuad(right - lt * 0.5F, topY, lt, boxHeight, rgb | midAlpha << 24);
            }
         }
      } else {
         for (int[] edge : edges) {
            int a = edge[0];
            int b = edge[1];
            Render2D.drawLine(matrix, sx[a], sy[a], sx[b], sy[b], 2.4F, ColorAssist.HALF_BLACK);
            Render2D.drawLine(matrix, sx[a], sy[a], sx[b], sy[b], 1.2F, rgb | 0xFF000000);
         }
      }
   }
}
