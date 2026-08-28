package code.elixir.features.impl.render.targetesp;

import code.elixir.features.impl.render.targetesp.modes.CircleTargetEsp;
import code.elixir.features.impl.render.targetesp.modes.CubeTargetEsp;
import code.elixir.features.impl.render.targetesp.modes.EchoTargetEsp;
import code.elixir.features.impl.render.targetesp.modes.EnergyTargetEsp;
import code.elixir.features.impl.render.targetesp.modes.GlitchTargetEsp;
import code.elixir.features.impl.render.targetesp.modes.HelixTargetEsp;
import code.elixir.features.impl.render.targetesp.modes.OrbitTargetEsp;
import code.elixir.features.impl.render.targetesp.modes.PulseTargetEsp;
import code.elixir.features.impl.render.targetesp.modes.QuantumTargetEsp;
import code.elixir.features.impl.render.targetesp.modes.RadarTargetEsp;
import code.elixir.features.impl.render.targetesp.modes.SoulsTargetEsp;
import code.elixir.features.impl.render.targetesp.modes.WaveTargetEsp;
import java.util.HashMap;
import java.util.Map;

public class TargetEspManager {
   private static final TargetEspManager INSTANCE = new TargetEspManager();
   private final Map<String, TargetEspMode> modes = new HashMap<>();

   private TargetEspManager() {
      this.register(new SoulsTargetEsp());
      this.register(new CubeTargetEsp());
      this.register(new CircleTargetEsp());
      this.register(new PulseTargetEsp());
      this.register(new OrbitTargetEsp());
      this.register(new WaveTargetEsp());
      this.register(new RadarTargetEsp());
      this.register(new EnergyTargetEsp());
      this.register(new EchoTargetEsp());
      this.register(new HelixTargetEsp());
      this.register(new GlitchTargetEsp());
      this.register(new QuantumTargetEsp());
   }

   public static TargetEspManager getInstance() {
      return INSTANCE;
   }

   public void register(TargetEspMode mode) {
      this.modes.put(mode.getName(), mode);
   }

   public TargetEspMode getMode(String name) {
      return this.modes.get(name);
   }

   public void render(String modeName, TargetEspContext ctx) {
      TargetEspMode mode = this.modes.get(modeName);
      if (mode != null) {
         mode.render(ctx);
      }
   }

   public void resetAll() {
      for (TargetEspMode mode : this.modes.values()) {
         mode.reset();
      }
   }
}
