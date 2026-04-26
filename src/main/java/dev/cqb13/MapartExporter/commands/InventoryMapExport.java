package dev.cqb13.MapartExporter.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.cqb13.MapartExporter.ExportUtils;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Formatting;

public class InventoryMapExport extends Command {
    public InventoryMapExport() {
        super("export-inventory-maps", "Exports all maps in your inventory");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (mc.player == null) {
                return 0;
            }

            int count = 0;

            for (ItemStack stack : mc.player.getInventory().getMainStacks()) {
                if (!(stack.getItem() instanceof FilledMapItem)) {
                    continue;
                }

                MapState mapState = FilledMapItem.getMapState(stack, mc.player.getEntityWorld());
                if (mapState == null)
                    continue;

                byte[] mapColors = mapState.colors.clone();

                String rawName = stack.getName().getString();
                String filename = ExportUtils.sanitizeMapName(rawName);

                try {
                    ExportUtils.saveImageFromMapColors(mapColors, filename, true);
                    count++;
                } catch (Exception error) {
                    try {
                        ExportUtils.saveImageFromMapColors(mapColors, filename + ExportUtils.randomDigits(10), true);
                    } catch (Exception e) {
                        ChatUtils.sendMsg(Formatting.RED, "Failed to save map: " + e.getMessage());
                    }
                }
            }

            ChatUtils.sendMsg(Formatting.GREEN, "Exported " + count + " maps.");
            return SINGLE_SUCCESS;
        });
    }
}
