package code.elixir.utils.client.sound;

import code.elixir.features.impl.misc.ClientSounds;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class SoundManager implements QuickImports {
   public static SoundEvent OPEN_GUI = SoundEvent.of(Identifier.of("minecraft:gui_open"));
   public static SoundEvent CLOSE_GUI = SoundEvent.of(Identifier.of("minecraft:gui_close"));
   public static SoundEvent ENABLE_MODULE = SoundEvent.of(Identifier.of("minecraft:module_enable"));
   public static SoundEvent DISABLE_MODULE = SoundEvent.of(Identifier.of("minecraft:module_disable"));
   public static SoundEvent CATEGORY_CLICK = SoundEvent.of(Identifier.of("minecraft:category_click"));
   public static SoundEvent NOTIF = SoundEvent.of(Identifier.of("minecraft:notif"));
   public static SoundEvent SETTING_INTERACT = CATEGORY_CLICK;
   public static SoundEvent ORTHODOX = SoundEvent.of(Identifier.of("minecraft:kolokolnia_kill"));
   public static SoundEvent KILL1 = SoundEvent.of(Identifier.of("minecraft:kill1"));
   public static SoundEvent KILL2 = SoundEvent.of(Identifier.of("minecraft:kill2"));
   public static SoundEvent KILL3 = SoundEvent.of(Identifier.of("minecraft:kill3"));
   public static SoundEvent KILL4 = SoundEvent.of(Identifier.of("minecraft:kill4"));
   public static SoundEvent KILL5 = SoundEvent.of(Identifier.of("minecraft:kill5"));
   public static SoundEvent KILL6 = SoundEvent.of(Identifier.of("minecraft:kill6"));
   public static SoundEvent CRITICAL = SoundEvent.of(Identifier.of("minecraft:critical"));
   public static SoundEvent HELLO = SoundEvent.of(Identifier.of("minecraft:hello"));

   public static void init() {
      Registry.register(Registries.SOUND_EVENT, OPEN_GUI.id(), OPEN_GUI);
      Registry.register(Registries.SOUND_EVENT, CLOSE_GUI.id(), CLOSE_GUI);
      Registry.register(Registries.SOUND_EVENT, ENABLE_MODULE.id(), ENABLE_MODULE);
      Registry.register(Registries.SOUND_EVENT, DISABLE_MODULE.id(), DISABLE_MODULE);
      Registry.register(Registries.SOUND_EVENT, CATEGORY_CLICK.id(), CATEGORY_CLICK);
      Registry.register(Registries.SOUND_EVENT, NOTIF.id(), NOTIF);
      Registry.register(Registries.SOUND_EVENT, ORTHODOX.id(), ORTHODOX);
      Registry.register(Registries.SOUND_EVENT, KILL1.id(), KILL1);
      Registry.register(Registries.SOUND_EVENT, KILL2.id(), KILL2);
      Registry.register(Registries.SOUND_EVENT, KILL3.id(), KILL3);
      Registry.register(Registries.SOUND_EVENT, KILL4.id(), KILL4);
      Registry.register(Registries.SOUND_EVENT, KILL5.id(), KILL5);
      Registry.register(Registries.SOUND_EVENT, KILL6.id(), KILL6);
      Registry.register(Registries.SOUND_EVENT, CRITICAL.id(), CRITICAL);
      Registry.register(Registries.SOUND_EVENT, HELLO.id(), HELLO);
   }

   public static void playSound(SoundEvent sound) {
      if (!PlayerInteractionHelper.nullCheck()) {
         ClientSounds clientSounds = ClientSounds.getInstance();
         boolean guiSound = sound == OPEN_GUI
            || sound == CLOSE_GUI
            || sound == CATEGORY_CLICK
            || sound == SETTING_INTERACT
            || sound == NOTIF
            || sound == CRITICAL
            || sound == HELLO;
         if (guiSound) {
            mc.getSoundManager().play(PositionedSoundInstance.master(sound, 1.4F));
         } else if (clientSounds != null && clientSounds.isState()) {
            float volume = clientSounds.getVolume();
            if (!(volume <= 0.0F)) {
               mc.getSoundManager().play(PositionedSoundInstance.master(sound, volume, clientSounds.getPitch()));
            }
         }
      }
   }

   public static float getClientVolume() {
      ClientSounds clientSounds = ClientSounds.getInstance();
      return clientSounds != null && clientSounds.isState() ? clientSounds.getVolume() : 0.0F;
   }

   private SoundManager() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
