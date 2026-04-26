package dev.cqb13.MapartExporter.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.cqb13.MapartExporter.ExportUtils;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Formatting;

public class HandMapExport extends Command {
    public HandMapExport() {
        super("export-map", "Exports the map held in your hand");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("name", StringArgumentType.string())
                .executes(context -> {
                    String filename = context.getArgument("name", String.class);

                    ItemStack itemStack = mc.player.getMainHandStack();

                    if (!(itemStack.getItem() instanceof FilledMapItem)) {
                        itemStack = mc.player.getOffHandStack();
                    }

                    if (!(itemStack.getItem() instanceof FilledMapItem)) {
                        ChatUtils.sendMsg(Formatting.RED, "Item in hand is not a filled map");
                        return 0;
                    }

                    MapState mapState = FilledMapItem.getMapState(itemStack, mc.player.getEntityWorld());

                    if (mapState == null) {
                        ChatUtils.sendMsg(Formatting.RED, "Failed to get map state");
                        return 0;
                    }

                    byte[] mapColors = mapState.colors.clone();

                    try {
                        ExportUtils.saveImageFromMapColors(mapColors, filename, true);
                    } catch (IllegalArgumentException e) {
                        ChatUtils.sendMsg(Formatting.RED, "Failed to save map: " + e.getMessage());
                    }

                    ChatUtils.sendMsg(Formatting.GREEN, "Exported complete.");
                    return SINGLE_SUCCESS;
                }));
    }
}
