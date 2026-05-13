package dev.cqb13.MapartExporter.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.cqb13.MapartExporter.ExportUtils;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class HandMapExport extends Command {
    public HandMapExport() {
        super("export-map", "Exports the map held in your hand");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("name", StringArgumentType.string())
                .executes(context -> {
                    String filename = context.getArgument("name", String.class);

                    ItemStack itemStack = mc.player.getMainHandItem();

                    if (!(itemStack.getItem() instanceof MapItem)) {
                        itemStack = mc.player.getOffhandItem();
                    }

                    if (!(itemStack.getItem() instanceof MapItem)) {
                        ChatUtils.sendMsg(ChatFormatting.RED, "Item in hand is not a filled map");
                        return 0;
                    }

                    MapItemSavedData mapState = MapItem.getSavedData(itemStack, mc.player.level());

                    if (mapState == null) {
                        ChatUtils.sendMsg(ChatFormatting.RED, "Failed to get map state");
                        return 0;
                    }

                    byte[] mapColors = mapState.colors.clone();

                    try {
                        ExportUtils.saveImageFromMapColors(mapColors, filename, true);
                    } catch (IllegalArgumentException e) {
                        ChatUtils.sendMsg(ChatFormatting.RED, "Failed to save map: " + e.getMessage());
                    }

                    ChatUtils.sendMsg(ChatFormatting.GREEN, "Exported complete.");
                    return SINGLE_SUCCESS;
                }));
    }
}
