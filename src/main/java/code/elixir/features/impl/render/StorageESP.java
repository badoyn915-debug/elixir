package code.elixir.features.impl.render;

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
import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.theme.ThemeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

public class StorageESP extends Module {
   private final MultiSelectSetting types = new MultiSelectSetting("Типы хранилищ", "Хранилища для подсветки")
      .value("Сундуки", "Эндер-сундуки", "Шалкеры", "Бочки", "Печки", "Воронки")
      .selected("Сундуки", "Эндер-сундуки", "Шалкеры", "Бочки");

   private final RadioSetting style = new RadioSetting(
      "Стиль", "Вид отображения", new String[]{"Бокс и контур", "Только контур", "Только бокс"}, "Бокс и контур"
   );

   private final SliderSettings range = new SliderSettings("Радиус", "Радиус поиска в блоках")
      .range(8.0F, 64.0F)
      .setValue(32.0F)
      .step(4.0F);

   private final SliderSettings boxAlpha = new SliderSettings("Прозрачность заливки", "Прозрачность внутреннего бокса")
      .range(0.05F, 0.8F)
      .setValue(0.25F)
      .step(0.05F);

   private final SliderSettings outlineAlpha = new SliderSettings("Прозрачность контура", "Прозрачность внешнего контура")
      .range(0.1F, 1.0F)
      .setValue(0.9F)
      .step(0.05F);

   private final ColorSetting chestColor = new ColorSetting("Цвет сундуков", "Цвет обычных сундуков")
      .value(new Color(255, 170, 0, 255).getRGB());
   private final ColorSetting enderColor = new ColorSetting("Цвет эндер-сундуков", "Цвет сундуков Края")
      .value(new Color(180, 50, 255, 255).getRGB());
   private final ColorSetting shulkerColor = new ColorSetting("Цвет шалкеров", "Цвет шалкеровых ящиков")
      .value(new Color(240, 90, 160, 255).getRGB());
   private final ColorSetting barrelColor = new ColorSetting("Цвет бочек", "Цвет бочек")
      .value(new Color(180, 120, 60, 255).getRGB());
   private final ColorSetting furnaceColor = new ColorSetting("Цвет печей", "Цвет печей")
      .value(new Color(120, 130, 140, 255).getRGB());
   private final ColorSetting hopperColor = new ColorSetting("Цвет воронок", "Цвет воронок")
      .value(new Color(80, 90, 100, 255).getRGB());

   public static StorageESP getInstance() {
      return Instance.get(StorageESP.class);
   }

   public StorageESP() {
      super("StorageESP", "Storage ESP", ModuleCategory.ESP);
      this.setup(new Setting[]{
         this.types,
         this.style,
         this.range,
         this.boxAlpha,
         this.outlineAlpha,
         this.chestColor,
         this.enderColor,
         this.shulkerColor,
         this.barrelColor,
         this.furnaceColor,
         this.hopperColor
      });
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      if (mc.world == null || mc.player == null) return;

      float partialTicks = event.getPartialTicks();
      MatrixStack matrices = event.getStack();
      Matrix4f mat = matrices.peek().getPositionMatrix();

      BlockPos playerPos = mc.player.getBlockPos();
      int radiusBlocks = (int)this.range.getValue();
      int chunkRadius = (radiusBlocks >> 4) + 1;
      int pChunkX = playerPos.getX() >> 4;
      int pChunkZ = playerPos.getZ() >> 4;

      List<StorageEntry> entries = new ArrayList<>();
      double maxDistSq = radiusBlocks * radiusBlocks;

      for (int cx = pChunkX - chunkRadius; cx <= pChunkX + chunkRadius; cx++) {
         for (int cz = pChunkZ - chunkRadius; cz <= pChunkZ + chunkRadius; cz++) {
            WorldChunk chunk = mc.world.getChunk(cx, cz);
            if (chunk == null) continue;

            for (BlockEntity be : chunk.getBlockEntities().values()) {
               BlockPos pos = be.getPos();
               if (pos.getSquaredDistance(playerPos) > maxDistSq) continue;

               int baseColor = this.getStorageColor(be);
               if (baseColor != 0) {
                  Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
                  if (be instanceof ChestBlockEntity || be instanceof EnderChestBlockEntity) {
                     // Slightly shrink for chest model size
                     box = new Box(pos.getX() + 0.0625, pos.getY(), pos.getZ() + 0.0625, pos.getX() + 0.9375, pos.getY() + 0.875, pos.getZ() + 0.9375);
                  }
                  entries.add(new StorageEntry(box, baseColor));
               }
            }
         }
      }

      if (entries.isEmpty()) return;

      boolean renderBox = !this.style.get().equals("Только контур");
      boolean renderOutline = !this.style.get().equals("Только бокс");

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);

      float fBoxAlpha = this.boxAlpha.getValue();
      float fOutlineAlpha = this.outlineAlpha.getValue();

      // Filled Boxes
      if (renderBox) {
         BufferBuilder boxBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         for (StorageEntry entry : entries) {
            int fillCol = ColorAssist.multAlpha(entry.color, fBoxAlpha);
            drawFilledBox(mat, boxBuffer, entry.box, fillCol);
         }
         BufferDraw.draw(boxBuffer);
      }

      // Outlines
      if (renderOutline) {
         BufferBuilder outlineBuffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
         for (StorageEntry entry : entries) {
            int lineCol = ColorAssist.multAlpha(entry.color, fOutlineAlpha);
            drawBoxOutline(mat, outlineBuffer, entry.box, lineCol);
         }
         BufferDraw.draw(outlineBuffer);
      }

      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   private int getStorageColor(BlockEntity be) {
      if ((be instanceof ChestBlockEntity || be instanceof TrappedChestBlockEntity) && this.types.isSelected("Сундуки")) {
         return this.chestColor.getColor();
      }
      if (be instanceof EnderChestBlockEntity && this.types.isSelected("Эндер-сундуки")) {
         return this.enderColor.getColor();
      }
      if (be instanceof ShulkerBoxBlockEntity && this.types.isSelected("Шалкеры")) {
         return this.shulkerColor.getColor();
      }
      if (be instanceof BarrelBlockEntity && this.types.isSelected("Бочки")) {
         return this.barrelColor.getColor();
      }
      if (be instanceof AbstractFurnaceBlockEntity && this.types.isSelected("Печки")) {
         return this.furnaceColor.getColor();
      }
      if ((be instanceof HopperBlockEntity || be instanceof DispenserBlockEntity || be instanceof DropperBlockEntity) && this.types.isSelected("Воронки")) {
         return this.hopperColor.getColor();
      }
      return 0;
   }

   private static void drawFilledBox(Matrix4f mat, BufferBuilder buffer, Box b, int color) {
      float x1 = (float)b.minX, y1 = (float)b.minY, z1 = (float)b.minZ;
      float x2 = (float)b.maxX, y2 = (float)b.maxY, z2 = (float)b.maxZ;

      // Down
      buffer.vertex(mat, x1, y1, z1).color(color);
      buffer.vertex(mat, x2, y1, z1).color(color);
      buffer.vertex(mat, x2, y1, z2).color(color);
      buffer.vertex(mat, x1, y1, z2).color(color);

      // Up
      buffer.vertex(mat, x1, y2, z1).color(color);
      buffer.vertex(mat, x1, y2, z2).color(color);
      buffer.vertex(mat, x2, y2, z2).color(color);
      buffer.vertex(mat, x2, y2, z1).color(color);

      // North
      buffer.vertex(mat, x1, y1, z1).color(color);
      buffer.vertex(mat, x1, y2, z1).color(color);
      buffer.vertex(mat, x2, y2, z1).color(color);
      buffer.vertex(mat, x2, y1, z1).color(color);

      // South
      buffer.vertex(mat, x1, y1, z2).color(color);
      buffer.vertex(mat, x2, y1, z2).color(color);
      buffer.vertex(mat, x2, y2, z2).color(color);
      buffer.vertex(mat, x1, y2, z2).color(color);

      // West
      buffer.vertex(mat, x1, y1, z1).color(color);
      buffer.vertex(mat, x1, y1, z2).color(color);
      buffer.vertex(mat, x1, y2, z2).color(color);
      buffer.vertex(mat, x1, y2, z1).color(color);

      // East
      buffer.vertex(mat, x2, y1, z1).color(color);
      buffer.vertex(mat, x2, y2, z1).color(color);
      buffer.vertex(mat, x2, y2, z2).color(color);
      buffer.vertex(mat, x2, y1, z2).color(color);
   }

   private static void drawBoxOutline(Matrix4f mat, BufferBuilder buffer, Box b, int color) {
      float x1 = (float)b.minX, y1 = (float)b.minY, z1 = (float)b.minZ;
      float x2 = (float)b.maxX, y2 = (float)b.maxY, z2 = (float)b.maxZ;

      // Bottom
      buffer.vertex(mat, x1, y1, z1).color(color); buffer.vertex(mat, x2, y1, z1).color(color);
      buffer.vertex(mat, x2, y1, z1).color(color); buffer.vertex(mat, x2, y1, z2).color(color);
      buffer.vertex(mat, x2, y1, z2).color(color); buffer.vertex(mat, x1, y1, z2).color(color);
      buffer.vertex(mat, x1, y1, z2).color(color); buffer.vertex(mat, x1, y1, z1).color(color);

      // Top
      buffer.vertex(mat, x1, y2, z1).color(color); buffer.vertex(mat, x2, y2, z1).color(color);
      buffer.vertex(mat, x2, y2, z1).color(color); buffer.vertex(mat, x2, y2, z2).color(color);
      buffer.vertex(mat, x2, y2, z2).color(color); buffer.vertex(mat, x1, y2, z2).color(color);
      buffer.vertex(mat, x1, y2, z2).color(color); buffer.vertex(mat, x1, y2, z1).color(color);

      // Vertical pillars
      buffer.vertex(mat, x1, y1, z1).color(color); buffer.vertex(mat, x1, y2, z1).color(color);
      buffer.vertex(mat, x2, y1, z1).color(color); buffer.vertex(mat, x2, y2, z1).color(color);
      buffer.vertex(mat, x2, y1, z2).color(color); buffer.vertex(mat, x2, y2, z2).color(color);
      buffer.vertex(mat, x1, y1, z2).color(color); buffer.vertex(mat, x1, y2, z2).color(color);
   }

   private record StorageEntry(Box box, int color) {}
}
