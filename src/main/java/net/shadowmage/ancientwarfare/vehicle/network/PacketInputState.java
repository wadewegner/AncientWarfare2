package net.shadowmage.ancientwarfare.vehicle.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

public final class PacketInputState extends PacketBase {

    int entityID;
    public int commandID;
    public boolean[] keyStates;
    public double x, y, z;
    public float yaw, pitch;
    public boolean dummy = false;

    public PacketInputState() {
    }

    public void setID(Entity e, int id) {
        entityID = e.getEntityId();
        commandID = id;
    }

    public void setDummy(boolean val) {
        this.dummy = val;
    }

    public void setPosition(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setInputStates(boolean[] states) {
        keyStates = states;
    }

    @Override
    protected void writeToStream(ByteBuf data) {
        data.writeInt(commandID);
        data.writeInt(entityID);
        data.writeBoolean(dummy);
        if (!dummy) {
            data.writeDouble(x);
            data.writeDouble(y);
            data.writeDouble(z);
            data.writeFloat(yaw);
            data.writeFloat(pitch);
            data.writeByte(keyStates.length);
            for (boolean state : keyStates) {
                data.writeBoolean(state);
            }
        }
    }

    @Override
    protected void readFromStream(ByteBuf data) {
        commandID = data.readInt();
        entityID = data.readInt();
        dummy = data.readBoolean();
        if (!dummy) {
            x = data.readDouble();
            y = data.readDouble();
            z = data.readDouble();
            yaw = data.readFloat();
            pitch = data.readFloat();
            keyStates = new boolean[data.readByte()];
            for (int i = 0; i < keyStates.length; i++) {
                keyStates[i] = data.readBoolean();
            }
        }
    }

    @Override
    protected void execute(EntityPlayer player) {
        Entity e = player.world.getEntityByID(entityID);
        if (e instanceof VehicleBase) {
            ((VehicleBase) e).inputHandler.handleInputPacket(this);
        }
    }

}
