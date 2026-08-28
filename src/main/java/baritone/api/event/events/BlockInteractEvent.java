package baritone.api.event.events;

public class BlockInteractEvent {
    public enum Type {
        START_BREAK,
        BREAK,
        USE
    }

    private final Type type;

    public BlockInteractEvent(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
