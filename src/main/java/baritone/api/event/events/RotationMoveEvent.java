package baritone.api.event.events;

import baritone.api.utils.Rotation;

public class RotationMoveEvent {
    private Rotation original;
    private float yaw;
    private float pitch;

    public Rotation getOriginal() {
        return original;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
