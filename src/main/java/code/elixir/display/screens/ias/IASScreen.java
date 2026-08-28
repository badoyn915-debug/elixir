package code.elixir.display.screens.ias;

import code.elixir.Elixir;
import code.elixir.mixins.IMinecraftClient;
import code.elixir.utils.client.managers.file.impl.account.Account;
import code.elixir.utils.client.managers.file.impl.account.AccountRepository;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.scissor.ScissorAssist;
import code.elixir.utils.theme.ThemeManager;
import com.mojang.authlib.minecraft.UserApiService;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.Session.AccountType;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class IASScreen extends Screen implements QuickImports {
   public static IASScreen INSTANCE = new IASScreen();
   private AccountRepository accountRepository = Elixir.getInstance().getAccountRepository();
   private float scroll = 0.0F;
   private float smoothedScroll = 0.0F;
   private int selectedIndex = -1;
   private static final int ITEM_HEIGHT = 25;
   private static final int PADDING = 10;
   private static final int SCROLLBAR_WIDTH = 5;
   private boolean typing = false;
   private String typedText = "";
   private int cursorPos = 0;
   private int selStart = -1;
   private int selEnd = -1;
   private float textXOffset = 0.0F;
   private boolean editingAccount = false;
   private int editingIndex = -1;
   private static final int MIN_LENGTH = 3;
   private static final int MAX_LENGTH = 16;
   private long lastClickTime = 0L;
   private int lastSelectedIndex = -1;

   public IASScreen() {
      super(Text.of("Account Switcher"));
   }

   protected void init() {
      super.init();
      if (this.accountRepository == null) {
         this.accountRepository = Elixir.getInstance().getAccountRepository();
      }

      this.addDrawableChild(ButtonWidget.builder(Text.literal("Add Account"), button -> {
         if (!this.typing) {
            this.typing = true;
            this.editingAccount = false;
            this.editingIndex = -1;
            this.typedText = "";
            this.cursorPos = 0;
            this.clearSelection();
         } else {
            this.addNewAccount();
         }
      }).dimensions(this.width / 2 - 200, this.height - 30, 95, 20).build());
      this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), button -> {
         if (this.selectedIndex >= 0 && this.selectedIndex < this.accountRepository.accountList.size()) {
            this.startEditing(this.selectedIndex);
         }
      }).dimensions(this.width / 2 - 100, this.height - 30, 95, 20).build());
      this.addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), button -> {
         if (this.selectedIndex >= 0 && this.selectedIndex < this.accountRepository.accountList.size()) {
            this.accountRepository.accountList.remove(this.selectedIndex);
            this.selectedIndex = -1;
            this.saveAccounts();
         }
      }).dimensions(this.width / 2, this.height - 30, 95, 20).build());
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Close"), button -> this.close()).dimensions(this.width / 2 + 100, this.height - 30, 95, 20).build()
      );
   }

   private void addNewAccount() {
      if (this.typedText != null && !this.typedText.trim().isEmpty()) {
         String username = this.typedText.trim();
         if (username.length() >= 3
            && username.length() <= 16
            && !this.accountRepository.accountList.stream().anyMatch(account -> account.name.equals(username))) {
            String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString();
            this.accountRepository.accountList.add(new Account(username, false, false, null, offlineUuid, "0"));
            this.accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));
            this.typedText = "";
            this.cursorPos = 0;
            this.typing = false;
            this.clearSelection();
            this.saveAccounts();
         }
      } else {
         this.typing = true;
         this.editingAccount = false;
         this.editingIndex = -1;
      }
   }

   private void startEditing(int index) {
      this.editingAccount = true;
      this.editingIndex = index;
      Account account = this.accountRepository.accountList.get(index);
      this.typedText = account.name;
      this.cursorPos = this.typedText.length();
      this.selStart = -1;
      this.selEnd = -1;
   }

   private void finishEditing() {
      if (this.editingAccount && this.editingIndex >= 0 && this.editingIndex < this.accountRepository.accountList.size()) {
         if (this.typedText.length() >= 3 && this.typedText.length() <= 16) {
            String newName = this.typedText.trim();
            Account account = this.accountRepository.accountList.get(this.editingIndex);
            if (!newName.equals(account.name) && !this.accountRepository.accountList.stream().anyMatch(a -> a.name.equals(newName) && a != account)) {
               account.name = newName;
               account.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + newName).getBytes(StandardCharsets.UTF_8)).toString();
               this.saveAccounts();
            }
         }

         this.editingAccount = false;
         this.editingIndex = -1;
         this.typedText = "";
         this.cursorPos = 0;
         this.clearSelection();
      }
   }

   private void clearSelection() {
      this.selStart = -1;
      this.selEnd = -1;
   }

   private boolean hasSelection() {
      return this.selStart >= 0 && this.selEnd >= 0 && this.selStart != this.selEnd;
   }

   private void saveAccounts() {
      try {
         Elixir.getInstance().getFileController().saveFiles();
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderBackground(context, mouseX, mouseY, delta);
      if (this.accountRepository == null) {
         this.accountRepository = Elixir.getInstance().getAccountRepository();
      }

      if (this.accountRepository != null && this.accountRepository.accountList != null) {
         int x = this.width / 2 - 200;
         int y = this.height / 2 - 150;
         int width = 400;
         int height = 300;
         MatrixStack matrix = context.getMatrices();
         rectangle.render(
            ShapeProperties.create(matrix, x, y, width, height)
               .round(8.0F)
               .softness(2.0F)
               .outlineColor(new Color(33, 33, 33, 255).getRGB())
               .color(ThemeManager.BackgroundGui.getColor())
               .build()
         );
         Fonts.getSize(18, Fonts.Type.SuisseIntlMedium).drawString(matrix, "Account Switcher", x + 10, y + 10, ThemeManager.textColor.getColor());
         float textFieldX = x + 10;
         float textFieldY = y + height - 50;
         float textFieldWidth = width - 20;
         float textFieldHeight = 20.0F;
         rectangle.render(
            ShapeProperties.create(matrix, textFieldX, textFieldY, textFieldWidth, textFieldHeight)
               .round(5.0F)
               .thickness(2.0F)
               .outlineColor(new Color(100, 100, 100, 95).getRGB())
               .color(ThemeManager.BackgroundSettings.getColor())
               .build()
         );
         Fonts.Type fontType = Fonts.Type.DEFAULT;
         FontRenderer font = Fonts.getSize(16, fontType);
         long currentTime = System.currentTimeMillis();
         boolean blink = currentTime % 1000L < 500L;
         context.enableScissor((int)(textFieldX + 3.0F), (int)textFieldY, (int)(textFieldX + textFieldWidth - 3.0F), (int)(textFieldY + textFieldHeight) + 5);
         if (this.typing && this.hasSelection()) {
            int start = Math.min(this.selStart, this.selEnd);
            int end = Math.max(this.selStart, this.selEnd);
            float selXStart = textFieldX + 5.0F - this.textXOffset + font.getStringWidth(this.typedText.substring(0, start));
            float selWidth = font.getStringWidth(this.typedText.substring(start, end));
            rectangle.render(ShapeProperties.create(matrix, selXStart, textFieldY + 13.5F, selWidth, textFieldHeight - 10.0F).color(-11172376).build());
         }

         if (this.typedText.isEmpty() && !this.typing) {
            String placeholder = this.editingAccount ? "Editing account..." : "Type nickname to add account";
            font.drawString(matrix, placeholder, textFieldX + 5.0F, textFieldY + 16.0F, new Color(150, 150, 150, 255).getRGB());
         } else {
            font.drawString(matrix, this.typedText, textFieldX + 5.0F - this.textXOffset, textFieldY + 16.0F, ThemeManager.textColor.getColor());
         }

         if (this.typing && blink && !this.hasSelection()) {
            float cursorX = textFieldX + 5.0F - this.textXOffset + font.getStringWidth(this.typedText.substring(0, this.cursorPos));
            rectangle.render(
               ShapeProperties.create(matrix, cursorX + 1.0F, textFieldY + 15.0F, 0.5, textFieldHeight - 13.0F)
                  .color(ThemeManager.textColor.getColor())
                  .build()
            );
         }

         context.disableScissor();
         if (this.accountRepository.accountList.isEmpty()) {
            Fonts.getSize(14, Fonts.Type.SuisseIntlMedium)
               .drawString(matrix, "No accounts found. Type nickname above to add.", x + 10, y + 60, new Color(150, 150, 150, 255).getRGB());
            super.render(context, mouseX, mouseY, delta);
         } else {
            float listX = x + 10;
            float listY = y + 35;
            float listWidth = width - 20 - 5 - 5;
            float listHeight = height - 100;
            ScissorAssist scissorManager = Elixir.getInstance().getScissorManager();
            scissorManager.push(matrix.peek().getPositionMatrix(), listX, listY, listWidth, listHeight);
            this.smoothedScroll = MathHelper.lerp(0.1F, this.smoothedScroll, this.scroll);
            int startIndex = Math.max(0, (int)(this.smoothedScroll / 25.0F));
            int endIndex = Math.min(this.accountRepository.accountList.size(), startIndex + (int)(listHeight / 25.0F) + 1);

            for (int i = startIndex; i < endIndex && i < this.accountRepository.accountList.size(); i++) {
               Account account = this.accountRepository.accountList.get(i);
               float itemY = listY + i * 25 - this.smoothedScroll;
               if (!(itemY + 25.0F < listY) && !(itemY > listY + listHeight)) {
                  boolean isHovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= itemY && mouseY <= itemY + 25.0F;
                  boolean isSelected = i == this.selectedIndex;
                  boolean isEditing = this.editingAccount && this.editingIndex == i;
                  int bgColor = !isHovered && !isSelected
                     ? ThemeManager.BackgroundSettings.getColor()
                     : new Color(ThemeManager.BackgroundSettings.getColor()).darker().getRGB();
                  if (isEditing) {
                     bgColor = new Color(100, 150, 255, 100).getRGB();
                  }

                  rectangle.render(ShapeProperties.create(matrix, listX, itemY, listWidth, 25.0).round(4.0F).color(bgColor).build());
                  if (account.starred) {
                     Fonts.getSize(20, Fonts.Type.ICONS)
                        .drawString(matrix, "S", listX + listWidth - 25.0F, itemY + 12.0F - 8.0F, new Color(255, 255, 0, 255).getRGB());
                  }

                  Object displayName = account.name;
                  if (account.starred) {
                     displayName = "★ " + (String)displayName;
                  }

                  Fonts.getSize(14, Fonts.Type.SuisseIntlMedium)
                     .drawString(matrix, (String)displayName, listX + 5.0F, itemY + 12.0F - 5.0F, ThemeManager.textColor.getColor());
               }
            }

            scissorManager.pop();
            int visibleItems = (int)(listHeight / 25.0F);
            if (this.accountRepository.accountList.size() > visibleItems) {
               float scrollbarX = x + width - 10 - 5;
               float scrollbarThumbHeight = listHeight * ((float)visibleItems / this.accountRepository.accountList.size());
               float scrollbarThumbY = listY
                  + (listHeight - scrollbarThumbHeight) * (this.smoothedScroll / Math.max(1, (this.accountRepository.accountList.size() - visibleItems) * 25));
               rectangle.render(
                  ShapeProperties.create(matrix, scrollbarX, listY, 5.0, listHeight).round(2.0F).color(new Color(50, 50, 50, 255).getRGB()).build()
               );
               rectangle.render(
                  ShapeProperties.create(matrix, scrollbarX, scrollbarThumbY, 5.0, scrollbarThumbHeight)
                     .round(2.0F)
                     .color(new Color(100, 100, 100, 255).getRGB())
                     .build()
               );
            }

            super.render(context, mouseX, mouseY, delta);
         }
      } else {
         super.render(context, mouseX, mouseY, delta);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.accountRepository != null && this.accountRepository.accountList != null) {
         float listX = this.width / 2 - 200 + 10;
         float listY = this.height / 2 - 150 + 35;
         float listWidth = 370.0F;
         float listHeight = 200.0F;
         int visibleItems = (int)(listHeight / 25.0F);
         int startIndex = Math.max(0, (int)(this.smoothedScroll / 25.0F));
         float textFieldX = this.width / 2 - 200 + 10;
         float textFieldY = this.height / 2 - 150 + 300 - 50;
         float textFieldWidth = 380.0F;
         float textFieldHeight = 20.0F;
         if (button == 0 && mouseX >= textFieldX && mouseX <= textFieldX + textFieldWidth && mouseY >= textFieldY && mouseY <= textFieldY + textFieldHeight) {
            this.typing = true;
            this.selStart = this.cursorPos = this.getCursorIndexAt((float)mouseX);
            this.selEnd = this.cursorPos;
            return true;
         } else {
            if (button == 0) {
               this.typing = false;
               if (!this.editingAccount) {
                  this.clearSelection();
               }
            }

            for (int i = startIndex; i < Math.min(this.accountRepository.accountList.size(), startIndex + visibleItems + 1); i++) {
               float itemY = listY + i * 25 - this.smoothedScroll;
               float starX = listX + listWidth - 25.0F;
               float starY = itemY + 12.0F - 8.0F;
               float starSize = 20.0F;
               if (button == 0 && mouseX >= starX && mouseX <= starX + starSize && mouseY >= starY && mouseY <= starY + starSize) {
                  Account account = this.accountRepository.accountList.get(i);
                  account.starred = !account.starred;
                  this.accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));
                  this.saveAccounts();
                  return true;
               }

               if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= itemY && mouseY <= itemY + 25.0F) {
                  this.selectedIndex = i;
                  if (button == 0) {
                     long currentTime = System.currentTimeMillis();
                     if (currentTime - this.lastClickTime < 300L && this.lastSelectedIndex == i) {
                        this.switchAccount(this.accountRepository.accountList.get(i));
                     }

                     this.lastClickTime = currentTime;
                     this.lastSelectedIndex = i;
                  } else if (button == 1) {
                     this.accountRepository.accountList.remove(i);
                     this.selectedIndex = -1;
                     this.saveAccounts();
                  }

                  return true;
               }
            }

            return super.mouseClicked(mouseX, mouseY, button);
         }
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private int getCursorIndexAt(float mouseX) {
      float textFieldX = this.width / 2 - 200 + 10;
      FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
      float textStartX = textFieldX + 5.0F - this.textXOffset;
      float relativeX = mouseX - textStartX;
      if (relativeX < 0.0F) {
         return 0;
      } else if (relativeX > font.getStringWidth(this.typedText)) {
         return this.typedText.length();
      } else {
         for (int i = 0; i <= this.typedText.length(); i++) {
            float charX = font.getStringWidth(this.typedText.substring(0, i));
            if (relativeX <= charX + font.getStringWidth(this.typedText.substring(i, Math.min(i + 1, this.typedText.length()))) / 2.0F) {
               return i;
            }
         }

         return this.typedText.length();
      }
   }

   private void switchAccount(Account account) {
      for (Account other : this.accountRepository.accountList) {
         other.active = other == account;
      }

      this.saveAccounts();
      applyAccountSession(account);
      this.close();
   }

   public static void restoreActiveAccount() {
      try {
         Elixir instance = Elixir.getInstance();
         if (instance == null) {
            return;
         }

         AccountRepository repository = instance.getAccountRepository();
         if (repository == null || repository.accountList == null) {
            return;
         }

         for (Account account : repository.accountList) {
            if (account != null && account.active && account.uuid != null && account.name != null) {
               applyAccountSession(account);
               break;
            }
         }
      } catch (Exception var4) {
      }
   }

   public static void applyAccountSession(Account account) {
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

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.accountRepository == null) {
         return false;
      } else {
         float listY = this.height / 2 - 150 + 35;
         float listHeight = 200.0F;
         int maxScroll = Math.max(0, this.accountRepository.accountList.size() * 25 - (int)listHeight);
         this.scroll = MathHelper.clamp(this.scroll - (float)verticalAmount * 10.0F, 0.0F, maxScroll);
         return true;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         if (this.editingAccount) {
            this.finishEditing();
            return true;
         } else {
            this.close();
            return true;
         }
      } else {
         if (this.typing) {
            if (keyCode == 257 || keyCode == 335) {
               if (this.editingAccount) {
                  this.finishEditing();
               } else {
                  this.addNewAccount();
               }

               return true;
            }

            if (keyCode == 259) {
               if (this.hasSelection()) {
                  int start = Math.min(this.selStart, this.selEnd);
                  int end = Math.max(this.selStart, this.selEnd);
                  this.typedText = this.typedText.substring(0, start) + this.typedText.substring(end);
                  this.cursorPos = start;
                  this.clearSelection();
               } else if (this.cursorPos > 0) {
                  this.typedText = this.typedText.substring(0, this.cursorPos - 1) + this.typedText.substring(this.cursorPos);
                  this.cursorPos--;
               }

               return true;
            }

            if (keyCode == 261) {
               if (this.hasSelection()) {
                  int start = Math.min(this.selStart, this.selEnd);
                  int end = Math.max(this.selStart, this.selEnd);
                  this.typedText = this.typedText.substring(0, start) + this.typedText.substring(end);
                  this.cursorPos = start;
                  this.clearSelection();
               } else if (this.cursorPos < this.typedText.length()) {
                  this.typedText = this.typedText.substring(0, this.cursorPos) + this.typedText.substring(this.cursorPos + 1);
               }

               return true;
            }

            if (keyCode == 263) {
               if (this.cursorPos > 0) {
                  this.cursorPos--;
               }

               if ((modifiers & 1) == 0) {
                  this.clearSelection();
               }

               return true;
            }

            if (keyCode == 262) {
               if (this.cursorPos < this.typedText.length()) {
                  this.cursorPos++;
               }

               if ((modifiers & 1) == 0) {
                  this.clearSelection();
               }

               return true;
            }
         }

         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public boolean charTyped(char chr, int modifiers) {
      if (this.typing && this.typedText.length() < 16) {
         if (this.hasSelection()) {
            int start = Math.min(this.selStart, this.selEnd);
            int end = Math.max(this.selStart, this.selEnd);
            this.typedText = this.typedText.substring(0, start) + chr + this.typedText.substring(end);
            this.cursorPos = start + 1;
            this.clearSelection();
         } else {
            this.typedText = this.typedText.substring(0, this.cursorPos) + chr + this.typedText.substring(this.cursorPos);
            this.cursorPos++;
         }

         return true;
      } else {
         return super.charTyped(chr, modifiers);
      }
   }
}
