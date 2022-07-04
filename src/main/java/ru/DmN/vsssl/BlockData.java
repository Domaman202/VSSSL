package ru.DmN.vsssl;

import java.io.Serializable;

public class BlockData implements Serializable {
    public String id;
    public int xOffset;
    public int yOffset;
    public int zOffset;

    public BlockData(String id, int xOffset, int yOffset, int zOffset) {
        this.id = id;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
    }
}
