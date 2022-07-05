package ru.DmN.vsssl;

import net.minecraft.util.math.BlockPos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RegionData implements Serializable {
    public List<BlockData> blocks;

    public RegionData() {
        blocks = new ArrayList<>();
    }

    public BlockPos getMinPos() {
        int x = Integer.MAX_VALUE;
        int y = Integer.MAX_VALUE;
        int z = Integer.MAX_VALUE;
        for (var data : this.blocks) {
            x = Math.min(x, data.xOffset);
            y = Math.min(y, data.yOffset);
            z= Math.min(z, data.zOffset);
        }
        return new BlockPos(x == Integer.MAX_VALUE ? 0 : x, y == Integer.MAX_VALUE ? 0 : y, z == Integer.MAX_VALUE ? 0 : z);
    }

    public BlockPos getMaxPos() {
        int x = Integer.MIN_VALUE;
        int y = Integer.MIN_VALUE;
        int z = Integer.MIN_VALUE;
        for (var data : this.blocks) {
            x = Math.max(x, data.xOffset);
            y = Math.max(y, data.yOffset);
            z= Math.max(z, data.zOffset);
        }
        return new BlockPos(x == Integer.MIN_VALUE ? 0 : x, y == Integer.MIN_VALUE ? 0 : y, z == Integer.MIN_VALUE ? 0 : z);
    }
}
