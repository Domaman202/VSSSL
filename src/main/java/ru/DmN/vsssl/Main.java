package ru.DmN.vsssl;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.util.math.BlockPos;

import java.io.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Main implements ModInitializer {
    public static byte[] save;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("copyregion").then(argument("start", BlockPosArgumentType.blockPos()).then(argument("end", BlockPosArgumentType.blockPos()).executes(context -> {
                RegionData save = new RegionData();
                var pos0 =  BlockPosArgumentType.getBlockPos(context, "start");
                var pos1 = BlockPosArgumentType.getBlockPos(context, "end");
                var xStart = Math.min(pos0.getX(), pos1.getX());
                var xEnd = Math.max(pos0.getX(), pos1.getX());
                var yStart = Math.min(pos0.getY(), pos1.getY());
                var yEnd = Math.max(pos0.getY(), pos1.getY());
                var zStart = Math.min(pos0.getZ(), pos1.getZ());
                var zEnd = Math.max(pos0.getZ(), pos1.getZ());
                var world = context.getSource().getWorld();
                int i = xStart;
                do {
                    int j = yStart;
                    do {
                        int k = zStart;
                        do {
                            var state = world.getBlockState(new BlockPos(i, j, k));
                            if (!state.isAir())
                                save.blocks.add(new BlockData( i - xStart, j - yStart, k - zStart, state));
                            k++;
                        } while (k < zEnd);
                        j++;
                    } while (j < yEnd);
                    i++;
                } while (i < xEnd);

                try {
                    var baos = new ByteArrayOutputStream();
                    var oos = new ObjectOutputStream(baos);
                    oos.writeObject(save);
                    oos.close();
                    baos.close();
                    Main.save = baos.toByteArray();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                return 1;
            }))));

            dispatcher.register(literal("pasteregion").then(argument("pos", BlockPosArgumentType.blockPos()).executes(context -> {
                RegionData save;

                try {
                    var bais = new ByteArrayInputStream(Main.save);
                    var ois = new ObjectInputStream(bais);
                    save = (RegionData) ois.readObject();
                    ois.close();
                    bais.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                var world = context.getSource().getWorld();
                var offset = BlockPosArgumentType.getBlockPos(context, "pos");
                for (var data : save.blocks)
                    world.setBlockState(offset.add(data.xOffset, data.yOffset, data.zOffset), data.getState());
                return 1;
            })));
        });
    }
}
