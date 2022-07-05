package ru.DmN.vsssl;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BlockData implements Serializable {
    public String id;
    public int xOffset;
    public int yOffset;
    public int zOffset;
    public Map<String, Object> properties;

    public BlockData(int xOffset, int yOffset, int zOffset, BlockState state) {
        this.id = Registry.BLOCK.getId(state.getBlock()).getPath();
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
                }
                state = state.with((Property<T>) state.getProperties().stream().filter(p -> p.getName().equals(property)).findFirst().orElseThrow(), val);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return state;
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
}
