package ru.DmN.vsssl;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.ApiStatus;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

public class BlockData implements Externalizable {
    public String id;
    public String type;
    public NbtCompound nbt;
    public int xOffset;
    public int yOffset;
    public int zOffset;
    public Map<String, Object> properties;

    @ApiStatus.Internal
    public BlockData() {
    }

    public BlockData(int xOffset, int yOffset, int zOffset, BlockState state, BlockEntity entity) {
        this.id = Registry.BLOCK.getId(state.getBlock()).getPath();
        if (entity != null) {
            this.type = Registry.BLOCK_ENTITY_TYPE.getId(entity.getType()).getPath();
            this.nbt = entity.createNbt();
        }
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
        this.properties = new HashMap<>();
        for (var property : state.getProperties()) {
            this.properties.put(property.getName(), state.get(property));
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>, V extends T> BlockState getState(int rotation) {
        var state = Registry.BLOCK.get(new Identifier(this.id)).getDefaultState();
        for (var property : this.properties.keySet()) {
            try {
                var val = (V) this.properties.get(property);
                if (val instanceof Direction v) {
                    for (int i = 0; i < rotation; i++)
                        v = rotate(v);
                    val = (V) v;
                } else if (property.equals("rotation"))
                    for (int i = 0; i < rotation; i++)
                        val = (V) (Object) BlockRotation.CLOCKWISE_90.rotate((Integer) val, 16);
                state = state.with((Property<T>) state.getProperties().stream().filter(p -> p.getName().equals(property)).findFirst().orElseThrow(), val);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return state;
    }

    public BlockEntity getBlockEntity(BlockPos pos, BlockState state) {
        if (type == null)
            return null;
        var entity = Registry.BLOCK_ENTITY_TYPE.get(new Identifier(type)).instantiate(pos, state);
        if (entity != null && nbt != null)
            entity.readNbt(nbt);
        return entity;
    }

    public static Direction rotate(Direction dir) {
        return switch (dir) {
            default -> dir;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case NORTH -> Direction.EAST;
        };
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(id);
        if (type != null) {
            out.writeBoolean(true);
            out.writeUTF(type);
            NbtIo.write(nbt, out);
        } else out.writeBoolean(false);
        out.writeInt(xOffset);
        out.writeInt(yOffset);
        out.writeInt(zOffset);
        out.writeObject(properties);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = in.readUTF();
        if (in.readBoolean()) {
            type = in.readUTF();
            nbt = NbtIo.read(in);
        }
        xOffset = in.readInt();
        yOffset = in.readInt();
        zOffset = in.readInt();
        properties = (Map<String, Object>) in.readObject();
    }
}
