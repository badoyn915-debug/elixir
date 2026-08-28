package baritone.event;

import baritone.api.event.events.BlockInteractEvent;
import baritone.api.event.events.RenderEvent;
import baritone.api.event.events.RotationMoveEvent;

public class GameEventHandler {
    public void onRenderPass(RenderEvent event) {}
    public void onPlayerRotationMove(RotationMoveEvent event) {}
    public void onBlockInteract(BlockInteractEvent event) {}
}
