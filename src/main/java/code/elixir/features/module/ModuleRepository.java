package code.elixir.features.module;

import code.elixir.features.impl.combat.AimAssistModule;
import code.elixir.features.impl.combat.AntiBot;
import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.impl.combat.AutoArmor;
import code.elixir.features.impl.combat.AutoDodge;
import code.elixir.features.impl.combat.AutoExplosion;
import code.elixir.features.impl.combat.AutoPotion;
import code.elixir.features.impl.combat.AutoSwap;
import code.elixir.features.impl.combat.AutoTotem;
import code.elixir.features.impl.combat.CrystalTap;
import code.elixir.features.impl.combat.ElytraResolver;
import code.elixir.features.impl.combat.FastBow;
import code.elixir.features.impl.combat.HitBox;
import code.elixir.features.impl.combat.Neuro;
import code.elixir.features.impl.combat.NoFriendDamage;
import code.elixir.features.impl.combat.NoInteract;
import code.elixir.features.impl.combat.PacketCriticals;
import code.elixir.features.impl.combat.ProjectHelper;
import code.elixir.features.impl.combat.ShiftTap;
import code.elixir.features.impl.combat.TriggerBot;
import code.elixir.features.impl.combat.Velocity;
import code.elixir.features.impl.misc.AutoAuth;
import code.elixir.features.impl.misc.AutoBrew;
import code.elixir.features.impl.misc.AutoDuel;
import code.elixir.features.impl.misc.AutoLeave;
import code.elixir.features.impl.misc.AutoMineBot;
import code.elixir.features.impl.misc.AutoTpAccept;
import code.elixir.features.impl.misc.AutoWarden;
import code.elixir.features.impl.misc.ChestStealer;
import code.elixir.features.impl.misc.ClickFriend;
import code.elixir.features.impl.misc.ClientSounds;
import code.elixir.features.impl.misc.DeathCoords;
import code.elixir.features.impl.misc.ElytraHelper;
import code.elixir.features.impl.misc.FreeCam;
import code.elixir.features.impl.misc.Globals;
import code.elixir.features.impl.misc.ItemScroller;
import code.elixir.features.impl.misc.Panic;
import code.elixir.features.impl.misc.ServerHelper;
import code.elixir.features.impl.misc.ServerJoiner;
import code.elixir.features.impl.misc.ServerRPSpoofer;
import code.elixir.features.impl.misc.ShulkerBypass;
import code.elixir.features.impl.misc.SoundsRemove;
import code.elixir.features.impl.misc.TapeMouse;
import code.elixir.features.impl.misc.UseTracker;
import code.elixir.features.impl.misc.XCarry;
import code.elixir.features.impl.misc.astrummc.AstrummcHelper;
import code.elixir.features.impl.movement.AirStuck;
import code.elixir.features.impl.movement.AutoSprint;
import code.elixir.features.impl.movement.Blink;
import code.elixir.features.impl.movement.DragonFly;
import code.elixir.features.impl.movement.ElytraJump;
import code.elixir.features.impl.movement.ElytraMotion;
import code.elixir.features.impl.movement.ElytraRecast;
import code.elixir.features.impl.movement.ElytraTarget;
import code.elixir.features.impl.movement.ElytraWhatsapp;
import code.elixir.features.impl.movement.Flight;
import code.elixir.features.impl.movement.GuiMove;
import code.elixir.features.impl.movement.HighJump;
import code.elixir.features.impl.movement.NoClip;
import code.elixir.features.impl.movement.NoSlow;
import code.elixir.features.impl.movement.NoWeb;
import code.elixir.features.impl.movement.Speed;
import code.elixir.features.impl.movement.Spider;
import code.elixir.features.impl.movement.Strafe;
import code.elixir.features.impl.movement.SuperFireWork;
import code.elixir.features.impl.movement.TargetStrafe;
import code.elixir.features.impl.movement.WindHop;
import code.elixir.features.impl.player.AntiAFK;
import code.elixir.features.impl.player.AutoRespawn;
import code.elixir.features.impl.player.AutoTool;
import code.elixir.features.impl.player.AutoUse;
import code.elixir.features.impl.player.ClickPearl;
import code.elixir.features.impl.player.EnderChestPlus;
import code.elixir.features.impl.player.FakePing;
import code.elixir.features.impl.player.FastBreak;
import code.elixir.features.impl.player.FreeLook;
import code.elixir.features.impl.player.LevitationControl;
import code.elixir.features.impl.player.NameProtect;
import code.elixir.features.impl.player.NoDelay;
import code.elixir.features.impl.player.NoFall;
import code.elixir.features.impl.player.NoPush;
import code.elixir.features.impl.player.SafeWalk;
import code.elixir.features.impl.player.autoMine.receivers.AutoMineModule;
import code.elixir.features.impl.render.Arrows;
import code.elixir.features.impl.render.AspectRatio;
import code.elixir.features.impl.render.AuctionHelper;
import code.elixir.features.impl.render.BlockESP;
import code.elixir.features.impl.render.BlockOverlay;
import code.elixir.features.impl.render.CameraSettings;
import code.elixir.features.impl.render.Chams;
import code.elixir.features.impl.render.ChinaHat;
import code.elixir.features.impl.render.ClickGui;
import code.elixir.features.impl.render.CrossHair;
import code.elixir.features.impl.render.CustomCape;
import code.elixir.features.impl.render.CustomModelModule;
import code.elixir.features.impl.render.Esp;
import code.elixir.features.impl.render.EspSkeleton;
import code.elixir.features.impl.render.FireFly;
import code.elixir.features.impl.render.Fullbright;
import code.elixir.features.impl.render.HandShader;
import code.elixir.features.impl.render.HitMarker;
import code.elixir.features.impl.render.HitRange;
import code.elixir.features.impl.render.Hud;
import code.elixir.features.impl.render.ItemReplacer;
import code.elixir.features.impl.render.KillEffect;
import code.elixir.features.impl.render.LineGlyphs;
import code.elixir.features.impl.render.NameTags;
import code.elixir.features.impl.render.NoRender;
import code.elixir.features.impl.render.Particles;
import code.elixir.features.impl.render.Prediction;
import code.elixir.features.impl.render.SeeInvisible;
import code.elixir.features.impl.render.ShaderFog;
import code.elixir.features.impl.render.ShulkerView;
import code.elixir.features.impl.render.SmoothCamera;
import code.elixir.features.impl.render.SwingAnimations;
import code.elixir.features.impl.render.TargetESP;
import code.elixir.features.impl.render.Trails;
import code.elixir.features.impl.render.UseIndicator;
import code.elixir.features.impl.render.ViewModel;
import code.elixir.features.impl.render.WardenHelper;
import code.elixir.features.impl.render.WorldTweaks;
import code.elixir.features.impl.render.XRay;
import code.elixir.features.impl.render.jumpCircle.receiver.JumpCircleModule;
import code.elixir.features.impl.render.wings.Wings;
import code.elixir.utils.client.managers.event.EventManager;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;

public class ModuleRepository {
   private final List<Module> modules = new ArrayList<>();

   public void setup() {
      this.register(
         new Neuro(),
         new Particles(),
         new ItemReplacer(),
         new TargetStrafe(),
         new Strafe(),
         new AirStuck(),
         new ElytraMotion(),
         new ElytraRecast(),
         new ElytraJump(),
         new ElytraTarget(),
         new ElytraWhatsapp(),
         new Flight(),
         new ElytraResolver(),
         new HighJump(),
         new ShiftTap(),
         new AspectRatio(),
         new FreeLook(),
         new ClickPearl(),
         new ClickFriend(),
         new Globals(),
         new WindHop(),
         new TargetESP(),
         new code.elixir.features.impl.render.zenisesp.ZenisESP(),
         new code.elixir.features.impl.render.Tracers(),
         new code.elixir.features.impl.render.Breadcrumbs(),
         new code.elixir.features.impl.render.StorageESP(),
         new NoWeb(),
         new ServerHelper(),
         new AstrummcHelper(),
         new AutoMineModule(),
         new AutoMineBot(),
         new AutoWarden(),
         new ItemScroller(),
         new Hud(),
         new ClickGui(),
         new AuctionHelper(),
         new WardenHelper(),
         new Prediction(),
         new XRay(),
         new AttackAura(),
         new AimAssistModule(),
         new ProjectHelper(),
         new HitBox(),
         new TriggerBot(),
         new AutoSwap(),
         new NoFriendDamage(),
         new AntiBot(),
         new AutoExplosion(),
         new CrystalTap(),
         new FastBow(),
         new AutoSprint(),
         new NoPush(),
         new ElytraHelper(),
         new ServerJoiner(),
         new NoDelay(),
         new Velocity(),
         new AutoRespawn(),
         new NoSlow(),
         new GuiMove(),
         new Blink(),
         new AutoTool(),
         new FastBreak(),
         new CameraSettings(),
         new SmoothCamera(),
         new Speed(),
         new DragonFly(),
         new SwingAnimations(),
         new ViewModel(),
         new Chams(),
         new Esp(),
         new EspSkeleton(),
         new NameTags(),
         new ShulkerView(),
         new BlockESP(),
         new AutoTotem(),
         new EnderChestPlus(),
         new FreeCam(),
         new ChestStealer(),
         new AutoBrew(),
         new AutoTpAccept(),
         new AutoAuth(),
         new Arrows(),
         new AutoLeave(),
         new NoClip(),
         new NoRender(),
         new Fullbright(),
         new NameProtect(),
         new SeeInvisible(),
         new AutoArmor(),
         new AutoUse(),
         new AutoPotion(),
         new NoInteract(),
         new CrossHair(),
         new HitMarker(),
         new HitRange(),
         new SuperFireWork(),
         new Spider(),
         new SafeWalk(),
         new ServerRPSpoofer(),
         new NoFall(),
         new DeathCoords(),
         new XCarry(),
         new AntiAFK(),
         new FakePing(),
         new LevitationControl(),
         new KillEffect(),
         new FireFly(),
         new LineGlyphs(),
         new UseTracker(),
         new ShulkerBypass(),
         new ChinaHat(),
         new PacketCriticals(),
         new JumpCircleModule(),
         new AutoDodge(),
         new ClientSounds(),
         new SoundsRemove(),
         new Trails(),
         new CustomModelModule(),
         new CustomCape(),
         new UseIndicator(),
         new TapeMouse(),
         new AutoDuel(),
         new Panic(),
         new Wings(),
         new WorldTweaks(),
         new ShaderFog(),
         new BlockOverlay(),
         new HandShader()
      );
      this.sanitizeBrokenModules();
   }

   private void sanitizeBrokenModules() {
      this.modules.removeIf(module -> {
         if (EventManager.canInspectEventHandlers(module)) {
            return false;
         } else {
            LogManager.getLogger("Elixir").warn("Removing broken module {} from registry", module.getClass().getName());
            if (module.isState()) {
               module.setState(false);
            }

            return true;
         }
      });
   }

   public void register(Module... module) {
      this.modules.addAll(List.of(module));
   }

   public List<Module> modules() {
      return this.modules;
   }
}
