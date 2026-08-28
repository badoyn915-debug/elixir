package code.elixir.features.impl.render.custommodels;

import code.elixir.features.impl.render.CustomModelModule;
import code.elixir.features.impl.render.custommodels.models.AmogusModel;
import code.elixir.features.impl.render.custommodels.models.ChinchillaModel;
import code.elixir.features.impl.render.custommodels.models.CrabModel;
import code.elixir.features.impl.render.custommodels.models.DemonModel;
import code.elixir.features.impl.render.custommodels.models.FredBearModel;
import code.elixir.features.impl.render.custommodels.models.JeffKillerModel;
import code.elixir.features.impl.render.custommodels.models.RabbitModel;
import code.elixir.features.impl.render.custommodels.models.SonicModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

public class CustomModelFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   private static final Identifier RABBIT_TEXTURE = Identifier.of("minecraft", "models/rabbit.png");
   private static final Identifier FREDDY_TEXTURE = Identifier.of("minecraft", "models/freddy.png");
   private static final Identifier DEMON_TEXTURE = Identifier.of("minecraft", "models/demon.png");
   private static final Identifier WHITE_DEMON_TEXTURE = Identifier.of("minecraft", "models/whitedemon.png");
   private static final Identifier SONIC_TEXTURE = Identifier.of("minecraft", "models/sonic.png");
   private static final Identifier AMOGUS_TEXTURE = Identifier.of("minecraft", "models/amogus.png");
   private static final Identifier JEFF_TEXTURE = Identifier.of("minecraft", "models/jeff.png");
   private static final Identifier CRAB_TEXTURE = Identifier.of("minecraft", "models/crab.png");
   private static final Identifier CHINCHILLA_TEXTURE = Identifier.of("minecraft", "models/chinchilla.png");
   private static final Identifier DEFAULT_TEXTURE = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
   private final RabbitModel rabbitModel = new RabbitModel();
   private final DemonModel demonModel = new DemonModel();
   private final FredBearModel fredBearModel = new FredBearModel();
   private final SonicModel sonicModel = new SonicModel();
   private final AmogusModel amogusModel = new AmogusModel();
   private final JeffKillerModel jeffKillerModel = new JeffKillerModel();
   private final CrabModel crabModel = new CrabModel();
   private final ChinchillaModel chinchillaModel = new ChinchillaModel();

   public CustomModelFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
      super(context);
   }

   public void render(
      MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance
   ) {
      AbstractClientPlayerEntity player = this.getPlayer(state);
      String modelType = player == null ? null : CustomModelModule.getModelType(player);
      if (modelType != null && !modelType.isEmpty()) {
         this.hideDefaultModel();
         this.renderCustomModel(matrices, vertexConsumers, light, state, modelType);
      } else {
         this.restoreDefaultModel();
      }
   }

   private void hideDefaultModel() {
      PlayerEntityModel model = (PlayerEntityModel)this.getContextModel();
      model.head.visible = false;
      model.hat.visible = false;
      model.body.visible = false;
      model.rightArm.visible = false;
      model.leftArm.visible = false;
      model.rightLeg.visible = false;
      model.leftLeg.visible = false;
      model.jacket.visible = false;
      model.leftSleeve.visible = false;
      model.rightSleeve.visible = false;
      model.leftPants.visible = false;
      model.rightPants.visible = false;
   }

   private void restoreDefaultModel() {
      PlayerEntityModel model = (PlayerEntityModel)this.getContextModel();
      model.head.visible = true;
      model.hat.visible = true;
      model.body.visible = true;
      model.rightArm.visible = true;
      model.leftArm.visible = true;
      model.rightLeg.visible = true;
      model.leftLeg.visible = true;
      model.jacket.visible = true;
      model.leftSleeve.visible = true;
      model.rightSleeve.visible = true;
      model.leftPants.visible = true;
      model.rightPants.visible = true;
   }

   private void renderCustomModel(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, PlayerEntityRenderState state, String modelType) {
      matrices.push();
      VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(this.getTextureForModel(modelType)));
      PlayerEntityModel playerModel = (PlayerEntityModel)this.getContextModel();
      switch (modelType) {
         case "Crazy Rabbit":
            matrices.scale(0.825F, 0.825F, 0.825F);
            matrices.translate(0.0F, 0.3F, 0.0F);
            this.rabbitModel.setAngles(state, playerModel);
            this.rabbitModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
            break;
         case "White Demon":
         case "Red Demon":
            this.demonModel.setAngles(state, playerModel);
            this.demonModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
            break;
         case "Freddy Bear":
            matrices.scale(0.75F, 0.65F, 0.75F);
            matrices.translate(0.0F, 0.85F, 0.0F);
            this.fredBearModel.setAngles(state, playerModel);
            this.fredBearModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
            break;
         case "Sonic":
            matrices.scale(1.3F, 1.3F, 1.3F);
            matrices.translate(0.0, -0.35, 0.0);
            this.sonicModel.setAngles(state, playerModel);
            this.sonicModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
            break;
         case "Amogus":
            this.amogusModel.setAngles(state, playerModel);
            this.amogusModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
            break;
         case "Jeff Killer":
            this.jeffKillerModel.setAngles(state, playerModel);
            this.jeffKillerModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
            break;
         case "Crab":
            this.crabModel.setAngles(state, playerModel);
            this.crabModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
            break;
         case "Chinchilla":
            matrices.scale(1.4F, 1.4F, 1.4F);
            matrices.translate(0.0F, -0.45F, 0.0F);
            this.chinchillaModel.setAngles(state, playerModel);
            this.chinchillaModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
      }

      matrices.pop();
   }

   private Identifier getTextureForModel(String modelType) {
      return switch (modelType) {
         case "Crazy Rabbit" -> RABBIT_TEXTURE;
         case "Freddy Bear" -> FREDDY_TEXTURE;
         case "Red Demon" -> DEMON_TEXTURE;
         case "White Demon" -> WHITE_DEMON_TEXTURE;
         case "Sonic" -> SONIC_TEXTURE;
         case "Amogus" -> AMOGUS_TEXTURE;
         case "Jeff Killer" -> JEFF_TEXTURE;
         case "Crab" -> CRAB_TEXTURE;
         case "Chinchilla" -> CHINCHILLA_TEXTURE;
         default -> DEFAULT_TEXTURE;
      };
   }

   private AbstractClientPlayerEntity getPlayer(PlayerEntityRenderState state) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world == null) {
         return null;
      } else {
         Entity class_12972 = client.world.getEntityById(state.id);
         AbstractClientPlayerEntity player;
         return class_12972 instanceof AbstractClientPlayerEntity ? (player = (AbstractClientPlayerEntity)class_12972) : null;
      }
   }
}
