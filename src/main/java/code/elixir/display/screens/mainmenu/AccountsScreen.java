package code.elixir.display.screens.mainmenu;

import code.elixir.Elixir;
import code.elixir.common.animation.Easy.Direction;
import code.elixir.common.animation.Easy.EaseBackIn;
import code.elixir.mixins.IMinecraftClient;
import code.elixir.utils.client.managers.file.impl.AccountFile;
import code.elixir.utils.client.managers.file.impl.account.Account;
import code.elixir.utils.client.managers.file.impl.account.AccountRepository;
import code.elixir.utils.display.atlasfont.msdf.MsdfFont;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.MainMenuSkyRenderer;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.render.systemrender.builders.Builder;
import code.elixir.utils.display.render.systemrender.builders.states.QuadColorState;
import code.elixir.utils.display.render.systemrender.builders.states.QuadRadiusState;
import code.elixir.utils.display.render.systemrender.builders.states.SizeState;
import code.elixir.utils.display.scissor.ScissorAssist;
import com.google.common.base.Suppliers;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.Session.AccountType;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class AccountsScreen extends Screen implements QuickImports {
   static final float PANEL_WIDTH = 260.0F;
   static final float PANEL_HEIGHT = 340.0F;
   static final float ACCOUNT_SPACING = 30.0F;
   static final int MIN_LENGTH = 3;
   static final int MAX_LENGTH = 16;
   private static final Supplier<MsdfFont> ICONS_FONT_MAIN = Suppliers.memoize(() -> MsdfFont.builder().atlas("iconmain").data("iconmain").build());
   private final Screen parent;
   private final AccountRepository accountRepository = Elixir.getInstance().getAccountRepository();
   private final Random rand = new Random();
   private final EaseBackIn openAnimation = new EaseBackIn(400, 1.0, 1.15F);
   private String currentAccount = "None";
   private boolean typing;
   private String typedText = "";
   private int cursorPos;
   private int selStart = -1;
   private int selEnd = -1;
   private long lastClick;
   private float textXOffset;
   private boolean dragging;
   private float scroll;
   private float smoothedScroll;
   private float parallaxX;
   private float parallaxY;
   private final Color bgColor = new Color(24, 24, 24, 238);
   private final Color fillColor = new Color(42, 42, 42, 255);
   private final Color outlineColor = new Color(220, 220, 220, 140);
   private final Color textColor = new Color(232, 232, 232, 255);

   public AccountsScreen(Screen parent) {
      super(Text.of("Accounts"));
      this.parent = parent;
      this.openAnimation.setDirection(Direction.FORWARDS);
      this.openAnimation.reset();
   }

   static float panelX(int screenWidth) {
      return screenWidth / 2.0F - 130.0F;
   }

   static float panelY(int screenHeight) {
      return screenHeight / 2.0F - 170.0F;
   }

   protected void init() {
      super.init();
      if (mc.getSession() != null) {
         this.currentAccount = mc.getSession().getUsername();
      }
   }

   public void tick() {
      super.tick();

      for (Account account : this.accountRepository.accountList) {
         float target = account.starred ? 1.0F : 0.0F;
         account.starAnim = account.starAnim + (target - account.starAnim) * 0.2F;
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderBackgroundLayer(context, mouseX, mouseY);
      float anim = MathHelper.clamp((float)this.openAnimation.getOutput(), 0.0F, 1.0F);
      float panelX = panelX(this.width);
      float panelY = panelY(this.height);
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, anim);
      this.renderPanel(context, panelX, panelY);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      matrices.pop();
   }

   private void renderBackgroundLayer(DrawContext context, int mouseX, int mouseY) {
      MatrixStack matrices = context.getMatrices();
      MainMenuSkyRenderer.render(context, this.width, this.height);
      rectangle.render(ShapeProperties.create(matrices, -1.0, -1.0, this.width + 2, this.height + 2).color(new Color(0, 0, 0, 140).getRGB()).build());
   }

   private void drawFullscreenBackgroundTexture(MatrixStack matrices, Identifier id, float x, float y, float w, float h, float alpha) {
      int glId = mc.getTextureManager().getTexture(id).getGlId();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, MathHelper.clamp(alpha, 0.0F, 1.0F));
      Builder.texture()
         .size(new SizeState(w, h))
         .radius(QuadRadiusState.NO_ROUND)
         .color(QuadColorState.WHITE)
         .smoothness(1.0F)
         .texture(0.0F, 0.0F, 1.0F, 1.0F, glId)
         .build()
         .render(matrices.peek().getPositionMatrix(), x, y, 0.0F);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderPanel(DrawContext context, float panelX, float panelY) {
      MatrixStack matrices = context.getMatrices();
      rectangle.render(ShapeProperties.create(matrices, panelX + 130.0F - 30.0F, panelY - 18.0F, 60.0, 16.0).color(this.bgColor.getRGB()).round(3.0F).build());
      Fonts.getSize(18, Fonts.Type.DEFAULT).drawCenteredString(matrices, "Accounts", panelX + 130.0F, panelY - 14.0F, this.textColor.getRGB());
      rectangle.render(ShapeProperties.create(matrices, panelX, panelY, 260.0, 340.0).color(this.bgColor.getRGB()).round(8.0F).build());
      float listY = panelY + 10.0F;
      float listHeight = 248.0F;
      float inputY = panelY + 340.0F - 58.0F;
      float inputX = panelX + 10.0F;
      float inputW = 240.0F;
      float textFieldW = inputW - 34.0F;
      this.solidOutlinedRect(matrices, inputX, inputY, textFieldW, 26.0F, 6.0F);
      this.solidOutlinedRect(matrices, inputX + textFieldW + 6.0F, inputY + 3.0F, 22.0F, 22.0F, 5.0F);
      Builder.text()
         .font(ICONS_FONT_MAIN.get())
         .text("D")
         .size(16.0F)
         .color(this.textColor.getRGB())
         .build()
         .render(matrices.peek().getPositionMatrix(), inputX + textFieldW + 10.0F, inputY + 5.0F);
      this.renderTextField(context, inputX + 4.0F, inputY + 5.0F, textFieldW - 8.0F, 22.0F);
      this.solidOutlinedRect(matrices, panelX + 10.0F, panelY + 340.0F - 24.0F, 240.0F, 18.0F, 5.0F);
      Fonts.getSize(16, Fonts.Type.DEFAULT).drawCenteredString(matrices, "Back", panelX + 130.0F, panelY + 340.0F - 19.0F, this.textColor.getRGB());
      Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
      ScissorAssist scissor = Elixir.getInstance().getScissorManager();
      scissor.push(positionMatrix, panelX + 8.0F, listY, 244.0F, listHeight);
      float maxScrollClamp = Math.max(0.0F, this.accountRepository.accountList.size() * 30.0F - listHeight);
      this.scroll = MathHelper.clamp(this.scroll, 0.0F, maxScrollClamp);
      this.smoothedScroll = MathHelper.lerp(0.1F, this.smoothedScroll, this.scroll);

      for (int i = 0; i < this.accountRepository.accountList.size(); i++) {
         Account account = this.accountRepository.accountList.get(i);
         float accY = panelY + 14.0F + i * 30.0F - this.smoothedScroll;
         if (!(accY + 26.0F < listY) && !(accY > listY + listHeight)) {
            this.solidOutlinedRect(matrices, panelX + 10.0F, accY, 240.0F, 26.0F, 6.0F);
            this.solidOutlinedRect(matrices, panelX + 14.0F, accY + 3.0F, 20.0F, 20.0F, 10.0F);
            Color starColor = this.interpolateColor(this.textColor, new Color(255, 255, 0, this.textColor.getAlpha()), account.starAnim);
            Fonts.getSize(28, Fonts.Type.ICONS).drawString(matrices, "S", panelX + 260.0F - 36.0F, accY + 8.0F, starColor.getRGB());
            this.drawAccountFace(context, account, panelX + 15.0F, accY + 4.5F);
            Fonts.getSize(16, Fonts.Type.SEMI).drawString(matrices, account.name, panelX + 40.0F, accY + 10.0F, this.textColor.getRGB());
         }
      }

      scissor.pop();
      if (this.accountRepository.accountList.size() > 7) {
         this.renderScrollbar(matrices, panelX, listY, listHeight);
      }

      String currentText = "Current account - " + this.currentAccount;
      FontRenderer font = Fonts.getSize(16, Fonts.Type.SEMI);
      float currentWidth = font.getStringWidth(currentText) + 24.0F;
      float currentX = panelX + 130.0F - currentWidth / 2.0F;
      rectangle.render(ShapeProperties.create(matrices, currentX, panelY + 340.0F + 8.0F, currentWidth, 16.0).color(this.bgColor.getRGB()).round(3.0F).build());
      font.drawCenteredString(matrices, currentText, panelX + 130.0F, panelY + 340.0F + 12.0F, this.textColor.getRGB());
   }

   private void solidOutlinedRect(MatrixStack matrices, float x, float y, float w, float h, float round) {
      rectangle.render(
         ShapeProperties.create(matrices, x, y, w, h)
            .thickness(2.0F)
            .round(round)
            .outlineColor(this.outlineColor.getRGB())
            .color(this.fillColor.getRGB())
            .build()
      );
   }

   private void renderTextField(DrawContext context, float textFieldX, float textFieldY, float textFieldWidth, float textFieldHeight) {
      FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
      boolean blink = System.currentTimeMillis() % 1000L < 500L;
      context.enableScissor((int)textFieldX, (int)textFieldY, (int)(textFieldX + textFieldWidth), (int)(textFieldY + textFieldHeight));
      if (this.typing && this.hasSelection()) {
         int start = Math.min(this.selStart, this.selEnd);
         int end = Math.max(this.selStart, this.selEnd);
         float selXStart = textFieldX + 2.0F - this.textXOffset + font.getStringWidth(this.typedText.substring(0, start));
         float selWidth = font.getStringWidth(this.typedText.substring(start, end));
         rectangle.render(
            ShapeProperties.create(context.getMatrices(), selXStart, textFieldY + 1.0F, selWidth, textFieldHeight - 2.0F).color(-11172376).build()
         );
      }

      if (this.typedText.isEmpty() && !this.typing) {
         font.drawString(context.getMatrices(), "Typing nickname", textFieldX + 2.0F, textFieldY + 6.0F, this.textColor.getRGB());
      } else {
         font.drawString(context.getMatrices(), this.typedText, textFieldX + 2.0F - this.textXOffset, textFieldY + 6.0F, this.textColor.getRGB());
      }

      if (this.typing && blink && !this.hasSelection()) {
         float cursorX = textFieldX + 2.0F - this.textXOffset + font.getStringWidth(this.typedText.substring(0, this.cursorPos));
         rectangle.render(
            ShapeProperties.create(context.getMatrices(), cursorX, textFieldY + 3.0F, 0.5, textFieldHeight - 6.0F).color(this.textColor.getRGB()).build()
         );
      }

      context.disableScissor();
   }

   private void renderScrollbar(MatrixStack matrices, float panelX, float listY, float listHeight) {
      float contentHeight = this.accountRepository.accountList.size() * 30.0F;
      float maxScroll = Math.max(0.0F, contentHeight - listHeight);
      this.scroll = MathHelper.clamp(this.scroll, 0.0F, maxScroll);
      float scrollbarWidth = 3.0F;
      float scrollbarX = panelX + 260.0F - scrollbarWidth - 12.0F;
      float scrollbarY = listY + 1.0F;
      float scrollbarHeight = listHeight - 2.0F;
      rectangle.render(
         ShapeProperties.create(matrices, scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight)
            .round(1.0F)
            .color(new Color(30, 30, 30, 100).getRGB())
            .build()
      );
      float handleHeight = Math.max(24.0F, listHeight * (listHeight / (contentHeight + listHeight)));
      float scrollRatio = maxScroll > 0.0F ? this.smoothedScroll / maxScroll : 0.0F;
      float handleY = scrollbarY + (scrollbarHeight - handleHeight) * scrollRatio;
      rectangle.render(
         ShapeProperties.create(matrices, scrollbarX, handleY, scrollbarWidth, handleHeight).round(1.0F).color(new Color(100, 100, 100, 150).getRGB()).build()
      );
   }

   private void drawAccountFace(DrawContext context, Account account, float x, float y) {
      GameProfile profile = new GameProfile(UUID.fromString(account.uuid), account.name);
      Identifier skinTexture = MinecraftClient.getInstance().getSkinProvider().getSkinTextures(profile).texture();
      if (skinTexture == null) {
         skinTexture = Identifier.of("minecraft", "textures/entity/steve.png");
      }

      Render2D.drawTexture(context, skinTexture, x, y, 18.0F, 8.0F, 8, 8, 64, ColorAssist.getRect(1.0F), -1);
   }

   private Color interpolateColor(Color start, Color end, float t) {
      int r = (int)(start.getRed() + (end.getRed() - start.getRed()) * t);
      int g = (int)(start.getGreen() + (end.getGreen() - start.getGreen()) * t);
      int b = (int)(start.getBlue() + (end.getBlue() - start.getBlue()) * t);
      int a = (int)(start.getAlpha() + (end.getAlpha() - start.getAlpha()) * t);
      return new Color(r, g, b, a);
   }

   private boolean isHover(double mouseX, double mouseY, float x, float y, float w, float h) {
      return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.openAnimation.getOutput() < 0.95F) {
         return true;
      } else {
         float panelX = panelX(this.width);
         float panelY = panelY(this.height);
         float inputY = panelY + 340.0F - 58.0F;
         float inputX = panelX + 10.0F;
         float inputW = 240.0F;
         float textFieldW = inputW - 34.0F;
         if (button == 0 && this.isHover(mouseX, mouseY, panelX + 10.0F, panelY + 340.0F - 24.0F, 240.0F, 18.0F)) {
            mc.setScreen(this.parent);
            return true;
         } else if (button == 0 && this.isHover(mouseX, mouseY, inputX + 4.0F, inputY + 5.0F, textFieldW - 8.0F, 22.0F)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastClick < 250L) {
               this.selStart = 0;
               this.selEnd = this.typedText.length();
            } else {
               this.typing = true;
               this.selStart = this.cursorPos = this.getCursorIndexAt(mouseX);
               this.selEnd = this.cursorPos;
               this.lastClick = currentTime;
            }

            this.dragging = true;
            return true;
         } else {
            this.typing = false;
            this.clearSelection();
            if (button == 0 && this.isHover(mouseX, mouseY, inputX + textFieldW + 6.0F, inputY + 3.0F, 22.0F, 22.0F)) {
               this.addRandomAccount();
               return true;
            } else {
               float listY = panelY + 10.0F;
               float listHeight = 248.0F;

               for (int i = 0; i < this.accountRepository.accountList.size(); i++) {
                  float accY = panelY + 14.0F + i * 30.0F - this.smoothedScroll;
                  if (!(accY + 26.0F < listY) && !(accY > listY + listHeight)) {
                     float starX = panelX + 260.0F - 36.0F;
                     if (button == 0 && this.isHover(mouseX, mouseY, starX, accY + 8.0F, 18.0F, 18.0F)) {
                        Account account = this.accountRepository.accountList.get(i);
                        account.starred = !account.starred;
                        this.accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));
                        this.saveAccounts();
                        return true;
                     }

                     if (this.isHover(mouseX, mouseY, panelX + 10.0F, accY, 240.0F, 26.0F)) {
                        if (button == 0) {
                           this.selectAccount(this.accountRepository.accountList.get(i));
                        } else if (button == 1) {
                           this.accountRepository.accountList.remove(i);
                           this.saveAccounts();
                        }

                        return true;
                     }
                  }
               }

               return super.mouseClicked(mouseX, mouseY, button);
            }
         }
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      float panelX = panelX(this.width);
      float panelY = panelY(this.height);
      float listY = panelY + 10.0F;
      float listHeight = 248.0F;
      float contentHeight = this.accountRepository.accountList.size() * 30.0F;
      float maxScroll = Math.max(0.0F, contentHeight - listHeight);
      if (maxScroll > 0.0F && this.isHover(mouseX, mouseY, panelX + 8.0F, listY, 244.0F, listHeight)) {
         this.scroll = (float)(this.scroll - verticalAmount * 24.0);
         this.scroll = MathHelper.clamp(this.scroll, 0.0F, maxScroll);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.dragging && button == 0) {
         this.selEnd = this.cursorPos = this.getCursorIndexAt(mouseX);
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.dragging = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean charTyped(char chr, int modifiers) {
      if (this.typing && this.typedText.length() < 16) {
         this.deleteSelectedText();
         this.typedText = this.typedText.substring(0, this.cursorPos) + chr + this.typedText.substring(this.cursorPos);
         this.cursorPos++;
         this.clearSelection();
         this.updateTextXOffset();
         return true;
      } else {
         return super.charTyped(chr, modifiers);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.typing) {
         if (Screen.hasControlDown()) {
            switch (keyCode) {
               case 65:
                  this.selStart = 0;
                  this.selEnd = this.typedText.length();
                  return true;
               case 67:
                  if (this.hasSelection()) {
                     GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), this.getSelectedText());
                  }

                  return true;
               case 86:
                  String clipboard = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
                  if (clipboard != null) {
                     this.deleteSelectedText();
                     this.typedText = this.typedText.substring(0, this.cursorPos) + clipboard + this.typedText.substring(this.cursorPos);
                     this.cursorPos = this.cursorPos + clipboard.length();
                     this.clearSelection();
                     this.updateTextXOffset();
                  }

                  return true;
            }
         }

         switch (keyCode) {
            case 257:
               if (this.typedText.length() >= 3 && this.typedText.length() <= 16) {
                  this.addAccount(this.typedText);
               }

               return true;
            case 258:
            case 260:
            case 261:
            default:
               break;
            case 259:
               if (this.hasSelection()) {
                  this.deleteSelectedText();
               } else if (this.cursorPos > 0) {
                  this.typedText = this.typedText.substring(0, this.cursorPos - 1) + this.typedText.substring(this.cursorPos);
                  this.cursorPos--;
               }

               this.updateTextXOffset();
               return true;
            case 262:
               if (this.cursorPos < this.typedText.length()) {
                  this.cursorPos++;
               }

               this.updateSelectionAfterMove();
               this.updateTextXOffset();
               return true;
            case 263:
               if (this.cursorPos > 0) {
                  this.cursorPos--;
               }

               this.updateSelectionAfterMove();
               this.updateTextXOffset();
               return true;
         }
      }

      if (keyCode == 256) {
         mc.setScreen(this.parent);
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   private void addAccount(String name) {
      String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)).toString();
      this.accountRepository.accountList.add(new Account(name, false, false, null, offlineUuid, "0"));
      this.accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));
      this.saveAccounts();
      this.typedText = "";
      this.cursorPos = 0;
      this.typing = false;
      this.clearSelection();
   }

   private void addRandomAccount() {
      String finalUsername = null;
      StringBuilder username = new StringBuilder();
      char[] vowels = new char[]{'a', 'e', 'i', 'o', 'u'};
      char[] consonants = new char[]{'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z'};
      int attempts = 0;

      do {
         username.setLength(0);
         int length = 6 + this.rand.nextInt(5);
         boolean startWithVowel = this.rand.nextBoolean();

         for (int i = 0; i < length; i++) {
            if (i % 2 == 0) {
               username.append(startWithVowel ? vowels[this.rand.nextInt(vowels.length)] : consonants[this.rand.nextInt(consonants.length)]);
            } else {
               username.append(startWithVowel ? consonants[this.rand.nextInt(consonants.length)] : vowels[this.rand.nextInt(vowels.length)]);
            }
         }

         if (this.rand.nextInt(100) < 30) {
            username.append(this.rand.nextInt(100));
         }

         String tempUsername = username.substring(0, 1).toUpperCase() + username.substring(1);
         attempts++;
         if (this.accountRepository.accountList.stream().noneMatch(account -> account.name.equals(tempUsername))) {
            finalUsername = tempUsername;
            break;
         }
      } while (attempts < 10);

      if (finalUsername == null) {
         finalUsername = username.substring(0, 1).toUpperCase() + username.substring(1) + System.currentTimeMillis() % 1000L;
      }

      this.addAccount(finalUsername);
   }

   private void selectAccount(Account account) {
      for (Account other : this.accountRepository.accountList) {
         other.active = other == account;
      }

      this.currentAccount = account.name;
      this.setSession(account);
      this.saveAccounts();
   }

   private void saveAccounts() {
      try {
         Elixir.getInstance().getFileController().saveFile(AccountFile.class);
      } catch (Exception var2) {
      }
   }

   private void setSession(Account account) {
      Session newSession = new Session(account.name, UUID.fromString(account.uuid), "0", Optional.empty(), Optional.empty(), AccountType.MOJANG);
      IMinecraftClient mca = (IMinecraftClient)MinecraftClient.getInstance();
      mca.setSessionT(newSession);
      MinecraftClient.getInstance().getGameProfile().getProperties().clear();
      UserApiService apiService = UserApiService.OFFLINE;
      mca.setUserApiService(apiService);
      mca.setSocialInteractionsManagerT(new SocialInteractionsManager(MinecraftClient.getInstance(), apiService));
      mca.setProfileKeys(ProfileKeys.create(apiService, newSession, MinecraftClient.getInstance().runDirectory.toPath()));
      mca.setAbuseReportContextT(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), apiService));
   }

   private boolean hasSelection() {
      return this.selStart != this.selEnd;
   }

   private String getSelectedText() {
      int start = Math.min(this.selStart, this.selEnd);
      int end = Math.max(this.selStart, this.selEnd);
      return this.typedText.substring(start, end);
   }

   private void deleteSelectedText() {
      if (this.hasSelection()) {
         int start = Math.min(this.selStart, this.selEnd);
         int end = Math.max(this.selStart, this.selEnd);
         this.typedText = this.typedText.substring(0, start) + this.typedText.substring(end);
         this.cursorPos = start;
         this.clearSelection();
      }
   }

   private void clearSelection() {
      this.selStart = this.cursorPos;
      this.selEnd = this.cursorPos;
   }

   private void updateSelectionAfterMove() {
      if (Screen.hasShiftDown()) {
         this.selEnd = this.cursorPos;
      } else {
         this.clearSelection();
      }
   }

   private int getCursorIndexAt(double mouseX) {
      float textFieldX = panelX(this.width) + 14.0F;
      FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
      float relX = (float)mouseX - textFieldX - 2.0F + this.textXOffset;
      int pos = 0;

      while (pos < this.typedText.length() && !(font.getStringWidth(this.typedText.substring(0, pos + 1)) > relX)) {
         pos++;
      }

      return pos;
   }

   private void updateTextXOffset() {
      float textFieldWidth = 190.0F;
      FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
      float cursorX = font.getStringWidth(this.typedText.substring(0, this.cursorPos));
      if (cursorX < this.textXOffset) {
         this.textXOffset = cursorX;
      } else if (cursorX > this.textXOffset + textFieldWidth - 10.0F) {
         this.textXOffset = cursorX - (textFieldWidth - 10.0F);
      }
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean shouldPause() {
      return false;
   }
}
