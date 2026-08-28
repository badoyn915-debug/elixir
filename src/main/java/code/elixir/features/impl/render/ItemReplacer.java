package code.elixir.features.impl.render;

import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.utils.client.Instance;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ItemReplacer extends Module {
   private static volatile ItemReplacer.SwordVariant[] swordVariants;
   private static volatile Map<String, ItemReplacer.SwordVariant> variantsByLabel;
   private static ItemReplacer instance;
   private static final ThreadLocal<ItemStack> PENDING_STACK = new ThreadLocal<>();
   private static final ThreadLocal<ModelTransformationMode> PENDING_MODE = new ThreadLocal<>();
   private static final ThreadLocal<String> PENDING_VARIANT = new ThreadLocal<>();
   private final MultiSelectSetting swordTypes;
   private final SelectSetting diamondSwordModel;
   private final SelectSetting netheriteSwordModel;
   private final SelectSetting diamondAxeModel;
   private final SelectSetting netheriteAxeModel;
   private String cachedSelectedLabel;
   private ItemReplacer.SwordVariant cachedSelectedVariant;

   private static void clearPending() {
      PENDING_STACK.remove();
      PENDING_MODE.remove();
      PENDING_VARIANT.remove();
   }

   public static ItemReplacer getInstance() {
      return Instance.get(ItemReplacer.class);
   }

   public ItemReplacer() {
      super("ItemReplacer", "Заменки на Меч", ModuleCategory.RENDER);
      ItemReplacer.SwordVariant[] variants = swordVariants();
      this.cachedSelectedVariant = variants[0];
      this.swordTypes = new MultiSelectSetting("Заменять", "Какие предметы заменять")
         .value("Алмазный меч", "Незеритовый меч", "Алмазный топор", "Незеритовый топор")
         .selected("Алмазный меч", "Незеритовый меч");
      this.diamondSwordModel = this.createModelSetting("Модель алм. меча", "Алмазный меч", variants);
      this.netheriteSwordModel = this.createModelSetting("Модель нез. меча", "Незеритовый меч", variants);
      this.diamondAxeModel = this.createModelSetting("Модель алм. топора", "Алмазный топор", variants);
      this.netheriteAxeModel = this.createModelSetting("Модель нез. топора", "Незеритовый топор", variants);
      this.setup(new Setting[]{this.swordTypes, this.diamondSwordModel, this.netheriteSwordModel, this.diamondAxeModel, this.netheriteAxeModel});
      instance = this;
   }

   private SelectSetting createModelSetting(String name, String typeKey, ItemReplacer.SwordVariant[] variants) {
      return new SelectSetting(name, "Модель для " + typeKey.toLowerCase())
         .values(Arrays.asList(getVariantLabels(variants)))
         .selected(variants[0].label)
         .visible(() -> this.swordTypes.isSelected(typeKey));
   }

   public static boolean captureForRender(ItemStack stack, ModelTransformationMode transformType, LivingEntity entity) {
      clearPending();
      if (stack != null && !stack.isEmpty() && entity != null) {
         ItemReplacer replacer = instance;
         if (replacer != null && replacer.isState()) {
            String variantOverride = replacer.resolveVariantLabel(entity, stack);
            if (variantOverride == null) {
               return false;
            } else {
               PENDING_STACK.set(stack);
               PENDING_MODE.set(transformType);
               PENDING_VARIANT.set(variantOverride);
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean renderPending(MatrixStack matrixStack, VertexConsumerProvider buffer, int combinedLight, int combinedOverlay) {
      ItemStack stack = PENDING_STACK.get();
      ModelTransformationMode transformType = PENDING_MODE.get();
      String variantLabel = PENDING_VARIANT.get();
      clearPending();
      if (stack != null && transformType != null && variantLabel != null) {
         ItemReplacer replacer = instance;
         return replacer != null && replacer.isState()
            ? replacer.resolveVariant(variantLabel).getModel().render(stack, transformType, matrixStack, buffer, combinedLight, combinedOverlay)
            : false;
      } else {
         return false;
      }
   }

   @Nullable
   private String resolveVariantLabel(LivingEntity entity, ItemStack stack) {
      MinecraftClient client = MinecraftClient.getInstance();
      return client.player != null && entity == client.player && this.shouldReplace(stack) ? this.getModelLabelFor(stack.getItem()) : null;
   }

   @Nullable
   private String getModelLabelFor(Item item) {
      if (item == Items.DIAMOND_SWORD && this.swordTypes.isSelected("Алмазный меч")) {
         return this.diamondSwordModel.getSelected();
      } else if (item == Items.NETHERITE_SWORD && this.swordTypes.isSelected("Незеритовый меч")) {
         return this.netheriteSwordModel.getSelected();
      } else if (item == Items.DIAMOND_AXE && this.swordTypes.isSelected("Алмазный топор")) {
         return this.diamondAxeModel.getSelected();
      } else {
         return item == Items.NETHERITE_AXE && this.swordTypes.isSelected("Незеритовый топор") ? this.netheriteAxeModel.getSelected() : null;
      }
   }

   private ItemReplacer.SwordVariant resolveVariant(@Nullable String labelOverride) {
      if (labelOverride != null && !labelOverride.isBlank()) {
         ItemReplacer.SwordVariant variant = variantsByLabel().get(labelOverride.toLowerCase(Locale.ROOT));
         return variant != null ? variant : this.getSelectedVariant();
      } else {
         return this.getSelectedVariant();
      }
   }

   private boolean shouldReplace(ItemStack stack) {
      return this.getModelLabelFor(stack.getItem()) != null;
   }

   public ItemReplacer.SwordVariant getSelectedVariant() {
      return this.getSelectedVariant(this.diamondSwordModel.getSelected());
   }

   public ItemReplacer.SwordVariant getSelectedVariant(@Nullable String label) {
      if (label == null || label.isBlank()) {
         return swordVariants()[0];
      } else if (label.equalsIgnoreCase(this.cachedSelectedLabel)) {
         return this.cachedSelectedVariant;
      } else {
         ItemReplacer.SwordVariant variant = variantsByLabel().get(label.toLowerCase(Locale.ROOT));
         if (variant == null) {
            variant = swordVariants()[0];
         }

         this.cachedSelectedLabel = label;
         this.cachedSelectedVariant = variant;
         return variant;
      }
   }

   @Override
   public void deactivate() {
      this.clearLoadedModels();
      clearPending();
      super.deactivate();
   }

   public void clearLoadedModels() {
      for (ItemReplacer.SwordVariant variant : swordVariants()) {
         variant.clear();
      }
   }

   public static String[] getVariantLabels() {
      return getVariantLabels(swordVariants());
   }

   private static String[] getVariantLabels(ItemReplacer.SwordVariant[] variants) {
      String[] labels = new String[variants.length];

      for (int i = 0; i < variants.length; i++) {
         labels[i] = variants[i].label;
      }

      return labels;
   }

   private static ItemReplacer.SwordVariant[] swordVariants() {
      ItemReplacer.SwordVariant[] cached = swordVariants;
      if (cached != null) {
         return cached;
      } else {
         Class<ItemReplacer> var2 = ItemReplacer.class;
         synchronized (ItemReplacer.class) {
            cached = swordVariants;
            if (cached == null) {
               cached = createSwordVariants();
               swordVariants = cached;
            }

            return cached;
         }
      }
   }

   private static Map<String, ItemReplacer.SwordVariant> variantsByLabel() {
      Map<String, ItemReplacer.SwordVariant> cached = variantsByLabel;
      if (cached != null) {
         return cached;
      } else {
         Class<ItemReplacer> var2 = ItemReplacer.class;
         synchronized (ItemReplacer.class) {
            cached = variantsByLabel;
            if (cached == null) {
               variantsByLabel = cached = createVariantMap(swordVariants());
            }

            return cached;
         }
      }
   }

   private static Map<String, ItemReplacer.SwordVariant> createVariantMap(ItemReplacer.SwordVariant[] variants) {
      HashMap<String, ItemReplacer.SwordVariant> map = new HashMap<>(variants.length * 2);

      for (ItemReplacer.SwordVariant variant : variants) {
         map.put(variant.label.toLowerCase(Locale.ROOT), variant);
      }

      return map;
   }

   private static ItemReplacer.SwordVariant[] createSwordVariants() {
      String[][] data = new String[][]{
         {"Abominable Blade", "abominableblade"},
         {"Abominable Greatsaber", "abominablegreatsaber"},
         {"Abominable Scythe", "abominablescythe"},
         {"Acid Demon", "aciddemon"},
         {"Amethyst Shuriken", "amethyst_shuriken"},
         {"Ancient Royal Greatsword", "ancient_royal_great_sword"},
         {"Aquantic Sacred Blade", "aquantic_sacred_blade"},
         {"Aquantic Trident", "aquantictrident"},
         {"Arcanethyst", "arcanethyst"},
         {"Ashura Blade", "ashura_blade"},
         {"Awakened Lichblade", "awakened_lichblade"},
         {"Blood Edge", "bloodedge"},
         {"Bloody Death", "bloodydeath"},
         {"Bramblethorn", "bramblethorn"},
         {"Brimstone Claymore", "brimstone_claymore"},
         {"Carian Sword", "cariansword"},
         {"Chrono Blade", "chrono_blade"},
         {"Corrupted Mythic Blade", "corruptedmythicblade"},
         {"Creation Splitter", "creationsplitter"},
         {"Crescent Rose", "crescentrose"},
         {"Cyber Katana", "cyberkatana"},
         {"Cyber Mantis Blade", "cybermantisblade"},
         {"Cybernetic Katana", "cybernetickatana"},
         {"Cybernetic Knife", "cyberneticknife"},
         {"Cybernetic Sawblade", "cyberneticsawblade"},
         {"Cyber Sword", "cybersword"},
         {"Dainsleif", "dainsleif"},
         {"Dark Blade", "dark_blade"},
         {"Dark Cleaver", "dark_cleaver"},
         {"Death Knight Dagger", "death_knight_dagger"},
         {"Death Knight Sword", "death_knight_sword"},
         {"Demigod's Unholy Blade", "demigodsunholyblade"},
         {"Demigod's Unholy Halberd", "demigodsunholyhalberd"},
         {"Demonic Blade", "demonicblade"},
         {"Demonic Cleaver", "demoniccleaver"},
         {"Demon Lord's Great Axe", "demonlordsgreataxe"},
         {"Demon Lord's Sword", "demonlordsword"},
         {"Divine Justice", "divine_justice"},
         {"Divine Reaper", "divine_reaper"},
         {"Divine Axe Rhitta", "divineaxerhitta"},
         {"Divine Punisher", "divinepunisher"},
         {"Dragon Slaying Blade", "dragonslayingblade"},
         {"Edge of the Astral Plane", "edgeoftheastralplane"},
         {"Emberblade", "emberblade"},
         {"Enigma", "enigma"},
         {"Epic Sword", "epicsword"},
         {"Estoc", "estoc"},
         {"Excalibur", "excalibur"},
         {"Fallen God Spear", "fallengodspear"},
         {"Fallen God Sword", "fallengodsword"},
         {"Floral Longsword", "floral_longsword"},
         {"Floral Sabre", "floral_sabre"},
         {"Forest Guardian Glaive", "forest_guardian_glaive"},
         {"Frost Axe", "frostaxe"},
         {"Frostblade", "frostblade"},
         {"Frost Scythe", "frostscythe"},
         {"Green Scythe", "greenscythe"},
         {"Hearthflame", "hearthflame"},
         {"Hero Sword", "herosword"},
         {"Holy Moonlight Sword", "holymoonlightsword"},
         {"Hornet's Needle", "hornetsneedle"},
         {"Ice Whisper", "icewhisper"},
         {"Jade Halberd", "jadehalberd"},
         {"Katana", "katana"},
         {"Legendary Sword", "legendarysword"},
         {"Longsword", "longsword"},
         {"Magi Scythe", "magiscythe"},
         {"Masamune", "masamune"},
         {"Mjolnir", "mjolnir"},
         {"Molten Blade", "moltenblade"},
         {"Molten Sword", "moltensword"},
         {"Muramasa", "muramasa"},
         {"Mystical Spellblade", "mysticalspellblade"},
         {"Mythic Blade", "mythicblade"},
         {"Partisan", "partisan"},
         {"Pharaoh's Treasure", "pharaohs_treasure"},
         {"Phoenix Grace", "pheonixgrace"},
         {"Powerfuse Hammer", "powerfusehammer"},
         {"Powerfuse Sword", "powerfusesword"},
         {"Requiem of Hell", "requiem_of_hell"},
         {"Ribbon Cleaver", "ribboncleaver"},
         {"Righteous Relic", "righteous_relic"},
         {"Rivers of Blood", "riversofblood"},
         {"Royal Chakram", "royalchakram"},
         {"Royal Rapier", "royalrapier"},
         {"Sabre", "sabre"},
         {"Scissor Blade", "scissorblade"},
         {"Sculk Cleaver", "sculkcleaver"},
         {"Sculk Scythe", "sculkscythe"},
         {"Sculk Sword", "sculksword"},
         {"Sentinel's Will", "sentinels_will"},
         {"Silverine Blade", "silverine_blade"},
         {"Soul Claws", "soulclaws"},
         {"Soul Edge", "souledge"},
         {"Soul Harvester", "soulharvester"},
         {"Soul Render", "soulrender"},
         {"Soul Stealer", "soulstealer"},
         {"Soul Collector", "soul_collector"},
         {"Soul Devourer", "soul_devourer"},
         {"Star's Edge", "stars_edge"},
         {"Steel Sword", "steelsword"},
         {"Stop Sign", "stop_sign"},
         {"Stormbringer", "stormbringer"},
         {"Storm's Edge", "storms_edge"},
         {"Sunbreak", "sunbreak"},
         {"Tengen's Blade", "tengensblade"},
         {"Terrablade", "terrablade"},
         {"Thousand Demon Daggers", "thousanddemondaggers"},
         {"Thunderbrand", "thunderbrand"},
         {"Thunderbringer", "thunderbringer"},
         {"Toxic Longsword", "toxic_longsword"},
         {"Vampiric Needle", "vampiricneedle"},
         {"Wakizashi", "wakizashi"},
         {"Watcher Claymore", "watcher_claymore"},
         {"Watching Warglaive", "watching_warglaive"},
         {"Waxweaver", "waxweaver"},
         {"Whisperwind", "whisperwind"},
         {"Wickpiercer", "wickpiercer"},
         {"Yoru", "yoru"}
      };
      ItemReplacer.SwordVariant[] variants = new ItemReplacer.SwordVariant[data.length];

      for (int i = 0; i < data.length; i++) {
         variants[i] = new ItemReplacer.SwordVariant(data[i][0], data[i][1]);
      }

      return variants;
   }

   private static final class DisplayTransform {
      private final float txN;
      private final float txM;
      private final float ty;
      private final float tz;
      private final float rx;
      private final float ry;
      private final float rz;
      private final float sx;
      private final float sy;
      private final float sz;
      private final boolean hasTrans;
      private final boolean hasRot;
      private final boolean hasScale;

      private DisplayTransform(float[] rotation, float[] translation, float[] scale) {
         this.txN = translation[0] / 16.0F;
         this.txM = -this.txN;
         this.ty = translation[1] / 16.0F;
         this.tz = translation[2] / 16.0F;
         this.rx = rotation[0];
         this.ry = rotation[1];
         this.rz = rotation[2];
         this.sx = scale[0];
         this.sy = scale[1];
         this.sz = scale[2];
         this.hasTrans = translation[0] != 0.0F || translation[1] != 0.0F || translation[2] != 0.0F;
         this.hasRot = rotation[0] != 0.0F || rotation[1] != 0.0F || rotation[2] != 0.0F;
         this.hasScale = scale[0] != 1.0F || scale[1] != 1.0F || scale[2] != 1.0F;
      }

      private void apply(MatrixStack ms, boolean leftHand) {
         if (this.hasTrans) {
            ms.translate(leftHand ? this.txM : this.txN, this.ty, this.tz);
         }

         if (this.hasRot) {
            float yaw = leftHand ? -this.ry : this.ry;
            float pitch = leftHand ? -this.rx : this.rx;
            float roll = this.rz;
            if (yaw != 0.0F) {
               ms.multiply(new Quaternionf().rotationY(Math.toRadians(yaw)));
            }

            if (pitch != 0.0F) {
               ms.multiply(new Quaternionf().rotationX(Math.toRadians(pitch)));
            }

            if (roll != 0.0F) {
               ms.multiply(new Quaternionf().rotationZ(Math.toRadians(roll)));
            }
         }

         if (this.hasScale) {
            ms.scale(this.sx, this.sy, this.sz);
         }
      }
   }

   private static final class FaceUv {
      final float u1;
      final float v1;
      final float u2;
      final float v2;

      FaceUv(float u1, float v1, float u2, float v2) {
         this.u1 = u1;
         this.v1 = v1;
         this.u2 = u2;
         this.v2 = v2;
      }
   }

   private static final class ModelCube {
      static final int NO_ROTATION = 0;
      static final int ROTATE_X = 1;
      static final int ROTATE_Y = 2;
      static final int ROTATE_Z = 3;
      final float x1;
      final float y1;
      final float z1;
      final float x2;
      final float y2;
      final float z2;
      ItemReplacer.FaceUv north;
      ItemReplacer.FaceUv south;
      ItemReplacer.FaceUv east;
      ItemReplacer.FaceUv west;
      ItemReplacer.FaceUv up;
      ItemReplacer.FaceUv down;
      float rotationOriginX;
      float rotationOriginY;
      float rotationOriginZ;
      int rotationAxis = 0;
      float rotationSin;
      float rotationCos = 1.0F;

      ModelCube(float x1, float y1, float z1, float x2, float y2, float z2) {
         this.x1 = x1;
         this.y1 = y1;
         this.z1 = z1;
         this.x2 = x2;
         this.y2 = y2;
         this.z2 = z2;
      }

      int countFaces() {
         int n = 0;
         if (this.north != null) {
            n++;
         }

         if (this.south != null) {
            n++;
         }

         if (this.east != null) {
            n++;
         }

         if (this.west != null) {
            n++;
         }

         if (this.up != null) {
            n++;
         }

         if (this.down != null) {
            n++;
         }

         return n;
      }

      void setRotation(String axis, float angle) {
         switch (axis) {
            case "x":
               this.rotationAxis = 1;
               break;
            case "y":
               this.rotationAxis = 2;
               break;
            case "z":
               this.rotationAxis = 3;
               break;
            default:
               this.rotationAxis = 0;
               return;
         }

         float rad = (float)java.lang.Math.toRadians(angle);
         this.rotationSin = (float)java.lang.Math.sin(rad);
         this.rotationCos = (float)java.lang.Math.cos(rad);
      }
   }

   private static final class RuntimeSwordModel {
      private static final String MODEL_ROOT = "item_replacer/sword/models/";
      private static final String TEXTURE_ROOT = "item_replacer/sword/textures/";
      private static final String NAMESPACE = "mre";
      private static final float PIXEL = 0.0625F;
      private static final float MODEL_CENTER = 0.5F;
      private static final int VERTEX_STRIDE = 8;
      private final String modelPath;
      private final ArrayList<ItemReplacer.ModelCube> cubes = new ArrayList<>();
      private final ItemReplacer.DisplayTransform[] displayTransforms = new ItemReplacer.DisplayTransform[8];
      private Identifier textureId;
      private RenderLayer renderLayer;
      private float[] bakedMesh = new float[0];
      private int vertexCount;
      private boolean loaded;

      private RuntimeSwordModel(String modelName) {
         this.modelPath = "item_replacer/sword/models/" + modelName + ".json";
      }

      private void clear() {
         this.loaded = false;
         this.textureId = null;
         this.renderLayer = null;
         this.bakedMesh = new float[0];
         this.vertexCount = 0;
         this.cubes.clear();

         for (int i = 0; i < this.displayTransforms.length; i++) {
            this.displayTransforms[i] = null;
         }
      }

      private boolean render(
         ItemStack stack, ModelTransformationMode transformType, MatrixStack matrixStack, VertexConsumerProvider buffer, int combinedLight, int combinedOverlay
      ) {
         this.loadIfNeeded();
         if (this.vertexCount != 0 && this.renderLayer != null) {
            VertexConsumer consumer = stack.hasGlint()
               ? ItemRenderer.getItemGlintConsumer(buffer, this.renderLayer, true, true)
               : buffer.getBuffer(this.renderLayer);
            matrixStack.push();
            this.applyDisplayTransform(matrixStack, transformType);
            Entry entry = matrixStack.peek();
            this.drawMesh(consumer, entry.getPositionMatrix(), entry.getNormalMatrix(), combinedLight, combinedOverlay);
            matrixStack.pop();
            return true;
         } else {
            return false;
         }
      }

      private MinecraftClient mc() {
         return MinecraftClient.getInstance();
      }

      private void loadIfNeeded() {
         if (!this.loaded) {
            this.loaded = true;

            try (
               InputStream input = this.mc().getResourceManager().open(Identifier.of("mre", this.modelPath));
               InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            ) {
               JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
               this.readModel(root);
            } catch (Exception var9) {
               System.err.println("[ItemReplacer] Failed to load model " + this.modelPath + ": " + var9.getMessage());
            }
         }
      }

      private void readModel(JsonObject root) {
         JsonObject texturesObj = root.getAsJsonObject("textures");
         if (texturesObj != null) {
            HashMap<String, String> textureMap = new HashMap<>();

            for (java.util.Map.Entry e : texturesObj.entrySet()) {
               textureMap.put((String)e.getKey(), ((JsonElement)e.getValue()).getAsString());
            }

            String primaryKey = this.resolvePrimaryTextureKey(textureMap, root);
            String primaryPath = textureMap.get(primaryKey);
            if (primaryPath != null) {
               String texName = primaryPath.contains("/") ? primaryPath.substring(primaryPath.lastIndexOf(47) + 1) : primaryPath;
               this.textureId = Identifier.of("mre", "item_replacer/sword/textures/" + texName + ".png");
               this.renderLayer = this.hasPartialAlpha(this.textureId)
                  ? RenderLayer.getItemEntityTranslucentCull(this.textureId)
                  : RenderLayer.getEntityCutout(this.textureId);
               JsonArray elements = root.getAsJsonArray("elements");
               if (elements != null) {
                  String primaryRef = "#" + primaryKey;

                  for (JsonElement el : elements) {
                     JsonObject obj = el.getAsJsonObject();
                     JsonArray from = obj.getAsJsonArray("from");
                     JsonArray to = obj.getAsJsonArray("to");
                     if (from != null && to != null) {
                        ItemReplacer.ModelCube cube = new ItemReplacer.ModelCube(
                           from.get(0).getAsFloat() * 0.0625F,
                           from.get(1).getAsFloat() * 0.0625F,
                           from.get(2).getAsFloat() * 0.0625F,
                           to.get(0).getAsFloat() * 0.0625F,
                           to.get(1).getAsFloat() * 0.0625F,
                           to.get(2).getAsFloat() * 0.0625F
                        );
                        JsonObject faces = obj.getAsJsonObject("faces");
                        if (faces != null && this.readFaces(cube, faces, primaryRef)) {
                           if (obj.has("rotation")) {
                              this.readRotation(cube, obj.getAsJsonObject("rotation"));
                           }

                           this.cubes.add(cube);
                        }
                     }
                  }

                  this.bakeMesh();
                  this.readDisplayTransforms(root);
               }
            }
         }
      }

      private boolean readFaces(ItemReplacer.ModelCube cube, JsonObject faces, String primaryRef) {
         boolean has = false;
         if (faces.has("north")) {
            cube.north = this.readFace(faces.getAsJsonObject("north"), primaryRef);
            has |= cube.north != null;
         }

         if (faces.has("south")) {
            cube.south = this.readFace(faces.getAsJsonObject("south"), primaryRef);
            has |= cube.south != null;
         }

         if (faces.has("east")) {
            cube.east = this.readFace(faces.getAsJsonObject("east"), primaryRef);
            has |= cube.east != null;
         }

         if (faces.has("west")) {
            cube.west = this.readFace(faces.getAsJsonObject("west"), primaryRef);
            has |= cube.west != null;
         }

         if (faces.has("up")) {
            cube.up = this.readFace(faces.getAsJsonObject("up"), primaryRef);
            has |= cube.up != null;
         }

         if (faces.has("down")) {
            cube.down = this.readFace(faces.getAsJsonObject("down"), primaryRef);
            has |= cube.down != null;
         }

         return has;
      }

      private ItemReplacer.FaceUv readFace(JsonObject face, String primaryRef) {
         if (face.has("texture") && !face.get("texture").getAsString().equals(primaryRef)) {
            return null;
         } else {
            JsonArray uv = face.getAsJsonArray("uv");
            return uv != null && uv.size() >= 4
               ? new ItemReplacer.FaceUv(
                  uv.get(0).getAsFloat() * 0.0625F, uv.get(1).getAsFloat() * 0.0625F, uv.get(2).getAsFloat() * 0.0625F, uv.get(3).getAsFloat() * 0.0625F
               )
               : null;
         }
      }

      private void readRotation(ItemReplacer.ModelCube cube, JsonObject rotation) {
         if (rotation != null && rotation.has("origin") && rotation.has("axis") && rotation.has("angle")) {
            JsonArray origin = rotation.getAsJsonArray("origin");
            cube.rotationOriginX = origin.get(0).getAsFloat() * 0.0625F;
            cube.rotationOriginY = origin.get(1).getAsFloat() * 0.0625F;
            cube.rotationOriginZ = origin.get(2).getAsFloat() * 0.0625F;
            cube.setRotation(rotation.get("axis").getAsString(), rotation.get("angle").getAsFloat());
         }
      }

      private String resolvePrimaryTextureKey(Map<String, String> textureMap, JsonObject root) {
         if (textureMap.containsKey("0")) {
            return "0";
         } else {
            HashMap<String, Integer> useCount = new HashMap<>();
            JsonArray elements = root.getAsJsonArray("elements");
            if (elements != null) {
               for (JsonElement el : elements) {
                  JsonObject faces = el.getAsJsonObject().getAsJsonObject("faces");
                  if (faces != null) {
                     for (java.util.Map.Entry fe : faces.entrySet()) {
                        JsonObject f = ((JsonElement)fe.getValue()).getAsJsonObject();
                        if (f.has("texture")) {
                           String ref = f.get("texture").getAsString().replace("#", "");
                           useCount.merge(ref, 1, Integer::sum);
                        }
                     }
                  }
               }
            }

            String best = null;
            int bestCount = -1;

            for (java.util.Map.Entry e : useCount.entrySet()) {
               if (textureMap.containsKey(e.getKey()) && !"particle".equals(e.getKey()) && (Integer)e.getValue() > bestCount) {
                  best = (String)e.getKey();
                  bestCount = (Integer)e.getValue();
               }
            }

            if (best != null) {
               return best;
            } else {
               for (String key : textureMap.keySet()) {
                  if (!"particle".equals(key)) {
                     return key;
                  }
               }

               return textureMap.keySet().iterator().next();
            }
         }
      }

      private boolean hasPartialAlpha(Identifier id) {
         throw new IllegalStateException("Decompilation failed");
      }

      private void bakeMesh() {
         int faceCount = 0;

         for (ItemReplacer.ModelCube c : this.cubes) {
            faceCount += c.countFaces();
         }

         float[] baked = new float[faceCount * 4 * 8];
         int offset = 0;

         for (ItemReplacer.ModelCube c : this.cubes) {
            offset = this.bakeCube(baked, offset, c);
         }

         this.bakedMesh = baked;
         this.vertexCount = offset / 8;
         this.cubes.clear();
         this.cubes.trimToSize();
      }

      private int bakeCube(float[] out, int offset, ItemReplacer.ModelCube cube) {
         float x1 = cube.x1;
         float y1 = cube.y1;
         float z1 = cube.z1;
         float x2 = cube.x2;
         float y2 = cube.y2;
         float z2 = cube.z2;
         if (cube.north != null) {
            ItemReplacer.FaceUv f = cube.north;
            offset = this.writeBakedVertex(out, offset, cube, x2, y1, z1, f.u1, f.v2, 0.0F, 0.0F, -1.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y1, z1, f.u2, f.v2, 0.0F, 0.0F, -1.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y2, z1, f.u2, f.v1, 0.0F, 0.0F, -1.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y2, z1, f.u1, f.v1, 0.0F, 0.0F, -1.0F);
         }

         if (cube.south != null) {
            ItemReplacer.FaceUv f = cube.south;
            offset = this.writeBakedVertex(out, offset, cube, x1, y1, z2, f.u1, f.v2, 0.0F, 0.0F, 1.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y1, z2, f.u2, f.v2, 0.0F, 0.0F, 1.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y2, z2, f.u2, f.v1, 0.0F, 0.0F, 1.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y2, z2, f.u1, f.v1, 0.0F, 0.0F, 1.0F);
         }

         if (cube.east != null) {
            ItemReplacer.FaceUv f = cube.east;
            offset = this.writeBakedVertex(out, offset, cube, x2, y1, z2, f.u1, f.v2, 1.0F, 0.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y1, z1, f.u2, f.v2, 1.0F, 0.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y2, z1, f.u2, f.v1, 1.0F, 0.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y2, z2, f.u1, f.v1, 1.0F, 0.0F, 0.0F);
         }

         if (cube.west != null) {
            ItemReplacer.FaceUv f = cube.west;
            offset = this.writeBakedVertex(out, offset, cube, x1, y1, z1, f.u1, f.v2, -1.0F, 0.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y1, z2, f.u2, f.v2, -1.0F, 0.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y2, z2, f.u2, f.v1, -1.0F, 0.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y2, z1, f.u1, f.v1, -1.0F, 0.0F, 0.0F);
         }

         if (cube.up != null) {
            ItemReplacer.FaceUv f = cube.up;
            offset = this.writeBakedVertex(out, offset, cube, x1, y2, z1, f.u1, f.v1, 0.0F, 1.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y2, z2, f.u1, f.v2, 0.0F, 1.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y2, z2, f.u2, f.v2, 0.0F, 1.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y2, z1, f.u2, f.v1, 0.0F, 1.0F, 0.0F);
         }

         if (cube.down != null) {
            ItemReplacer.FaceUv f = cube.down;
            offset = this.writeBakedVertex(out, offset, cube, x1, y1, z2, f.u1, f.v1, 0.0F, -1.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y1, z1, f.u1, f.v2, 0.0F, -1.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y1, z1, f.u2, f.v2, 0.0F, -1.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y1, z2, f.u2, f.v1, 0.0F, -1.0F, 0.0F);
         }

         return offset;
      }

      private int writeBakedVertex(
         float[] out, int offset, ItemReplacer.ModelCube cube, float x, float y, float z, float u, float v, float nx, float ny, float nz
      ) {
         if (cube.rotationAxis != 0) {
            float dx = x - cube.rotationOriginX;
            float dy = y - cube.rotationOriginY;
            float dz = z - cube.rotationOriginZ;
            float sin = cube.rotationSin;
            float cos = cube.rotationCos;
            switch (cube.rotationAxis) {
               case 1:
                  y = cube.rotationOriginY + dy * cos - dz * sin;
                  z = cube.rotationOriginZ + dy * sin + dz * cos;
                  float rny = ny * cos - nz * sin;
                  nz = ny * sin + nz * cos;
                  ny = rny;
                  break;
               case 2: {
                  x = cube.rotationOriginX + dx * cos + dz * sin;
                  z = cube.rotationOriginZ - dx * sin + dz * cos;
                  float rnx = nx * cos + nz * sin;
                  nz = -nx * sin + nz * cos;
                  nx = rnx;
                  break;
               }
               case 3: {
                  x = cube.rotationOriginX + dx * cos - dy * sin;
                  y = cube.rotationOriginY + dx * sin + dy * cos;
                  float rnx = nx * cos - ny * sin;
                  ny = nx * sin + ny * cos;
                  nx = rnx;
               }
            }
         }

         out[offset++] = x - 0.5F;
         out[offset++] = y - 0.5F;
         out[offset++] = z - 0.5F;
         out[offset++] = u;
         out[offset++] = v;
         out[offset++] = nx;
         out[offset++] = ny;
         out[offset++] = nz;
         return offset;
      }

      private void readDisplayTransforms(JsonObject root) {
         if (root.has("display")) {
            JsonObject d = root.getAsJsonObject("display");
            this.displayTransforms[0] = this.readDT(d, "thirdperson_righthand");
            this.displayTransforms[1] = this.readDT(d, "thirdperson_lefthand");
            this.displayTransforms[2] = this.readDT(d, "firstperson_righthand");
            this.displayTransforms[3] = this.readDT(d, "firstperson_lefthand");
            this.displayTransforms[4] = this.readDT(d, "ground");
            this.displayTransforms[5] = this.readDT(d, "gui");
            this.displayTransforms[6] = this.readDT(d, "head");
            this.displayTransforms[7] = this.readDT(d, "fixed");
         }
      }

      private ItemReplacer.DisplayTransform readDT(JsonObject display, String key) {
         if (!display.has(key)) {
            return null;
         } else {
            JsonObject obj = display.getAsJsonObject(key);
            float[] rot = new float[]{0.0F, 0.0F, 0.0F};
            float[] trans = new float[]{0.0F, 0.0F, 0.0F};
            float[] scale = new float[]{1.0F, 1.0F, 1.0F};
            if (obj.has("rotation")) {
               this.readFA(obj.getAsJsonArray("rotation"), rot);
            }

            if (obj.has("translation")) {
               this.readFA(obj.getAsJsonArray("translation"), trans);
            }

            if (obj.has("scale")) {
               this.readFA(obj.getAsJsonArray("scale"), scale);
            }

            return new ItemReplacer.DisplayTransform(rot, trans, scale);
         }
      }

      private void readFA(JsonArray arr, float[] target) {
         for (int i = 0; i < target.length && i < arr.size(); i++) {
            target[i] = arr.get(i).getAsFloat();
         }
      }

      private void applyDisplayTransform(MatrixStack ms, ModelTransformationMode type) {
         ItemReplacer.DisplayTransform dt = this.getDisplayTransform(type);
         if (dt != null) {
            boolean left = type == ModelTransformationMode.THIRD_PERSON_LEFT_HAND || type == ModelTransformationMode.FIRST_PERSON_LEFT_HAND;
            dt.apply(ms, left);
         }
      }

      private ItemReplacer.DisplayTransform getDisplayTransform(ModelTransformationMode type) {
         return switch (type) {
            case THIRD_PERSON_RIGHT_HAND -> this.displayTransforms[0];
            case THIRD_PERSON_LEFT_HAND -> this.displayTransforms[1];
            case FIRST_PERSON_RIGHT_HAND -> this.displayTransforms[2];
            case FIRST_PERSON_LEFT_HAND -> this.displayTransforms[3];
            case GROUND -> this.displayTransforms[4];
            case GUI -> this.displayTransforms[5];
            case HEAD -> this.displayTransforms[6];
            case FIXED -> this.displayTransforms[7];
            default -> null;
         };
      }

      private void drawMesh(VertexConsumer vc, Matrix4f matrix, Matrix3f normalMatrix, int light, int overlay) {
         int limit = this.vertexCount * 8;
         Vector4f pos = new Vector4f();
         Vector3f norm = new Vector3f();

         for (int i = 0; i < limit; i += 8) {
            pos.set(this.bakedMesh[i], this.bakedMesh[i + 1], this.bakedMesh[i + 2], 1.0F);
            matrix.transform(pos);
            norm.set(this.bakedMesh[i + 5], this.bakedMesh[i + 6], this.bakedMesh[i + 7]);
            normalMatrix.transform(norm);
            vc.vertex(pos.x, pos.y, pos.z)
               .color(255, 255, 255, 255)
               .texture(this.bakedMesh[i + 3], this.bakedMesh[i + 4])
               .overlay(overlay)
               .light(light)
               .normal(norm.x, norm.y, norm.z);
         }
      }
   }

   public static final class SwordVariant {
      public final String label;
      public final String modelName;
      private ItemReplacer.RuntimeSwordModel model;

      private SwordVariant(String label, String modelName) {
         this.label = label;
         this.modelName = modelName;
      }

      ItemReplacer.RuntimeSwordModel getModel() {
         if (this.model == null) {
            this.model = new ItemReplacer.RuntimeSwordModel(this.modelName);
         }

         return this.model;
      }

      void clear() {
         if (this.model != null) {
            this.model.clear();
         }
      }
   }
}
