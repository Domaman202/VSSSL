package ru.DmN.vsssl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RegionData implements Serializable {
    public List<BlockData> blocks;

    public RegionData() {
        blocks = new ArrayList<>();
    }
}
