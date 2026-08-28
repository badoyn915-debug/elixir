package code.elixir.display.screens.clickgui.components.implement.other;

import code.elixir.Elixir;
import code.elixir.display.screens.clickgui.MenuScreen;
import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.managers.file.exception.FileLoadException;
import code.elixir.utils.client.managers.file.exception.FileSaveException;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.scissor.ScissorAssist;
import code.elixir.utils.math.calc.Calculate;
import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class BackgroundComponent extends AbstractComponent {
   private String editingConfig = null;
   private String newName = "";
   private int editCursor = 0;
   private boolean isDefaultTab = true;
   private float highlightX = 55.0F;
   private List<Map<String, Object>> configs = new ArrayList<>();
   private String configInput = "";
   private boolean editingInput = false;
   private int inputCursor = 0;
   private float scroll = 0.0F;
   private float smoothedScroll = 0.0F;
   private boolean loadedConfigs = false;
   private boolean cloudLoading = false;
   private long cloudLoadStartTime = 0L;
   private boolean cloudDataReady = false;
   private List<Map<String, Object>> tempConfigs = new ArrayList<>();
   private float loadingAlpha = 0.0F;
   private float configsAlpha = 0.0F;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
      String currentTime = LocalTime.now().format(formatter);
      String point = " • ";
      rectangle.render(
         ShapeProperties.create(matrix, this.x, this.y, this.width, this.height)
            .round(8.0F)
            .softness(1.0F)
            .thickness(2.0F)
            .outlineColor(new Color(33, 33, 33, 255).getRGB())
            .color(new Color(18, 19, 20, 255).getRGB())
            .build()
      );
      List<Map<String, Object>> displayedConfigs = new ArrayList<>();
      if (MenuScreen.getInstance().getCategory() == ModuleCategory.CONFIGS) {
         if (!this.loadedConfigs) {
            this.refreshConfigs();
            this.loadedConfigs = true;
         }

         if (this.cloudLoading && !this.isDefaultTab) {
            if (this.cloudDataReady && System.currentTimeMillis() - this.cloudLoadStartTime >= 3000L) {
               this.configs = this.tempConfigs;
               this.cloudLoading = false;
            }

            this.loadingAlpha = Calculate.interpolate(this.loadingAlpha, 1.0F, 0.1F);
            this.configsAlpha = Calculate.interpolate(this.configsAlpha, 0.0F, 0.1F);
         } else {
            this.loadingAlpha = Calculate.interpolate(this.loadingAlpha, 0.0F, 0.1F);
            this.configsAlpha = Calculate.interpolate(this.configsAlpha, 1.0F, 0.1F);
         }

         rectangle.render(
            ShapeProperties.create(context.getMatrices(), this.x + 55.0F, this.y + 38.0F, 70.0, 15.0)
               .round(3.0F)
               .thickness(2.0F)
               .softness(1.0F)
               .outlineColor(new Color(54, 54, 56, 255).getRGB())
               .color(
                  new Color(31, 27, 35, 75).getRGB(),
                  new Color(31, 27, 35, 75).getRGB(),
                  new Color(31, 27, 35, 75).getRGB(),
                  new Color(31, 27, 35, 75).getRGB()
               )
               .build()
         );
         float targetX = this.isDefaultTab ? 55.0F : 90.0F;
         this.highlightX = Calculate.interpolate(this.highlightX, targetX, 0.2F);
         rectangle.render(
            ShapeProperties.create(context.getMatrices(), this.x + this.highlightX, this.y + 38.0F, 35.0, 15.0)
               .round(3.0F)
               .thickness(0.0F)
               .softness(0.0F)
               .outlineColor(new Color(54, 54, 56, 0).getRGB())
               .color(
                  new Color(65, 65, 65, 255).getRGB(),
                  new Color(65, 65, 65, 255).getRGB(),
                  new Color(65, 65, 65, 255).getRGB(),
                  new Color(65, 65, 65, 255).getRGB()
               )
               .build()
         );
         rectangle.render(
            ShapeProperties.create(context.getMatrices(), this.x + 43.0F, this.y + 60.0F, this.width - 43.0F, 0.5)
               .color(
                  new Color(55, 55, 70, 250).getRGB(),
                  new Color(55, 55, 70, 15).getRGB(),
                  new Color(55, 55, 70, 250).getRGB(),
                  new Color(55, 55, 70, 15).getRGB()
               )
               .build()
         );
         Fonts.getSize(16, Fonts.Type.DEFAULT).drawString(matrix, "Default", this.x + 60.0F, this.y + 43.0F, ColorAssist.getText(0.7F));
         Fonts.getSize(16, Fonts.Type.DEFAULT).drawString(matrix, "Cloud", this.x + 97.0F, this.y + 43.0F, ColorAssist.getText(0.7F));
         rectangle.render(
            ShapeProperties.create(context.getMatrices(), this.x + 340.0F, this.y + 38.0F, 80.0, 15.0)
               .round(3.0F)
               .thickness(2.0F)
               .softness(1.0F)
               .outlineColor(new Color(54, 54, 56, 255).getRGB())
               .color(
                  new Color(31, 27, 35, 75).getRGB(),
                  new Color(31, 27, 35, 75).getRGB(),
                  new Color(31, 27, 35, 75).getRGB(),
                  new Color(31, 27, 35, 75).getRGB()
               )
               .build()
         );
         rectangle.render(
            ShapeProperties.create(context.getMatrices(), this.x + 292.0F, this.y + 38.0F, 40.0, 15.0)
               .round(3.0F)
               .thickness(2.0F)
               .softness(1.0F)
               .outlineColor(new Color(54, 54, 56, 255).getRGB())
               .color(
                  new Color(31, 27, 35, 75).getRGB(),
                  new Color(31, 27, 35, 75).getRGB(),
                  new Color(31, 27, 35, 75).getRGB(),
                  new Color(31, 27, 35, 75).getRGB()
               )
               .build()
         );
         Fonts.getSize(20, Fonts.Type.GUIICONS).drawString(matrix, "M", this.x + 296.0F, this.y + 42.0F, ColorAssist.getText(1.0F));
         Fonts.getSize(16, Fonts.Type.REGULAR).drawString(matrix, "Save", this.x + 307.0F, this.y + 43.5F, ColorAssist.getText(1.0F));
         rectangle.render(
            ShapeProperties.create(context.getMatrices(), this.x + 250.0F, this.y + 38.0F, 38.0, 15.0)
               .round(3.0F)
               .thickness(2.0F)
               .softness(1.0F)
               .outlineColor(new Color(54, 54, 56, 255).getRGB())
               .color(
                  new Color(31, 27, 35, 75).getRGB(),
                  new Color(31, 27, 35, 75).getRGB(),
                  new Color(31, 27, 35, 75).getRGB(),
                  new Color(31, 27, 35, 75).getRGB()
               )
               .build()
         );
         Fonts.getSize(21, Fonts.Type.GUIICONS).drawString(matrix, "O", this.x + 253.0F, this.y + 43.0F, ColorAssist.getText(1.0F));
         Fonts.getSize(16, Fonts.Type.REGULAR).drawString(matrix, "Clear", this.x + 263.0F, this.y + 43.5F, ColorAssist.getText(1.0F));
         String placeholder = this.isDefaultTab ? "Поиск" : "Добавить по ID";
         String inputDisplay = this.configInput.isEmpty() && !this.editingInput ? placeholder : this.configInput;
         Fonts.getSize(15, Fonts.Type.REGULAR).drawString(matrix, inputDisplay, this.x + 343.0F, this.y + 43.5F, ColorAssist.getText(0.6F));
         Fonts.getSize(26, Fonts.Type.ICONS).drawString(matrix, "U", this.x + 405.0F, this.y + 41.0F, ColorAssist.getText(0.6F));
         if (this.editingInput && System.currentTimeMillis() % 1000L < 500L) {
            float curWidth = Fonts.getSize(15, Fonts.Type.REGULAR).getStringWidth(this.configInput.substring(0, this.inputCursor));
            Fonts.getSize(15, Fonts.Type.DEFAULT).drawString(matrix, "|", this.x + 342.0F + curWidth, this.y + 43.5F - 0.5F, ColorAssist.getText(0.7F));
         }

         displayedConfigs = this.isDefaultTab
            ? this.configs.stream().filter(m -> ((String)m.get("name")).toLowerCase().contains(this.configInput.toLowerCase())).collect(Collectors.toList())
            : this.configs;
         int configsPerRow = 2;
         int numConfigs = displayedConfigs.size();
         int rows = (numConfigs + configsPerRow - 1) / configsPerRow;
         float contentHeight = numConfigs > 0 ? 50.0F + (rows - 1) * 55.0F : 0.0F;
         float viewHeight = this.height - 70.0F;
         float maxScrollAmount = Math.max(0.0F, contentHeight - viewHeight) + 7.0F;
         if (numConfigs < 7) {
            maxScrollAmount = 0.0F;
            this.scroll = 0.0F;
            this.smoothedScroll = 0.0F;
         }

         this.scroll = MathHelper.clamp(this.scroll, -maxScrollAmount, 0.0F);
         this.smoothedScroll = Calculate.interpolate(this.smoothedScroll, this.scroll, 0.2F);
         Matrix4f positionMatrix = matrix.peek().getPositionMatrix();
         ScissorAssist scissorManager = Elixir.getInstance().getScissorManager();
         float listX = this.x + 43.0F;
         float listY = this.y + 65.0F;
         float listWidth = this.width - 43.0F - 15.0F;
         scissorManager.push(positionMatrix, listX, listY, listWidth, viewHeight);
         if (!this.isDefaultTab) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.configsAlpha);
         }

         float configY = this.y + 70.0F + this.smoothedScroll;
         int index = 0;

         for (Map<String, Object> map : displayedConfigs) {
            String config = (String)map.get("name");
            float configX = this.x + 55.0F + index % configsPerRow * 190;
            if (index % configsPerRow == 0 && index > 0) {
               configY += 55.0F;
            }

            if (configY + 50.0F > this.y + 60.0F && configY < this.y + this.height) {
               rectangle.render(
                  ShapeProperties.create(context.getMatrices(), configX, configY, 180.0, 50.0)
                     .round(5.0F)
                     .thickness(2.0F)
                     .softness(1.0F)
                     .outlineColor(new Color(54, 54, 56, 255).getRGB())
                     .color(
                        new Color(31, 27, 35, 75).getRGB(),
                        new Color(31, 27, 35, 75).getRGB(),
                        new Color(31, 27, 35, 75).getRGB(),
                        new Color(31, 27, 35, 75).getRGB()
                     )
                     .build()
               );
               rectangle.render(
                  ShapeProperties.create(context.getMatrices(), configX, configY + 22.0F, 180.0, 0.5)
                     .color(
                        new Color(55, 55, 70, 250).getRGB(),
                        new Color(55, 55, 70, 15).getRGB(),
                        new Color(55, 55, 70, 250).getRGB(),
                        new Color(55, 55, 70, 15).getRGB()
                     )
                     .build()
               );
               rectangle.render(
                  ShapeProperties.create(context.getMatrices(), configX, configY, 20.5, 19.0)
                     .round(1.0F, 7.0F, 4.0F, 1.0F)
                     .thickness(2.0F)
                     .softness(1.0F)
                     .outlineColor(new Color(54, 54, 56, 255).getRGB())
                     .color(
                        new Color(55, 55, 55, 255).getRGB(),
                        new Color(55, 55, 55, 255).getRGB(),
                        new Color(55, 55, 55, 255).getRGB(),
                        new Color(55, 55, 55, 255).getRGB()
                     )
                     .build()
               );
               Fonts.getSize(26, Fonts.Type.ICONSCATEGORY).drawString(matrix, "F", configX + 3.5F, configY + 5.0F, ColorAssist.getText());
               if (config.equals(this.editingConfig)) {
                  String scrollRatio = this.newName;
                  Fonts.getSize(16, Fonts.Type.DEFAULT).drawString(matrix, scrollRatio, configX + 25.0F, configY + 9.0F, ColorAssist.getText());
                  if (System.currentTimeMillis() % 1000L < 500L) {
                     float curWidth = Fonts.getSize(16, Fonts.Type.DEFAULT).getStringWidth(this.newName.substring(0, this.editCursor));
                     Fonts.getSize(16, Fonts.Type.DEFAULT).drawString(matrix, "|", configX + 24.0F + curWidth, configY + 9.0F - 0.5F, ColorAssist.getText());
                  }
               } else {
                  Fonts.getSize(16, Fonts.Type.DEFAULT).drawString(matrix, config, configX + 25.0F, configY + 9.0F, ColorAssist.getText());
               }

               Number createdNum = (Number)map.getOrDefault("created", 0L);
               Number updatedNum = (Number)map.getOrDefault("updated", 0L);
               long createdTime = createdNum.longValue();
               long updatedTime = updatedNum.longValue();
               String createdStr = createdTime == 0L
                  ? "unknown"
                  : LocalDateTime.ofInstant(Instant.ofEpochMilli(createdTime), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
               String updatedStr = updatedTime == 0L
                  ? "unknown"
                  : LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedTime), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
               Fonts.getSize(11, Fonts.Type.REGULAR).drawString(matrix, "Created: " + createdStr, configX + 4.0F, configY + 35.0F, ColorAssist.getText(1.0F));
               String updateText = "Updated: " + updatedStr;
               boolean hasUpdate = (Boolean)map.getOrDefault("has_update", false);
               if (hasUpdate) {
                  updateText = updateText + " (доступно обновлений: 1)";
               }

               Fonts.getSize(11, Fonts.Type.REGULAR).drawString(matrix, updateText, configX + 4.0F, configY + 28.0F, ColorAssist.getText(1.0F));
               String author = (String)map.getOrDefault("owner", this.isDefaultTab ? "username" : "Unknown");
               Fonts.getSize(11, Fonts.Type.REGULAR).drawString(matrix, "Author: " + author, configX + 4.0F, configY + 42.0F, ColorAssist.getText(1.0F));
               Object avatarObj = map.getOrDefault("avatar_hash", Identifier.of("minecraft", "textures/gui/default_avatar.png"));
               Object avatarHash;
               if (avatarObj instanceof Map idMap) {
                  avatarHash = (String)idMap.get("namespace") + ":" + (String)idMap.get("path");
               } else {
                  avatarHash = avatarObj.toString();
               }

               Render2D.drawTexture(
                  context, Identifier.of((String)avatarHash), configX + 157.0F, configY + 3.0F, 16.0F, 7.5F, 0, 15, 21, ColorAssist.getGuiRectColor(1.0F)
               );
               rectangle.render(
                  ShapeProperties.create(context.getMatrices(), configX + 162.0F, configY + 35.0F, 14.0, 15.0)
                     .round(3.0F, 0.0F, 3.0F, 0.0F)
                     .thickness(2.0F)
                     .softness(1.0F)
                     .outlineColor(new Color(54, 54, 56, 255).getRGB())
                     .color(
                        new Color(55, 55, 55, 0).getRGB(),
                        new Color(55, 55, 55, 0).getRGB(),
                        new Color(55, 55, 55, 0).getRGB(),
                        new Color(55, 55, 55, 0).getRGB()
                     )
                     .build()
               );
               rectangle.render(
                  ShapeProperties.create(context.getMatrices(), configX + 146.0F, configY + 35.0F, 14.0, 15.0)
                     .round(3.0F, 0.0F, 3.0F, 0.0F)
                     .thickness(2.0F)
                     .softness(1.0F)
                     .outlineColor(new Color(54, 54, 56, 255).getRGB())
                     .color(
                        new Color(55, 55, 55, 0).getRGB(),
                        new Color(55, 55, 55, 0).getRGB(),
                        new Color(55, 55, 55, 0).getRGB(),
                        new Color(55, 55, 55, 0).getRGB()
                     )
                     .build()
               );
               rectangle.render(
                  ShapeProperties.create(context.getMatrices(), configX + 130.25F, configY + 35.0F, 14.0, 15.0)
                     .round(3.0F, 0.0F, 3.0F, 0.0F)
                     .thickness(2.0F)
                     .softness(1.0F)
                     .outlineColor(new Color(54, 54, 56, 255).getRGB())
                     .color(
                        new Color(55, 55, 55, 0).getRGB(),
                        new Color(55, 55, 55, 0).getRGB(),
                        new Color(55, 55, 55, 0).getRGB(),
                        new Color(55, 55, 55, 0).getRGB()
                     )
                     .build()
               );
               rectangle.render(
                  ShapeProperties.create(context.getMatrices(), configX + 114.35F, configY + 35.0F, 14.0, 15.0)
                     .round(3.0F, 0.0F, 3.0F, 0.0F)
                     .thickness(2.0F)
                     .softness(1.0F)
                     .outlineColor(new Color(54, 54, 56, 255).getRGB())
                     .color(
                        new Color(55, 55, 55, 0).getRGB(),
                        new Color(55, 55, 55, 0).getRGB(),
                        new Color(55, 55, 55, 0).getRGB(),
                        new Color(55, 55, 55, 0).getRGB()
                     )
                     .build()
               );
               Fonts.getSize(31, Fonts.Type.GUIICONS).drawString(matrix, "P", configX + 164.0F, configY + 36.0F, ColorAssist.getText(1.0F));
               Fonts.getSize(21, Fonts.Type.GUIICONS).drawString(matrix, "N", configX + 149.0F, configY + 39.5F, ColorAssist.getText(1.0F));
               Fonts.getSize(22, Fonts.Type.GUIICONS).drawString(matrix, "M", configX + 133.0F, configY + 38.5F, ColorAssist.getText(1.0F));
               Fonts.getSize(24, Fonts.Type.GUIICONS).drawString(matrix, "O", configX + 117.0F, configY + 38.0F, ColorAssist.getText(1.0F));
            }

            index++;
         }

         if (!this.isDefaultTab) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         scissorManager.pop();
         if (maxScrollAmount > 0.0F) {
            float scrollbarWidth = 4.0F;
            float f = this.x + this.width - 10.0F;
            float scrollbarY = this.y + 65.0F;
            float scrollbarHeight = this.height - 70.0F;
            rectangle.render(
               ShapeProperties.create(context.getMatrices(), f, scrollbarY, scrollbarWidth, scrollbarHeight)
                  .round(2.0F)
                  .color(new Color(30, 30, 30, 100).getRGB())
                  .build()
            );
            float handleHeight = Math.max(20.0F, scrollbarHeight * (viewHeight / contentHeight));
            float scrollRatio = maxScrollAmount > 0.0F ? -this.smoothedScroll / maxScrollAmount : 0.0F;
            float handleY = scrollbarY + (scrollbarHeight - handleHeight) * scrollRatio;
            rectangle.render(
               ShapeProperties.create(context.getMatrices(), f, handleY, scrollbarWidth, handleHeight)
                  .round(2.0F)
                  .color(new Color(100, 100, 100, 150).getRGB())
                  .build()
            );
         }
      } else {
         this.loadedConfigs = false;
      }

      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.x + 42.5F, this.y, 0.5, this.height)
            .color(
               new Color(55, 55, 70, 15).getRGB(), new Color(55, 55, 70, 250).getRGB(), new Color(55, 55, 70, 15).getRGB(), new Color(55, 55, 70, 250).getRGB()
            )
            .build()
      );
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.x + 43.0F, this.y + 28.0F, this.width - 43.0F, 0.5)
            .color(
               new Color(55, 55, 70, 250).getRGB(), new Color(55, 55, 70, 15).getRGB(), new Color(55, 55, 70, 250).getRGB(), new Color(55, 55, 70, 15).getRGB()
            )
            .build()
      );
      rectangle.render(
         ShapeProperties.create(matrix, this.x + 10.5F, this.y + 10.0F, 20.0, 20.0)
            .round(4.0F)
            .softness(1.0F)
            .thickness(3.0F)
            .outlineColor(new Color(33, 33, 33, 255).getRGB())
            .color(new Color(18, 19, 20, 255).getRGB())
            .build()
      );
      Fonts.getSize(26, Fonts.Type.ICONS).drawString(matrix, "A ", this.x + 14.0F, this.y + 15.0F, ColorAssist.getClientColor());
      switch (MenuScreen.getInstance().getCategory()) {
         case COMBAT: {
            String icon = "A";
            Fonts.getSize(17, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, this.x + 55.0F, this.y + 14.5F, ColorAssist.getClientColor());
            break;
         }
         case MOVEMENT: {
            String icon = "B";
            Fonts.getSize(18, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, this.x + 54.0F, this.y + 14.0F, ColorAssist.getText(1.0F));
            break;
         }
         case RENDER: {
            String icon = "C";
            Fonts.getSize(17, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, this.x + 54.0F, this.y + 14.0F, ColorAssist.getText(1.0F));
            break;
         }
         case ESP: {
            String icon = "G";
            Fonts.getSize(17, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, this.x + 54.0F, this.y + 14.0F, ColorAssist.getText(1.0F));
            break;
         }
         case PLAYER: {
            String icon = "D";
            Fonts.getSize(17, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, this.x + 54.0F, this.y + 14.0F, ColorAssist.getText(1.0F));
            break;
         }
         case MISC: {
            String icon = "E";
            Fonts.getSize(18, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, this.x + 54.0F, this.y + 14.0F, ColorAssist.getText(1.0F));
            break;
         }
         case CONFIGS: {
            String icon = "F";
            Fonts.getSize(17, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, this.x + 54.0F, this.y + 14.0F, ColorAssist.getText(1.0F));
            break;
         }
         case CUSTOMIZATION: {
            String icon = "H";
            Fonts.getSize(17, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, this.x + 54.0F, this.y + 14.0F, ColorAssist.getText(1.0F));
            break;
         }
         default: {
            String icon = MenuScreen.getInstance().getCategory().getReadableName().substring(0, 1);
            Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, this.x + 50.0F, this.y + 13.5F, ColorAssist.getText(1.0F));
         }
      }

      if (MenuScreen.getInstance().getCategory() == ModuleCategory.CONFIGS && displayedConfigs.isEmpty()) {
         Object message = "Тута пуста :(";
         float textAlpha = 0.7F;
         if (!this.isDefaultTab && this.cloudLoading) {
            long time = System.currentTimeMillis() - this.cloudLoadStartTime;
            int dotCount = (int)(time / 500L % 3L) + 1;
            message = "Loading" + ".".repeat(dotCount);
            textAlpha = this.loadingAlpha;
         } else if (!this.cloudLoading) {
            textAlpha = this.configsAlpha;
         }

         Fonts.getSize(20, Fonts.Type.DEFAULT)
            .drawString(
               matrix,
               (String)message,
               this.x + this.width / 2.0F - Fonts.getSize(20, Fonts.Type.DEFAULT).getStringWidth((String)message) / 2.0F + 10.0F,
               this.y + this.height / 2.0F + 15.0F,
               ColorAssist.getText(textAlpha)
            );
      }

      if (MenuScreen.getInstance().getCategory() == ModuleCategory.CONFIGS) {
         Fonts.getSize(15, Fonts.Type.DEFAULT)
            .drawGradientString(
               matrix,
               point + MenuScreen.getInstance().getCategory().getReadableName() + " | Beta",
               this.x + 63.0F,
               this.y + 13.5F,
               ColorAssist.getText(1.0F),
               ColorAssist.getText(1.0F)
            );
      } else {
         Fonts.getSize(15, Fonts.Type.DEFAULT)
            .drawGradientString(
               matrix,
               point + MenuScreen.getInstance().getCategory().getReadableName(),
               this.x + 63.0F,
               this.y + 13.5F,
               ColorAssist.getText(1.0F),
               ColorAssist.getText(1.0F)
            );
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.editingInput = false;
      }

      if (MenuScreen.getInstance().getCategory() == ModuleCategory.CONFIGS && button == 0) {
         if (Calculate.isHovered(mouseX, mouseY, this.x + 55.0F, this.y + 38.0F, 35.0, 15.0)) {
            this.isDefaultTab = true;
            this.loadingAlpha = 0.0F;
            this.configsAlpha = 1.0F;
            this.refreshConfigs();
            return true;
         }

         if (Calculate.isHovered(mouseX, mouseY, this.x + 90.0F, this.y + 38.0F, 35.0, 15.0)) {
            this.isDefaultTab = false;
            this.loadingAlpha = 0.0F;
            this.configsAlpha = 0.0F;
            this.refreshConfigs();
            return true;
         }

         if (Calculate.isHovered(mouseX, mouseY, this.x + 340.0F, this.y + 38.0F, 80.0, 15.0)) {
            this.editingInput = true;
            this.inputCursor = this.configInput.length();
            return true;
         }

         if (Calculate.isHovered(mouseX, mouseY, this.x + 292.0F, this.y + 38.0F, 40.0, 15.0)) {
            if (this.isDefaultTab) {
               this.createDefaultConfig();
            } else {
               this.createCloudConfig();
            }

            this.refreshConfigs();
            return true;
         }

         if (Calculate.isHovered(mouseX, mouseY, this.x + 250.0F, this.y + 38.0F, 38.0, 15.0)) {
            this.clearAllConfigs();
            this.refreshConfigs();
            return true;
         }

         List<Map<String, Object>> displayedConfigs = this.isDefaultTab
            ? this.configs.stream().filter(m -> ((String)m.get("name")).toLowerCase().contains(this.configInput.toLowerCase())).collect(Collectors.toList())
            : this.configs;
         float configY = this.y + 70.0F + this.smoothedScroll;
         int index = 0;

         for (Map<String, Object> map : displayedConfigs) {
            String config = (String)map.get("name");
            float configX = this.x + 55.0F + index % 2 * 190;
            if (index % 2 == 0 && index > 0) {
               configY += 55.0F;
            }

            double nameX;
            double var10002 = nameX = configX + 25.0F;
            double nameY;
            double var10003 = (nameY = configY + 9.0F) - 2.0;
            double var10004 = Fonts.getSize(16, Fonts.Type.DEFAULT).getStringWidth(config);
            double nameHeight = 10.0;
            if (Calculate.isHovered(mouseX, mouseY, var10002, var10003, var10004, 10.0)) {
               if (this.isDefaultTab) {
                  this.editingConfig = config;
                  this.newName = config;
                  this.editCursor = this.newName.length();
               } else {
                  MinecraftClient.getInstance().keyboard.setClipboard(config);
               }

               return true;
            }

            if (Calculate.isHovered(mouseX, mouseY, configX + 162.0F, configY + 35.0F, 14.0, 15.0)) {
               if (this.isDefaultTab) {
                  try {
                     Elixir.getInstance().getFileController().loadFile(config + ".json");
                  } catch (FileLoadException var23) {
                  }
               } else {
                  this.loadCloudConfig(config);
                  this.isDefaultTab = true;
                  this.refreshConfigs();
               }

               return true;
            }

            if (Calculate.isHovered(mouseX, mouseY, configX + 146.0F, configY + 35.0F, 14.0, 15.0)) {
               if (this.isDefaultTab) {
                  try {
                     Elixir.getInstance().getFileController().loadFile(config + ".json");
                  } catch (FileLoadException var24) {
                  }
               } else {
                  this.loadCloudConfig(config);
                  this.isDefaultTab = true;
                  this.refreshConfigs();
               }

               return true;
            }

            if (Calculate.isHovered(mouseX, mouseY, configX + 130.25, configY + 35.0F, 14.0, 15.0)) {
               if (this.isDefaultTab) {
                  String cloudId = (String)map.get("cloud_id");
                  if (cloudId != null) {
                     this.updateFromCloud(config, cloudId);
                  } else {
                     try {
                        Elixir.getInstance().getFileController().saveFile(config + ".json");
                     } catch (FileSaveException var25) {
                     }
                  }
               } else {
                  this.saveCloudConfig(config);
               }

               this.refreshConfigs();
               return true;
            }

            if (Calculate.isHovered(mouseX, mouseY, configX + 114.35, configY + 35.0F, 14.0, 15.0)) {
               if (this.isDefaultTab) {
                  File file = new File(Elixir.getInstance().getClientInfoProvider().configsDir(), config + ".json");
                  file.delete();
               } else {
                  this.removeCloudConfig(config);
               }

               this.refreshConfigs();
               return true;
            }

            index++;
         }
      } else {
         this.editingConfig = null;
         this.editingInput = false;
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      if (MenuScreen.getInstance().getCategory() == ModuleCategory.CONFIGS
         && Calculate.isHovered(mouseX, mouseY, this.x + 43.0F, this.y + 65.0F, this.width - 43.0F - 15.0F, this.height - 70.0F)) {
         this.scroll = (float)(this.scroll + amount * 20.0);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, amount);
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.editingInput) {
         if (keyCode == 257) {
            if (!this.isDefaultTab && this.configInput.length() == 8 && this.configInput.matches("\\d+")) {
               this.loadCloudConfig(this.configInput);
               this.isDefaultTab = true;
            }

            this.configInput = "";
            this.inputCursor = 0;
            this.editingInput = false;
            this.refreshConfigs();
            return true;
         }

         if (keyCode == 256) {
            this.configInput = "";
            this.inputCursor = 0;
            this.editingInput = false;
            return true;
         }

         if (keyCode == 259) {
            if (this.inputCursor > 0) {
               this.configInput = this.configInput.substring(0, this.inputCursor - 1) + this.configInput.substring(this.inputCursor);
               this.inputCursor--;
            }

            return true;
         }

         if (keyCode == 263) {
            if (this.inputCursor > 0) {
               this.inputCursor--;
            }

            return true;
         }

         if (keyCode == 262) {
            if (this.inputCursor < this.configInput.length()) {
               this.inputCursor++;
            }

            return true;
         }

         if (keyCode == 86 && (modifiers & 2) != 0) {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard().trim();
            if (!this.isDefaultTab) {
               clip = clip.replaceAll("\\D", "");
               int avail = 8 - this.configInput.length();
               if (clip.length() > avail) {
                  clip = clip.substring(0, avail);
               }
            } else {
               int avail = 15 - this.configInput.length();
               if (clip.length() > avail) {
                  clip = clip.substring(0, avail);
               }
            }

            this.configInput = this.configInput.substring(0, this.inputCursor) + clip + this.configInput.substring(this.inputCursor);
            this.inputCursor = this.inputCursor + clip.length();
            return true;
         }
      } else if (this.editingConfig != null) {
         if (keyCode == 257) {
            if (this.isDefaultTab) {
               File dir = Elixir.getInstance().getClientInfoProvider().configsDir();
               File oldFile = new File(dir, this.editingConfig + ".json");
               File newFile = new File(dir, this.newName + ".json");
               if (!this.newName.isEmpty() && this.newName.length() <= 15 && oldFile.exists() && (!newFile.exists() || this.newName.equals(this.editingConfig))
                  )
                {
                  oldFile.renameTo(newFile);
               }
            }

            this.editingConfig = null;
            this.refreshConfigs();
            return true;
         }

         if (keyCode == 256) {
            this.editingConfig = null;
            return true;
         }

         if (keyCode == 259) {
            if (this.editCursor > 0) {
               this.newName = this.newName.substring(0, this.editCursor - 1) + this.newName.substring(this.editCursor);
               this.editCursor--;
            }

            return true;
         }

         if (keyCode == 263) {
            if (this.editCursor > 0) {
               this.editCursor--;
            }

            return true;
         }

         if (keyCode == 262) {
            if (this.editCursor < this.newName.length()) {
               this.editCursor++;
            }

            return true;
         }

         if (keyCode == 86 && (modifiers & 2) != 0) {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard().trim();
            int avail = 15 - this.newName.length();
            if (clip.length() > avail) {
               clip = clip.substring(0, avail);
            }

            this.newName = this.newName.substring(0, this.editCursor) + clip + this.newName.substring(this.editCursor);
            this.editCursor = this.editCursor + clip.length();
            return true;
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.editingInput) {
         if (this.isDefaultTab && this.configInput.length() < 15 && Character.isLetterOrDigit(chr)) {
            this.configInput = this.configInput.substring(0, this.inputCursor) + chr + this.configInput.substring(this.inputCursor);
            this.inputCursor++;
            return true;
         }

         if (!this.isDefaultTab && this.configInput.length() < 8 && Character.isDigit(chr)) {
            this.configInput = this.configInput.substring(0, this.inputCursor) + chr + this.configInput.substring(this.inputCursor);
            this.inputCursor++;
            return true;
         }
      } else if (this.editingConfig != null && this.newName.length() < 15 && Character.isLetterOrDigit(chr)) {
         this.newName = this.newName.substring(0, this.editCursor) + chr + this.newName.substring(this.editCursor);
         this.editCursor++;
         return true;
      }

      return super.charTyped(chr, modifiers);
   }

   private void refreshConfigs() {
      if (this.isDefaultTab) {
         this.configs = this.getLocalConfigs();
      } else {
         if (this.cloudLoading) {
            return;
         }

         this.cloudLoading = true;
         this.cloudDataReady = false;
         this.configs = new ArrayList<>();
         this.cloudLoadStartTime = System.currentTimeMillis();
         new Thread(() -> {
            List<Map<String, Object>> cl = this.getCloudConfigs();
            MinecraftClient.getInstance().execute(() -> {
               this.tempConfigs = cl;
               this.cloudDataReady = true;
            });
         }).start();
      }
   }

   private List<Map<String, Object>> getLocalConfigs() {
      ArrayList<Map<String, Object>> localConfigs = new ArrayList<>();
      File dir = Elixir.getInstance().getClientInfoProvider().configsDir();
      File[] configFiles = dir.listFiles();
      if (configFiles != null) {
         for (File configFile : configFiles) {
            if (configFile.isFile() && configFile.getName().endsWith(".json")) {
               String configName = configFile.getName().replace(".json", "");
               long mod = configFile.lastModified();
               HashMap<String, Object> map = new HashMap<>();
               map.put("name", configName);
               map.put("created", mod);
               map.put("updated", mod);
               String jsonContent = "";

               try {
                  jsonContent = new String(Files.readAllBytes(configFile.toPath()));
               } catch (IOException var19) {
               }

               String cloudId;
               if (!jsonContent.isEmpty() && (cloudId = (String)((Map)new Gson().fromJson(jsonContent, Map.class)).get("cloud_id")) != null) {
                  map.put("cloud_id", cloudId);
                  Map<String, Object> metadata = this.getCloudMetadata(cloudId);
                  if (metadata != null) {
                     long serverUpdated = ((Number)metadata.get("updated")).longValue();
                     if (serverUpdated > mod) {
                        map.put("has_update", true);
                     }

                     map.put("owner", metadata.get("owner"));
                     map.put("avatar_hash", metadata.get("avatar_hash"));
                  }
               }

               localConfigs.add(map);
            }
         }
      }

      return localConfigs;
   }

   private List<Map<String, Object>> getCloudConfigs() {
      Gson gson = new Gson();
      HashMap<String, String> request = new HashMap<>();
      request.put("command", "list");
      request.put("username", Elixir.getInstance().getNativeUsername());
      request.put("uuid", Elixir.getInstance().getNativeUserIdentifier());
      String message = gson.toJson(request);
      String response = Elixir.getInstance().getCloudConfigClient().sendAndWaitForResponse(message);
      if (response == null) {
         return new ArrayList<>();
      } else {
         Map respMap = (Map)gson.fromJson(response, Map.class);
         return (List<Map<String, Object>>)(Boolean.TRUE.equals(respMap.get("success")) ? (List)respMap.get("data") : new ArrayList<>());
      }
   }

   private void createDefaultConfig() {
      Random random = new Random();
      File dir = Elixir.getInstance().getClientInfoProvider().configsDir();

      String name;
      while (new File(dir, (name = "ElixirConfig" + String.format("%03d", random.nextInt(1000))) + ".json").exists()) {
      }

      try {
         Elixir.getInstance().getFileController().saveFile(name + ".json");
      } catch (FileSaveException var5) {
      }
   }

   private void createCloudConfig() {
      Random random = new Random();

      String id;
      while (this.getCloudConfigJson(id = String.format("%08d", random.nextInt(100000000))) != null) {
      }

      this.saveCloudConfig(id);
   }

   private void clearAllConfigs() {
      if (!this.isDefaultTab) {
         for (Map<String, Object> map : this.configs) {
            this.removeCloudConfig((String)map.get("name"));
         }
      } else {
         File dir = Elixir.getInstance().getClientInfoProvider().configsDir();
         File[] files = dir.listFiles();
         if (files != null) {
            for (File f : files) {
               if (f.getName().endsWith(".json")) {
                  f.delete();
               }
            }
         }
      }
   }

   private String getCurrentConfigJson() {
      String json = "";
      File dir = Elixir.getInstance().getClientInfoProvider().configsDir();
      File temp = new File(dir, "temp.json");

      try {
         Elixir.getInstance().getFileController().saveFile("temp.json");
         json = new String(Files.readAllBytes(temp.toPath()));
      } catch (IOException | FileSaveException var8) {
      } finally {
         temp.delete();
      }

      return json;
   }

   private String getCloudConfigJson(String name) {
      Gson gson = new Gson();
      HashMap<String, String> request = new HashMap<>();
      request.put("command", "load");
      request.put("username", "username");
      request.put("uuid", "uid");
      request.put("configName", name);
      String message = gson.toJson(request);
      String response = Elixir.getInstance().getCloudConfigClient().sendAndWaitForResponse(message);
      if (response == null) {
         return null;
      } else {
         Map respMap = (Map)gson.fromJson(response, Map.class);
         if ((Boolean)respMap.get("success")) {
            Object data = respMap.get("data");
            return gson.toJson(data);
         } else {
            return null;
         }
      }
   }

   private Map<String, Object> getCloudMetadata(String name) {
      Gson gson = new Gson();
      HashMap<String, String> request = new HashMap<>();
      request.put("command", "metadata");
      request.put("username", "username");
      request.put("uuid", "uid");
      request.put("configName", name);
      String message = gson.toJson(request);
      String response = Elixir.getInstance().getCloudConfigClient().sendAndWaitForResponse(message);
      if (response == null) {
         return null;
      } else {
         Map respMap = (Map)gson.fromJson(response, Map.class);
         return Boolean.TRUE.equals(respMap.get("success")) ? (Map)respMap.get("data") : null;
      }
   }

   private void saveCloudConfig(String name) {
      String json = this.getCurrentConfigJson();
      if (!json.isEmpty()) {
         Gson gson = new Gson();
         LinkedHashMap<String, Object> data = new LinkedHashMap<>();
         long now = System.currentTimeMillis();
         String existing = this.getCloudConfigJson(name);
         long created = now;
         if (existing != null) {
            Map oldData = (Map)gson.fromJson(existing, Map.class);
            created = ((Number)oldData.get("created")).longValue();
         }

         data.put("owner", "username");
         data.put("created", created);
         data.put("updated", now);
         data.put("avatar_hash", "minecraft:textures/gui/default_avatar.png");
         Map configData = (Map)gson.fromJson(json, Map.class);
         data.putAll(configData);
         json = gson.toJson(data);
         this.saveCloudConfigWithJson(name, json);
      }
   }

   private void saveCloudConfigWithJson(String name, String json) {
      if (!json.isEmpty()) {
         Gson gson = new Gson();
         HashMap<String, Object> request = new HashMap<>();
         request.put("command", "save");
         request.put("username", "username");
         request.put("uuid", "uid");
         request.put("configName", name);
         request.put("configData", gson.fromJson(json, Map.class));
         String message = gson.toJson(request);
         Elixir.getInstance().getCloudConfigClient().sendAndWaitForResponse(message);
      }
   }

   private void loadCloudConfig(String name) {
      String json = this.getCloudConfigJson(name);
      if (json != null) {
         Gson gson = new Gson();
         Map data = (Map)gson.fromJson(json, Map.class);
         String owner = (String)data.get("owner");
         long created = ((Number)data.get("created")).longValue();
         long updated = ((Number)data.get("updated")).longValue();
         Object avatarObj = data.get("avatar_hash");
         if (avatarObj instanceof Map idMap) {
            String avatarHash = (String)idMap.get("namespace") + ":" + (String)idMap.get("path");
         } else {
            String avatarHash = String.valueOf(avatarObj);
         }

         data.remove("owner");
         data.remove("created");
         data.remove("updated");
         data.remove("avatar_hash");
         data.put("cloud_id", name);
         json = gson.toJson(data);
         File dir = Elixir.getInstance().getClientInfoProvider().configsDir();
         File temp = new File(dir, "temp.json");

         try {
            Files.write(temp.toPath(), json.getBytes());
            Elixir.getInstance().getFileController().loadFile("temp.json");
         } catch (IOException | FileLoadException var21) {
         } finally {
            temp.delete();
         }

         String baseName;
         String localName = baseName = owner + " Config";
         if (!owner.equals("username")) {
            int num = 1;

            while (new File(dir, localName + ".json").exists()) {
               localName = baseName + " (" + num++ + ")";
            }
         } else {
            localName = "Cloud_" + name;
         }

         try {
            Files.write(new File(dir, localName + ".json").toPath(), json.getBytes());
         } catch (IOException var20) {
         }
      }
   }

   private void updateFromCloud(String localName, String cloudId) {
      String json = this.getCloudConfigJson(cloudId);
      if (json != null) {
         Gson gson = new Gson();
         Map data = (Map)gson.fromJson(json, Map.class);
         data.remove("owner");
         data.remove("created");
         data.remove("updated");
         data.remove("avatar_hash");
         data.put("cloud_id", cloudId);
         json = gson.toJson(data);
         File dir = Elixir.getInstance().getClientInfoProvider().configsDir();
         File temp = new File(dir, "temp.json");

         try {
            Files.write(temp.toPath(), json.getBytes());
            Elixir.getInstance().getFileController().loadFile("temp.json");
            Files.write(new File(dir, localName + ".json").toPath(), json.getBytes());
         } catch (IOException | FileLoadException var12) {
         } finally {
            temp.delete();
         }
      }
   }

   private void removeCloudConfig(String name) {
      Gson gson = new Gson();
      HashMap<String, String> request = new HashMap<>();
      request.put("command", "remove");
      request.put("username", "username");
      request.put("uuid", "uid");
      request.put("configName", name);
      String message = gson.toJson(request);
      Elixir.getInstance().getCloudConfigClient().sendAndWaitForResponse(message);
   }

   public BackgroundComponent setEditingConfig(String editingConfig) {
      this.editingConfig = editingConfig;
      return this;
   }

   public BackgroundComponent setNewName(String newName) {
      this.newName = newName;
      return this;
   }

   public BackgroundComponent setEditCursor(int editCursor) {
      this.editCursor = editCursor;
      return this;
   }

   public BackgroundComponent setDefaultTab(boolean isDefaultTab) {
      this.isDefaultTab = isDefaultTab;
      return this;
   }

   public BackgroundComponent setHighlightX(float highlightX) {
      this.highlightX = highlightX;
      return this;
   }

   public BackgroundComponent setConfigs(List<Map<String, Object>> configs) {
      this.configs = configs;
      return this;
   }

   public BackgroundComponent setConfigInput(String configInput) {
      this.configInput = configInput;
      return this;
   }

   public BackgroundComponent setEditingInput(boolean editingInput) {
      this.editingInput = editingInput;
      return this;
   }

   public BackgroundComponent setInputCursor(int inputCursor) {
      this.inputCursor = inputCursor;
      return this;
   }

   public BackgroundComponent setScroll(float scroll) {
      this.scroll = scroll;
      return this;
   }

   public BackgroundComponent setSmoothedScroll(float smoothedScroll) {
      this.smoothedScroll = smoothedScroll;
      return this;
   }

   public BackgroundComponent setLoadedConfigs(boolean loadedConfigs) {
      this.loadedConfigs = loadedConfigs;
      return this;
   }

   public BackgroundComponent setCloudLoading(boolean cloudLoading) {
      this.cloudLoading = cloudLoading;
      return this;
   }

   public BackgroundComponent setCloudLoadStartTime(long cloudLoadStartTime) {
      this.cloudLoadStartTime = cloudLoadStartTime;
      return this;
   }

   public BackgroundComponent setCloudDataReady(boolean cloudDataReady) {
      this.cloudDataReady = cloudDataReady;
      return this;
   }

   public BackgroundComponent setTempConfigs(List<Map<String, Object>> tempConfigs) {
      this.tempConfigs = tempConfigs;
      return this;
   }

   public BackgroundComponent setLoadingAlpha(float loadingAlpha) {
      this.loadingAlpha = loadingAlpha;
      return this;
   }

   public BackgroundComponent setConfigsAlpha(float configsAlpha) {
      this.configsAlpha = configsAlpha;
      return this;
   }
}
