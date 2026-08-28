package code.elixir.features.impl.render.targetesp;

public interface TargetEspMode {
   String getName();

   void render(TargetEspContext ctx);

   default void reset() {}
}
