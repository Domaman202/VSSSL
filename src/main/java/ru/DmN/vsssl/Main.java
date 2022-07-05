package ru.DmN.vsssl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

            dispatcher.register(literal("pasteregion").then(argument("pos", BlockPosArgumentType.blockPos()).then(argument("rotation", IntegerArgumentType.integer()).executes(context -> processSave(context, (pos, state) -> context.getSource().getWorld().setBlockState(pos, state))))));

            dispatcher.register(literal("preview").then(argument("pos", BlockPosArgumentType.blockPos()).then(argument("rotation", IntegerArgumentType.integer()).executes(context -> processSave(context, (pos, state) -> context.getSource().getPlayer().networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, state)))))));

            dispatcher.register(literal("validate").then(argument("pos", BlockPosArgumentType.blockPos()).then(argument("rotation", IntegerArgumentType.integer()).executes(context -> {
                var player = context.getSource().getPlayer();
                var net = player.networkHandler;
                for (var pos : validate(context)) {
                    net.sendPacket(new BlockUpdateS2CPacket(pos, Blocks.BEDROCK.getDefaultState()));
                    player.sendMessage(MutableText.of(new LiteralTextContent(pos.toShortString())).setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.RED))), false);
                }
                return 1;
            }))));
        });
    }

    public static List<BlockPos> validate(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var invalidList = new ArrayList<BlockPos>();
        processSave(context, (pos, state) -> {
            var s = context.getSource().getWorld().getBlockState(pos);
            if (s.isAir())
                return;
            if (s.getBlock().equals(state.getBlock())) {
                if (s.getProperties().size() == state.getProperties().size()) {
                    if (s.getProperties().stream().filter(property -> state.getProperties().stream().anyMatch(property1 -> property.getName().equals(property1.getName()) && s.get(property1).equals(state.get(property)))).count() == state.getProperties().size()) {
                        return;
                    }
                }
            }
            invalidList.add(pos);
        });
        return invalidList;
    }

    public static int processSave(CommandContext<ServerCommandSource> context, ProcessAction action) throws CommandSyntaxException {
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

        var offset = BlockPosArgumentType.getBlockPos(context, "pos");
        var rotation = context.getArgument("rotation", int.class);
        for (var data : save.blocks) {
            var pos = new BlockPos(data.xOffset, data.yOffset, data.zOffset);
            for (int i = 0; i < rotation; i++)
                pos = pos.rotate(BlockRotation.COUNTERCLOCKWISE_90);
            pos = pos.add(offset);
            action.process(pos, data.getState(rotation));
        }
        return 1;
    }

    @FunctionalInterface
    interface ProcessAction {
        void process(BlockPos pos, BlockState state);
    }
}
