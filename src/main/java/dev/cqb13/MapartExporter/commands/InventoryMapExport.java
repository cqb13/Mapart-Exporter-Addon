package dev.cqb13.MapartExporter.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.cqb13.MapartExporter.ExportUtils;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class InventoryMapExport extends Command {
    public InventoryMapExport() {
        super("export-inventory-maps", "Exports all maps in your inventory");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            if (mc.player == null) {
                return 0;
            }

            int count = 0;

            for (ItemStack stack : mc.player.getInventory().getNonEquipmentItems()) {
                if (!(stack.getItem() instanceof MapItem)) {
                    continue;
                }

                MapItemSavedData mapState = MapItem.getSavedData(stack, mc.player.level());
                if (mapState == null)
                    continue;

                byte[] mapColors = mapState.colors.clone();

                String rawName = stack.getHoverName().getString();
                String filename = ExportUtils.sanitizeMapName(rawName);

                try {
                    ExportUtils.saveImageFromMapColors(mapColors, filename, true);
                    count++;
                } catch (Exception error) {
                    try {
                        ExportUtils.saveImageFromMapColors(mapColors, filename + ExportUtils.randomDigits(10), true);
                    } catch (Exception e) {
                        ChatUtils.sendMsg(ChatFormatting.RED, "Failed to save map: " + e.getMessage());
                    }
                }
            }

            ChatUtils.sendMsg(ChatFormatting.GREEN, "Exported " + count + " maps.");
            return SINGLE_SUCCESS;
        });
    }
}
